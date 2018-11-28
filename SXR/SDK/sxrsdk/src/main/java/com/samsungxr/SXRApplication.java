/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.samsungxr.io.SXRTouchPadGestureListener;
import com.samsungxr.nodes.SXRViewNode;
import com.samsungxr.script.IScriptable;
import com.samsungxr.utility.DockEventReceiver;
import com.samsungxr.utility.GrowBeforeQueueThreadPoolExecutor;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.Threads;
import com.samsungxr.utility.VrAppSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SXRApplication is where SXRf apps start from. Provides lifecycle control and events control.
 * Consider using SXRActivity which would calls into the correct SXRApplication methods.
 * SXRApplication is meant for use-cases where are apps can't use SXRActivity.
 *
 * User is responsible to call pass standard Android events to the SXRApplication.
 * Mapping should be pretty obvious but there it is just in case:
 * OnCreate -> instantiate the SXRApplication
 * OnPause -> SXRApplication.pause
 * OnResume -> SXRApplication.resume
 * OnDestroy -> SXRApplication.destroy
 * dispatchKeyEvent -> SXRApplication.dispatchKeyEvent
 * OnKeyLongPress -> SXRApplication.keyLongPress
 * OnKeyUp -> SXRApplication.keyUp
 * OnKeyDown -> SXRApplication.keyDown
 * dispatchGenericMotionEvent -> SXRApplication.dispatchGenericMotionEvent
 * dispatchTouchEvent -> SXRApplication.dispatchTouchEvent
 * onConfigurationChanged -> SXRApplication.configurationChanged
 * onTouchEvent -> SXRApplication.touchEvent
 * onWindowFocusChanged -> SXRApplication.windowFocusChanged
 */
public final class SXRApplication implements IEventReceiver, IScriptable {

    static {
        System.loadLibrary("sxrsdk");
    }
    protected static final String TAG = "SXRApplication";

    private final Activity mActivity;
    private SXRViewManager mViewManager;
    private volatile SXRConfigurationManager mConfigurationManager;
    private SXRMain mSXRMain;
    private VrAppSettings mAppSettings;
    private static View mFullScreenView;

    // Group of views that are going to be drawn
    // by some SXRViewNode to the scene.
    private ViewGroup mRenderableViewGroup;
    private IActivityNative mActivityNative;
    private boolean mPaused = true;

    // Send to listeners and scripts but not this object itself
    private static final int SEND_EVENT_MASK =
            SXREventManager.SEND_MASK_ALL & ~SXREventManager.SEND_MASK_OBJECT;

    private SXREventReceiver mEventReceiver = new SXREventReceiver(this);

    public SXRApplication(final Activity activity) {
        android.util.Log.i(TAG, "SXRApplication " + Integer.toHexString(hashCode()));

        mActivity = activity;
        final int backendId = SystemPropertyUtil.getSystemProperty(DEBUG_GEARVRF_BACKEND);
        if (-1 != backendId) {
            mDelegate = tryBackend(backendId);
        } else {
            for (int i = 0; i <= MAX_BACKEND_ID; ++i) {
                mDelegate = tryBackend(i);
                if (null != mDelegate) {
                    break;
                }
            }
        }

        if (null == mDelegate) {
            throw new IllegalStateException("Fatal error: no backend available");
        }

        if (null != Threads.getThreadPool()) {
            Threads.getThreadPool().shutdownNow();
        }
        Threads.setThreadPool(new GrowBeforeQueueThreadPoolExecutor("sxrsdk"));

        /*
         * Removes the title bar and the status bar.
         */
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mRenderableViewGroup = (ViewGroup) activity.findViewById(android.R.id.content).getRootView();
        mActivityNative = mDelegate.getActivityNative();
    }

    /**
     *
     * @param activity the current activity
     * @param sxrMain SXRMain implementation to execute
     */
    public SXRApplication(final Activity activity, SXRMain sxrMain) {
        this(activity, sxrMain, "_sxr.xml");
    }

    /**
     *
     * @param activity the current activity
     * @param sxrMain SXRMain implementation to execute
     * @param dataFileName alternate configuration file; see _sxr.xml that is part of the framework
     *                     for reference
     */
    public SXRApplication(final Activity activity, SXRMain sxrMain, String dataFileName) {
        this(activity);

        setMain(sxrMain, dataFileName);
    }

    private final SXRActivityDelegate tryBackend(final int backendId) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            AssetManager assets = mActivity.getAssets();
            inputStream = assets.open("backend_" + backendId + ".txt");
            reader = new BufferedReader(new InputStreamReader(inputStream));

            final String line = reader.readLine();
            Log.i(TAG, "trying backend " + line);
            final Class<?> aClass = Class.forName(line);

