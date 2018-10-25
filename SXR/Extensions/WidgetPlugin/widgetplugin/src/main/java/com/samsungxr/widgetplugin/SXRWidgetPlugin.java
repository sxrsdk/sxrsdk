package com.samsungxr.widgetplugin;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Audio;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.android.AndroidApplicationBase;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AndroidClipboard;
import com.badlogic.gdx.backends.android.AndroidEventListener;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.backends.android.AndroidGraphics;
import com.badlogic.gdx.backends.android.AndroidInput;
import com.badlogic.gdx.backends.android.AndroidInputFactory;
import com.badlogic.gdx.backends.android.AndroidNet;
import com.badlogic.gdx.backends.android.AndroidPreferences;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxNativesLoader;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRMain;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.IActivityEvents;
import com.samsungxr.IScriptEvents;
import com.samsungxr.ITouchEvents;
import com.samsungxr.io.SXRControllerType;
import com.samsungxr.utility.Log;

import java.lang.reflect.Method;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;

/**
 * This provides SXR (libGDX) widget lifecycle and context management and brings
 * it together with SXR activity context. Base activity for SXR apps which use
 * SXRWidgets
 */
public class SXRWidgetPlugin implements AndroidApplicationBase {

    static {
        GdxNativesLoader.load();
    }
    protected GLSurfaceView mWidgetView;

    protected AndroidGraphics mGraphics;

    protected int mViewWidth;
    protected int mViewHeight;

    protected AndroidAudio mAudio;
    protected AndroidFiles mFiles;
    protected AndroidNet mNet;
    protected SXRMain mMain;
    protected SXRWidget mWidget;

    protected ApplicationListener mListener;
    public Handler mHandler;
    protected final Array<Runnable> mRunnables = new Array<Runnable>();
    protected final Array<Runnable> mExecutedRunnables = new Array<Runnable>();
    protected final Array<LifecycleListener> mLifecycleListeners = new Array<LifecycleListener>();
    private final Array<AndroidEventListener> mAndroidEventListeners = new Array<AndroidEventListener>();
    protected boolean mUseImmersiveMode = false;
    protected boolean mHideStatusBar = false;
    private int mWasFocusChanged = -1;
    private boolean mIsWaitingForAudio = false;
    private volatile EGLContext mEGLContext;
    private SXRActivity mActivity;
    private AndroidInput mInput = null;

    private IActivityEvents mActivityEventsListener = new SXREventListeners.ActivityEvents() {
        @Override
        public void onPause() {
            if (null != mInput) {
                mInput.onPause();
            }

            if (mGraphics != null) {
                boolean isContinuous = mGraphics.isContinuousRendering();
                boolean isContinuousEnforced = AndroidGraphics.enforceContinuousRendering;

                // from here we don't want non continuous rendering
                AndroidGraphics.enforceContinuousRendering = true;
                mGraphics.setContinuousRendering(true);
                // calls to setContinuousRendering(false) from other thread (ex:
                // GLThread)
                // will be ignored at this point...
                mGraphics.pause();

                if (mActivity.isFinishing()) {
                    mGraphics.clearManagedCaches();
                    mGraphics.destroy();
                }

                AndroidGraphics.enforceContinuousRendering = isContinuousEnforced;
                mGraphics.setContinuousRendering(isContinuous);

                mGraphics.onPauseGLSurfaceView();
            }
        }

        @Override
        public void onResume() {
            if (mGraphics != null) {
                mGraphics.onResumeGLSurfaceView();
                mGraphics.resume();
            }
            if (null != mInput) {
                mInput.onResume();
            }
        }

        @Override
        public void onSetMain(SXRMain main) {
            main.getEventReceiver().addListener(mScriptEventsListener);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            if (hasFocus) {
                SXRWidgetPlugin.this.mWasFocusChanged = 1;
                if (SXRWidgetPlugin.this.mIsWaitingForAudio) {
                    SXRWidgetPlugin.this.mAudio.resume();
                    SXRWidgetPlugin.this.mIsWaitingForAudio = false;
                }
            } else {
                SXRWidgetPlugin.this.mWasFocusChanged = 0;
            }
        }

        @Override
        public void onConfigurationChanged(Configuration config) {
            boolean keyboardAvailable = false;
            if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
                keyboardAvailable = true;
            if (mInput != null)
            {
                mInput.keyboardAvailable = keyboardAvailable;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            // forward events to our listeners if there are any installed
            synchronized (mAndroidEventListeners) {
                for (int i = 0; i < mAndroidEventListeners.size; i++) {
                    mAndroidEventListeners.get(i).onActivityResult(requestCode,
                            resultCode, data);
                }
            }
        }
    };

    private IScriptEvents mScriptEventsListener = new SXREventListeners.ScriptEvents() {
        @Override
        public void onEarlyInit(SXRContext context) {
            mMain.getSXRContext().runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    mEGLContext = ((EGL10) EGLContext.getEGL()).eglGetCurrentContext();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            prepareGdx();
                        }
                    });
                }
            });
        }
    };

    public SXRWidgetPlugin(SXRActivity activity, final SXRWidget widget) {
        if (null == widget) {
            throw new IllegalArgumentException("widget can't be null");
        }
        mActivity = activity;
        activity.getEventReceiver().addListener(mActivityEventsListener);
        mWidget = widget;
    }

    public void setViewSize(int width, int height) {
        mViewWidth = width;
        mViewHeight = height;
    }

    @TargetApi(19)
    @Override
    public void useImmersiveMode(boolean use) {
        //SXRf takes care of it
    }

    @Override
    public ApplicationListener getApplicationListener() {
        return mListener;
    }

    @Override
    public Audio getAudio() {
        return mAudio;
    }

    @Override
    public Files getFiles() {
        return mFiles;
    }

    @Override
    public AndroidGraphics getGraphics() {
        return mGraphics;
    }

    @Override
    public AndroidInput getInput() {
        return mInput;
    }

    @Override
    public Net getNet() {
        return mNet;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.Android;
    }

    @Override
    public int getVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    @Override
    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();
    }

    @Override
    public long getNativeHeap() {
        return Debug.getNativeHeapAllocatedSize();
    }

    @Override
    public Preferences getPreferences(String name) {
        return new AndroidPreferences(mActivity.getSharedPreferences(name,
                Context.MODE_PRIVATE));
    }

    AndroidClipboard clipboard;

    @Override
    public Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = new AndroidClipboard(mActivity);
        }
        return clipboard;
    }

    @Override
    public void postRunnable(Runnable runnable) {
        synchronized (mRunnables) {
            mRunnables.add(runnable);
            Gdx.graphics.requestRendering();
        }
    }

    @Override
    public void exit() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mActivity.finish();
            }
        });
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (mLifecycleListeners) {
            mLifecycleListeners.add(listener);
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (mLifecycleListeners) {
            mLifecycleListeners.removeValue(listener, true);
        }
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public Array<Runnable> getRunnables() {
        return mRunnables;
    }

    @Override
    public Array<Runnable> getExecutedRunnables() {
        return mExecutedRunnables;
    }

    @Override
    public Array<LifecycleListener> getLifecycleListeners() {
        return mLifecycleListeners;
    }

    @Override
    public Window getApplicationWindow() {
        return mActivity.getWindow();
    }

    @Override
    public Handler getHandler() {
        return mHandler;
    }

    public View getWidgetView() {
        return mWidgetView;
    }

    public int getWidth() {
        return mViewWidth;
    }

    public int getHeight() {
        return mViewHeight;
    }

    public int getTextureId() {
        return mWidget.getTexId();
    }

    public void setMain(SXRMain main) {
        mMain = main;
    }

    public ITouchEvents getTouchHandler()
    {
        return touchHandler;
    }

    private void prepareGdx() {
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        mGraphics = new AndroidGraphics(this, config,
                config.resolutionStrategy == null ? new FillResolutionStrategy()
                        : config.resolutionStrategy, mEGLContext);
        mInput = AndroidInputFactory.newAndroidInput(this, mActivity, mGraphics.getView(), config);

        mAudio = new AndroidAudio(mActivity, config);
        mActivity.getFilesDir(); // workaround for Android bug #10515463
        mFiles = new AndroidFiles(mActivity.getAssets(), mActivity.getFilesDir().getAbsolutePath());
        mNet = new AndroidNet(this);
        mListener = mWidget;
        mHandler = new Handler();
        mUseImmersiveMode = config.useImmersiveMode;
        mHideStatusBar = config.hideStatusBar;

        // Add a specialized audio lifecycle listener
        addLifecycleListener(new LifecycleListener() {
            @Override
            public void resume() {
                mAudio.resume();
            }
            @Override
            public void pause() {
                mAudio.pause();
            }
            @Override
            public void dispose() {
                mAudio.dispose();
            }
        });

        if (this.mUseImmersiveMode
                && getVersion() >= Build.VERSION_CODES.KITKAT) {
            try {
                Class<?> vlistener = Class
                        .forName("com.badlogic.gdx.backends.android.AndroidVisibilityListener");
                Object o = vlistener.newInstance();
                Method method = vlistener.getDeclaredMethod("createListener",
                        AndroidApplicationBase.class);
                method.invoke(o, this);
            } catch (Exception e) {
                log("AndroidApplication",
                        "Failed to create AndroidVisibilityListener", e);
            }
        }

        Gdx.app = this;
        Gdx.input = getInput();
        Gdx.audio = getAudio();
        Gdx.files = getFiles();
        Gdx.graphics = getGraphics();
        Gdx.net = getNet();

        mGraphics.setFramebuffer(mViewWidth, mViewHeight);
        mWidgetView = (GLSurfaceView) mGraphics.getView();
        mActivity.addContentView(mWidgetView, new FrameLayout.LayoutParams(mViewWidth, mViewHeight));

        this.mIsWaitingForAudio = true;
        if (this.mWasFocusChanged == 1 || this.mWasFocusChanged == -1) {
            // this.audio.resume();
            this.mIsWaitingForAudio = false;
        }
    }

    @Override
    public void debug(String tag, String msg) {
        Log.d(TAG, tag + ": " + msg);
    }

    @Override
    public void debug(String tag, String msg, Throwable thr) {
        Log.d(TAG, tag + ": " + msg + "; " + thr);
        thr.printStackTrace();
    }

    @Override
    public void error(String tag, String msg) {
        Log.e(TAG, tag + ": " + msg);
    }

    @Override
    public void error(String tag, String msg, Throwable thr) {
        Log.e(TAG, tag + ": " + msg + "; " + thr);
        thr.printStackTrace();
    }

    @Override
    public int getLogLevel() {
        return 0;
    }

    @Override
    public void log(String tag, String msg) {
        Log.i(TAG, tag + ": " + msg);
    }

    @Override
    public void log(String tag, String msg, Throwable thr) {
        Log.i(TAG, tag + ": " + msg + "; " + thr);
        thr.printStackTrace();
    }

    @Override
    public void setLogLevel(int arg0) {
    }

    @Override
    public WindowManager getWindowManager() {
        return mActivity.getWindowManager();
    }

    @Override
    public void runOnUiThread(Runnable arg0) {
        mActivity.runOnUiThread(arg0);
    }

    @Override
    public void startActivity(Intent arg0) {
        mActivity.startActivity(arg0);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return mActivity.getSXRContext().getInputManager().dispatchMotionEvent(event);
    }

    private ITouchEvents touchHandler = new SXREventListeners.TouchEvents()
    {
        private float mHitX = 0;
        private float mHitY = 0;
        private float mActionDownX = 0;
        private float mActionDownY = 0;
        private SXRSceneObject mPicked = null;
        private SXRSceneObject mTouched = null;

        public void onEnter(SXRSceneObject sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            if (sceneObject instanceof SXRWidgetSceneObject)
            {
                mPicked = sceneObject;
            }
        }

        public void onExit(SXRSceneObject sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            if (sceneObject == mPicked)
            {
                mPicked = null;
            }
        }

        public void onTouchStart(SXRSceneObject sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            final MotionEvent event = pickInfo.motionEvent;

            if (event == null)
            {
                return;
            }
            Log.d("TOUCH", "onTouchStart");
            if (sceneObject == mPicked)
            {
                final float[] texCoords = pickInfo.getTextureCoords();

                mActionDownX = event.getRawX() - mWidgetView.getLeft();
                mActionDownY = event.getRawY() - mWidgetView.getTop();
                mHitX = texCoords[0] * getWidth();
                mHitY = texCoords[1] * getHeight();
                mTouched = sceneObject;
                dispatchPickerInputEvent(event, mHitX, mHitY);
            }
            else
            {
                onMotionOutside(pickInfo.picker, event);
            }
        }

        public void onInside(SXRSceneObject sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            if (!pickInfo.isTouched())
            {
                return;
            }
            if (sceneObject == mTouched)
            {
                onDrag(pickInfo);
            }
            else if ((sceneObject != mPicked) && (pickInfo.motionEvent != null))
            {
                onMotionOutside(pickInfo.picker, pickInfo.motionEvent);
            }
        }

        public void onTouchEnd(SXRSceneObject sceneObject, SXRPicker.SXRPickedObject pickInfo)
        {
            Log.d("TOUCH", "onTouchEnd");
            if (mTouched != null)
            {
                onDrag(pickInfo);
                mTouched = null;
            }
            else if (pickInfo.motionEvent != null)
            {
                onMotionOutside(pickInfo.picker, pickInfo.motionEvent);
            }
        }

        public void onDrag(SXRPicker.SXRPickedObject pickInfo)
        {
            if (pickInfo.motionEvent != null)
            {
                final MotionEvent event = pickInfo.motionEvent;
                final float[] texCoords = pickInfo.getTextureCoords();
                float x = event.getRawX() - mWidgetView.getLeft();
                float y = event.getRawY() - mWidgetView.getTop();

                /*
                 * When we get events from the Gear controller we replace the location
                 * with the current hit point since the pointer coordinates in
                 * these events are all zero.
                 */
                if ((pickInfo.getPicker().getController().getControllerType() == SXRControllerType.CONTROLLER) &&
                        (event.getButtonState() == MotionEvent.BUTTON_SECONDARY))
                {
                    x = texCoords[0] * getWidth();
                    y = texCoords[1] * getHeight();
                }
                /*
                 * The pointer values in other events are not with respect to the view.
                 * Here we make the event location relative to the hit point where
                 * the button went down.
                 */
                else
                {
                    x += mHitX - mActionDownX;
                    y += mHitY - mActionDownY;
                }
                dispatchPickerInputEvent(event, x, y);
            }
        }

        public void onMotionOutside(SXRPicker picker, MotionEvent e)
        {
            dispatchPickerInputEvent(e);
        }

        public void dispatchPickerInputEvent(final MotionEvent e)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Log.d("TOUCH", "dispatchPickerActivity action = %d %f, %f",
                          e.getAction(), e.getX(), e.getY());

                    mActivity.onTouchEvent(e);
                }
            });
        }

        public void dispatchPickerInputEvent(final MotionEvent e, final float x, final float y)
        {
            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    MotionEvent enew = MotionEvent.obtain(e);

                    if (e.getPointerCount() > 0)
                    {
                        enew.setLocation(x, y);
                    }
                    Log.d("TOUCH", "dispatchPicker action = %d %f, %f",
                          enew.getAction(), enew.getX(), enew.getY());
                    mInput.onTouch(mWidgetView, enew);
                    enew.recycle();
                }
            });
        }
    };

    private final static String TAG = "SXRWidgetPlugin";
}