            SXRActivityDelegate delegate = (SXRActivityDelegate) aClass.newInstance();
            mAppSettings = delegate.makeVrAppSettings();
            delegate.onCreate(this);

            return delegate;
        } catch (final Exception exc) {
            return null;
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected void onInitAppSettings(VrAppSettings appSettings) {
        mDelegate.onInitAppSettings(appSettings);
    }

    private void onConfigure(final String dataFilename) {
        mConfigurationManager = mDelegate.makeConfigurationManager();
        mConfigurationManager.addDockListener(this);
        mConfigurationManager.configureForHeadset(SXRConfigurationManager.DEFAULT_HEADSET_MODEL);
        mDelegate.parseXmlSettings(mActivity.getAssets(), dataFilename);

        onInitAppSettings(mAppSettings);
    }

    public final VrAppSettings getAppSettings() {
        return mAppSettings;
    }

    final SXRViewManager getViewManager() {
        return mViewManager;
    }

    final boolean isPaused() {
        return mPaused;
    }

    /**
     *
     */
    public void pause() {
        android.util.Log.i(TAG, "pause " + Integer.toHexString(hashCode()));

        mDelegate.onPause();
        mPaused = true;
        if (mViewManager != null) {
            mViewManager.onPause();

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onPause");
        }
    }

    /**
     *
     */
    public void resume() {
        android.util.Log.i(TAG, "resume " + Integer.toHexString(hashCode()));

        mDelegate.onResume();
        mPaused = false;

        if (mViewManager != null) {
            mViewManager.onResume();

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onResume");
        }
    }

    /**
     *
     */
    public void destroy() {
        android.util.Log.i(TAG, "destroy " + Integer.toHexString(hashCode()));
        mDelegate.onDestroy();

        if (mViewManager != null) {
            mViewManager.onDestroy();
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onDestroy");
            mViewManager = null;
        }
        if (null != mDockEventReceiver) {
            mDockEventReceiver.stop();
        }

        if (null != mConfigurationManager && !mConfigurationManager.isDockListenerRequired()) {
            handleOnUndock();
        }

        if (null != mActivityNative) {
            mActivityNative.onDestroy();
            mActivityNative = null;
        }

        mDockListeners.clear();
        mSXRMain = null;
        mDelegate = null;
        mAppSettings = null;
        mRenderableViewGroup = null;
        mConfigurationManager = null;
    }

    /**
     * Invalidating just the SXRView associated with the SXRViewNode
     * incorrectly set the clip rectangle to just that view. To fix this,
     * we have to create a full screen android View and invalidate this
     * to restore the clip rectangle.
     * @return full screen View object
     */
    public View getFullScreenView() {
        if (mFullScreenView != null) {
            return mFullScreenView;
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final int screenWidthPixels = Math.max(metrics.widthPixels, metrics.heightPixels);
        final int screenHeightPixels = Math.min(metrics.widthPixels, metrics.heightPixels);

        final ViewGroup.LayoutParams layout = new ViewGroup.LayoutParams(screenWidthPixels, screenHeightPixels);
        mFullScreenView = new View(mActivity);
        mFullScreenView.setLayoutParams(layout);
        mRenderableViewGroup.addView(mFullScreenView);

        return mFullScreenView;
    }

    /**
     * Gets the {@linkplain SXRMain} linked to the activity.
     * @return the {@link SXRMain}.
     */
    public final SXRMain getMain() {
        return mSXRMain;
    }

    final long getNative() {
        return null != mActivityNative ? mActivityNative.getNative() : 0;
    }

    final IActivityNative getActivityNative() {
        return mActivityNative;
    }

    final void setCameraRig(SXRCameraRig cameraRig) {
        if (null != mActivityNative) {
            mActivityNative.setCameraRig(cameraRig);
        }
    }

    private long mBackKeyDownTime;

    /**
     *
     * @param event
     * @return
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyAction = event.getAction();
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            if (KeyEvent.ACTION_DOWN == keyAction) {
                if (0 == mBackKeyDownTime) {
                    mBackKeyDownTime = event.getDownTime();
                }
            } else if (KeyEvent.ACTION_UP == keyAction) {
                final long duration = event.getEventTime() - mBackKeyDownTime;
                mBackKeyDownTime = 0;
                if (!isPaused()) {
                    if (duration < 250) {
                        if (!mSXRMain.onBackPress()) {
                            if (!mDelegate.onBackPress()) {
                                mActivity.finish();
                            }
                        }
                    }
                }
            }
            return true;
        } else {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        final AudioManager audioManager = (AudioManager) mActivity.getSystemService(Activity.AUDIO_SERVICE);
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE, 0);
                        return true;
                    }
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (keyAction == KeyEvent.ACTION_DOWN) {
                        final AudioManager audioManager = (AudioManager) mActivity.getSystemService(Activity.AUDIO_SERVICE);
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER, 0);
                        return true;
                    }
            }
        }

        mViewManager.getEventManager().sendEventWithMask(
                SEND_EVENT_MASK,
                this,
                IApplicationEvents.class,
                "dispatchKeyEvent", event);

        if (mViewManager.dispatchKeyEvent(event)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean keyLongPress(int keyCode, KeyEvent event) {
        if (mDelegate.onKeyLongPress(keyCode, event)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean keyUp(int keyCode, KeyEvent event) {
        if (mDelegate.onKeyUp(keyCode, event)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean keyDown(int keyCode, KeyEvent event) {
        if (mDelegate.onKeyDown(keyCode, event)) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param event
     * @return
     */
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return mViewManager.dispatchMotionEvent(event);
    }

    /**
     *
     * @param event
     * @return
     */
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = mViewManager.dispatchMotionEvent(event);

        mViewManager.getEventManager().sendEventWithMask(
                SEND_EVENT_MASK,
                this,
                IApplicationEvents.class,
                "dispatchTouchEvent", event);

        return handled;
    }

    /**
     *
     * @param newConfig
     */
    public void configurationChanged(Configuration newConfig) {
        mDelegate.onConfigurationChanged(newConfig);

        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onConfigurationChanged", newConfig);
        }
    }

    /**
     *
     * @param event
     * @return
     */
    public boolean touchEvent(MotionEvent event) {
        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onTouchEvent", event);
        }

        return false;
    }

    /**
     *
     * @param hasFocus
     */
    public void windowFocusChanged(boolean hasFocus) {
        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onWindowFocusChanged", hasFocus);
        }

        if (hasFocus) {
            setImmersiveSticky();
        }
    }

    // Set Immersive Sticky as described here:
    // https://developer.android.com/training/system-ui/immersive.html
    private void setImmersiveSticky() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Called from C++
     */
    final boolean updateSensoredScene() {
        return mViewManager.updateSensoredScene();
    }

    /**
     * It is a convenient function to add a {@link View} to Android hierarchy
     * view. UI thread will refresh the view when necessary.
     *
     * @param view Is a {@link View} that draw itself into some
     *            {@link SXRViewNode}.
     */
    public final void registerView(final View view) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mRenderableViewGroup && view.getParent() != mRenderableViewGroup) {
                    /* The full screen should be updated otherwise just the children's bounds may be refreshed. */
                    mRenderableViewGroup.setClipChildren(false);
                    mRenderableViewGroup.addView(view);
                }
            }
        });
    }

    /**
     * Remove a child view of Android hierarchy view .
     *
     * @param view View to be removed.
     */
    public final void unregisterView(final View view) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != mRenderableViewGroup && view.getParent() == mRenderableViewGroup) {
                    mRenderableViewGroup.removeView(view);
                }
            }
        });
    }

    public final SXRContext getSXRContext() {
        return mViewManager;
    }

    @Override
    public final SXREventReceiver getEventReceiver() {
        return mEventReceiver;
    }

    private boolean mIsDocked = false;

    protected final void handleOnDock() {
        Log.i(TAG, "handleOnDock");
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!mIsDocked) {
                    mIsDocked = true;

                    if (null != mActivityNative) {
                        mActivityNative.onDock();
                    }

                    for (final DockListener dl : mDockListeners) {
                        dl.onDock();
                    }
                }
            }
        };
        mActivity.runOnUiThread(r);
    }

    protected final void handleOnUndock() {
        Log.i(TAG, "handleOnUndock");
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mIsDocked) {
                    mIsDocked = false;

                    if (null != mActivityNative) {
                        mActivityNative.onUndock();
                    }

                    for (final DockListener dl : mDockListeners) {
                        dl.onUndock();
                    }
                }
            }
        };
        mActivity.runOnUiThread(r);
    }

    public SXRConfigurationManager getConfigurationManager() {
        return mConfigurationManager;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setMain(SXRMain sxrMain, String dataFileName) {
        this.mSXRMain = sxrMain;
        if (mActivity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            onConfigure(dataFileName);
            if (!mDelegate.setMain(sxrMain, dataFileName)) {
                Log.w(TAG, "delegate's setMain failed");
                return;
            }

            mViewManager = mDelegate.makeViewManager();
            mDelegate.setViewManager(mViewManager);

            if (mConfigurationManager.isDockListenerRequired()) {
                startDockEventReceiver();
            } else {
                handleOnDock();
            }

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IApplicationEvents.class,
                    "onSetMain", sxrMain);

            final SXRConfigurationManager localConfigurationManager = mConfigurationManager;
            if (null != mDockEventReceiver && localConfigurationManager.isDockListenerRequired()) {
                getSXRContext().registerDrawFrameListener(new SXRDrawFrameListener() {
                    @Override
                    public void onDrawFrame(float frameTime) {
                        if (localConfigurationManager.isHmtConnected()) {
                            handleOnDock();
                            getSXRContext().unregisterDrawFrameListener(this);
                        }
                    }
                });
            }
        } else {
            throw new IllegalArgumentException(
                    "You can not set orientation to portrait for SXRF apps.");
        }
    }

    interface DockListener {
        void onDock();
        void onUndock();
    }

    private final List<DockListener> mDockListeners = new CopyOnWriteArrayList<DockListener>();

    final void addDockListener(final DockListener dl) {
        mDockListeners.add(dl);
    }

    private DockEventReceiver mDockEventReceiver;

    private void startDockEventReceiver() {
        mDockEventReceiver = mConfigurationManager.makeDockEventReceiver(this.getActivity(),
                new Runnable() {
                    @Override
                    public void run() {
                        handleOnDock();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        handleOnUndock();
                    }
                });
        if (null != mDockEventReceiver) {
            mDockEventReceiver.start();
        } else {
            Log.w(TAG, "dock listener not started");
        }
    }

    /**
     * Enables the Android GestureDetector which in turn fires the appropriate {@link SXRMain} callbacks.
     * By default it is not.
     * @see SXRMain#onSwipe(SXRTouchPadGestureListener.Action, float)
     * @see SXRMain#onSingleTapUp(MotionEvent)
     * @see SXRTouchPadGestureListener
     */
    public synchronized void enableGestureDetector() {
        final SXRTouchPadGestureListener gestureListener = new SXRTouchPadGestureListener() {
            @Override
            public boolean onSwipe(MotionEvent e, Action action, float vx, float vy) {
                if (null != mSXRMain) {
                    mSXRMain.onSwipe(action, vx);
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (null != mSXRMain) {
                    mSXRMain.onSingleTapUp(e);
                }
                return true;
            }
        };
        mGestureDetector = new GestureDetector(mActivity.getApplicationContext(), gestureListener);
        getEventReceiver().addListener(new SXREventListeners.ApplicationEvents() {
            @Override
            public void dispatchTouchEvent(MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
            }
        });
    }

    private GestureDetector mGestureDetector;

    private SXRActivityDelegate mDelegate;

    SXRActivityDelegate getDelegate() {
        return mDelegate;
    }

    interface SXRActivityDelegate {
        void onCreate(SXRApplication activity);
        void onPause();
        void onResume();
        void onDestroy();
        void onConfigurationChanged(final Configuration newConfig);

        boolean onKeyDown(int keyCode, KeyEvent event);
        boolean onKeyUp(int keyCode, KeyEvent event);
        boolean onKeyLongPress(int keyCode, KeyEvent event);

        boolean setMain(SXRMain sxrMain, String dataFileName);
        void setViewManager(SXRViewManager viewManager);
        void onInitAppSettings(VrAppSettings appSettings);

        VrAppSettings makeVrAppSettings();
        IActivityNative getActivityNative();
        SXRViewManager makeViewManager();
        SXRCameraRig makeCameraRig(SXRContext context);
        SXRConfigurationManager makeConfigurationManager();
        void parseXmlSettings(AssetManager assetManager, String dataFilename);

        boolean onBackPress();
    }

    static class ActivityDelegateStubs implements SXRActivityDelegate {

        @Override
        public void onCreate(SXRApplication application) {

        }

        @Override
        public void onPause() {

        }

        @Override
        public void onResume() {

        }

        @Override
        public void onDestroy() {

        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onKeyLongPress(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean setMain(SXRMain sxrMain, String dataFileName) {
            return true;
        }

        @Override
        public void setViewManager(SXRViewManager viewManager) {

        }

        @Override
        public void onInitAppSettings(VrAppSettings appSettings) {

        }

        @Override
        public VrAppSettings makeVrAppSettings() {
            return null;
        }

        @Override
        public IActivityNative getActivityNative() {
            return null;
        }

        @Override
        public SXRViewManager makeViewManager() {
            return null;
        }

        @Override
        public SXRCameraRig makeCameraRig(SXRContext context) {
            return null;
        }

        @Override
        public SXRConfigurationManager makeConfigurationManager() {
            return null;
        }

        @Override
        public void parseXmlSettings(AssetManager assetManager, String dataFilename) {

        }

        @Override
        public boolean onBackPress() {
            return false;
        }
    }

    private final static String DEBUG_GEARVRF_BACKEND = "debug.gearvrf.backend";
    private final static int MAX_BACKEND_ID = 9;
}
