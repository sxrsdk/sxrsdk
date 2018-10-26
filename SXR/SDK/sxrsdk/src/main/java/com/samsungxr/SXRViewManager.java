/* Copyright 2016 Samsung Electronics Co., LTD
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

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXROnFinish;
import com.samsungxr.animation.SXROpacityAnimation;
import com.samsungxr.asynchronous.SXRAsynchronousResourceLoader;
import com.samsungxr.io.SXRGearCursorController;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.script.IScriptManager;
import com.samsungxr.utility.ImageUtils;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.Threads;
import com.samsungxr.utility.VrAppSettings;
import com.samsungxr.utility.VrAppSettings.EyeBufferParams.DepthFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

abstract class SXRViewManager extends SXRContext {
    enum EYE {
        LEFT, RIGHT, MULTIVIEW, CENTER;
    }
    SXRViewManager(SXRApplication activity, SXRMain main) {
        super(activity);

        mApplication = activity;
        mMain = main;

        VrAppSettings vrAppSettings = activity.getAppSettings();
        SXRPerspectiveCamera.setDefaultFovY(vrAppSettings.getEyeBufferParams().getFovY());

        // Clear singletons and per-run data structures
        resetOnRestart();

        SXRAsynchronousResourceLoader.setup(this);
        VrAppSettings appSettings = activity.getAppSettings();
        initScriptManager();
        mEventManager = new SXREventManager(this);
        mInputManager = new SXRInputManager(this, appSettings.getCursorControllerTypes(),appSettings.getNumControllers());
        mInputManager.scanDevices();
    }

    void onPause() {
        if (null != mControllerReader) {
            mControllerReader.onPause();
        }
    }

    void onResume() {
        if (null != mControllerReader) {
            mControllerReader.onResume();
        }
    }

    void onDestroy() {
        mInputManager.close();
        if (mScriptManager != null) {
            mScriptManager.destroy();
        }

        mFrameListeners.clear();
        mRunnables.clear();
        mRunnablesPostRender.clear();
        super.onDestroy();
    }

    public SXREventManager getEventManager() {
        return mEventManager;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return mInputManager.dispatchKeyEvent(event);
    }

    public boolean dispatchMotionEvent(MotionEvent event) {
        return mInputManager.dispatchMotionEvent(event);
    }

    @Override
    public SXRScene getMainScene() {
        mMainSceneLock.lock();
        try {
            if (mState == ViewManagerState.SHOWING_SPLASH) {
                return mPendingMainScene;
            } else {
                return mMainScene;
            }
        } finally {
            mMainSceneLock.unlock();
        }
    }

    @Override
    public void setMainScene(SXRScene scene) {
        if (null == scene) {
            throw new IllegalArgumentException();
        }
        SXRScene oldScene = null;
        mMainSceneLock.lock();
        try {
            if (mState == ViewManagerState.SHOWING_SPLASH) {
                mPendingMainScene = scene;
            } else {
                oldScene = mMainScene;
                setMainSceneImpl(scene);
            }
        } finally {
            mMainSceneLock.unlock();
            if (oldScene != null)
            {
                mEventManager.sendEvent(this, IContextEvents.class, "onSceneChange", oldScene, mMainScene);
            }
        }
    }

    private void setMainSceneImpl(SXRScene scene) {
        NativeScene.setMainScene(scene.getNative());
        mApplication.setCameraRig(scene.getMainCameraRig());
        mInputManager.setScene(scene);
        this.mMainScene = scene;
    }

    protected boolean updateSensoredScene() {
        if (mSensoredScene != null && mMainScene.equals(mSensoredScene)) {
            return true;
        }

        final SXRCameraRig cameraRig = mMainScene.getMainCameraRig();

        if (null != cameraRig && (mSensoredScene == null || !mMainScene.equals(mSensoredScene))) {
            cameraRig.resetYaw();
            mSensoredScene = mMainScene;
            return true;
        }
        return false;
    }

    public final SXRInputManager getInputManager() {
        return mInputManager;
    }

    final void closeSplashScreen() {
        if (mSplashScreen != null) {
            mSplashScreen.closeSplashScreen();
        }
    }

    @Override
    public void registerDrawFrameListener(SXRDrawFrameListener frameListener) {
        if (!mFrameListeners.contains(frameListener)) {
            mFrameListeners.add(frameListener);
        }
    }

    @Override
    public void unregisterDrawFrameListener(SXRDrawFrameListener frameListener) {
        if (mFrameListeners.contains(frameListener)) {
            mFrameListeners.remove(frameListener);
        }
    }

    /**
     * Called when the surface changed size. When
     * setPreserveEGLContextOnPause(true) is called in the surface, this is
     * called only once.
     */
    void onSurfaceCreated() {
        Log.v(TAG, "onSurfaceCreated");

        Thread currentThread = Thread.currentThread();

        // Reduce contention with other Android processes
        currentThread.setPriority(Thread.MAX_PRIORITY);

        // we know that the current thread is a GL one, so we store it to
        // prevent non-GL thread from calling GL functions
        mGLThreadID = currentThread.getId();

        // Evaluating anisotropic support on GL Thread
        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        isAnisotropicSupported = extensions.contains("GL_EXT_texture_filter_anisotropic");

        // Evaluating max anisotropic value if supported
        if (isAnisotropicSupported) {
            maxAnisotropicValue = NativeTextureParameters.getMaxAnisotropicValue();
        }

        mPreviousTimeNanos = SXRTime.getCurrentTime();


        final SXRScene scene = null == mMainScene ? new SXRScene(SXRViewManager.this) : mMainScene;
        setMainSceneImpl(scene);
        mRenderBundle = makeRenderBundle();

        final DepthFormat depthFormat = mApplication.getAppSettings().getEyeBufferParams().getDepthFormat();
        mApplication.getConfigurationManager().configureRendering(DepthFormat.DEPTH_24_STENCIL_8 == depthFormat);
    }

    private void createMainScene() {
        if (mApplication.getAppSettings().showLoadingIcon) {
            mSplashScreen = mMain.createSplashScreen();
            if (mSplashScreen != null) {
                mMainSceneLock.lock();
                try {
                    mState = ViewManagerState.SHOWING_SPLASH;
                    mPendingMainScene = new SXRScene(SXRViewManager.this);
                } finally {
                    mMainSceneLock.unlock();
                }
                mMainScene.addNode(mSplashScreen);
            }
        } else {
            mSplashScreen = null;
        }
    }

    /**
     * Called on surface creation to create a properly configured render bundle
     * @return
     */
    protected SXRRenderBundle makeRenderBundle() {
        return new SXRRenderBundle(this);
    }

    /**
     * This is the code that needs to be executed before either eye is drawn.
     *
     * @return Current time, from {@link SXRTime#getCurrentTime()}
     */
    private long doMemoryManagementAndPerFrameCallbacks() {
        long currentTime = SXRTime.getCurrentTime();
        mFrameTime = (currentTime - mPreviousTimeNanos) / 1e9f;
        mPreviousTimeNanos = currentTime;

        /*
         * Without the sensor data, can't draw a scene properly.
         */
        if (!(mSensoredScene == null || !mMainScene.equals(mSensoredScene))) {
            Runnable runnable;
            while ((runnable = mRunnables.poll()) != null) {
                try {
                    runnable.run();
                } catch (final Exception exc) {
                    Log.e(TAG, "Runnable-on-GL %s threw %s", runnable, exc.toString());
                    exc.printStackTrace();
                }
            }

            final List<SXRDrawFrameListener> frameListeners = mFrameListeners;
            for (SXRDrawFrameListener listener : frameListeners) {
                try {
                    listener.onDrawFrame(mFrameTime);
                } catch (final Exception exc) {
                    Log.e(TAG, "DrawFrameListener %s threw %s", listener, exc.toString());
                    exc.printStackTrace();
                }
            }
        }

        return currentTime;
    }

    @Override
    public float getFrameTime() {
        return mFrameTime;
    }

    /*
     * Splash screen life cycle
     */

    /**
     * Efficient handling of the state machine.
     *
     * We want to be able to show an animated splash screen after
     * {@link SXRMain#onInit(SXRContext) onInit().} That means our frame
     * handler acts differently on the very first frame than it does during
     * splash screen animations, and differently again when we get to normal
     * mode. If we used a state enum and a switch statement, we'd have to keep
     * the two in synch, and we'd be spending render microseconds in a switch
     * statement, vectoring to a call to a handler. Using a interface
     * implementation instead of a state enum, we just call the handler
     * directly.
     */
    private static class FrameHandler {
        void beforeDrawEyes() {}
        void afterDrawEyes() {}
    }

    private ViewManagerState mState = ViewManagerState.RUNNING;
    private enum ViewManagerState {
        RUNNING,
        SHOWING_SPLASH
    }
    private final ReentrantLock mMainSceneLock = new ReentrantLock();

    private FrameHandler firstFrame = new FrameHandler() {
        @Override
        public void beforeDrawEyes() {
            mMain.setViewManager(SXRViewManager.this);

            createMainScene();

            // execute pending runnables now so any necessary gl calls
            // are done before onInit().  As an example the request to
            // get the GL_MAX_TEXTURE_SIZE needs to be fulfilled.
            synchronized (mRunnables) {
                Runnable runnable = null;
                while ((runnable = mRunnables.poll()) != null) {
                    try {
                        runnable.run();
                    } catch (final Exception exc) {
                        Log.e(TAG, "Runnable-on-GL %s threw %s", runnable, exc.toString());
                        exc.printStackTrace();
                    }
                }
            }

            runOnTheFrameworkThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mEventManager.sendEvent(mMain, IScriptEvents.class, "onEarlyInit", SXRViewManager.this);
                        mEventManager.sendEvent(mMain, IScriptEvents.class, "onInit", SXRViewManager.this);

                        if (null != mSplashScreen && SXRMain.SplashMode.AUTOMATIC == mMain
                                .getSplashMode() && mMain.getSplashDisplayTime() < 0f) {
                            runOnGlThread(new Runnable() {
                                public void run() {
                                    mSplashScreen.closeSplashScreen();
                                }
                            });
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        runOnGlThread(new Runnable() {
                            public void run() {
                                getActivity().finish();

                                // Just to be safe ...
                                mFrameHandler = splashFrames;
                                firstFrame = null;
                            }
                        });
                    }

                    // Trigger event "onAfterInit" for post-processing of scene
                    // graph after initialization. Also trigger "onInit" for the SXRContext.
                    mEventManager.sendEvent(mMain, IScriptEvents.class, "onAfterInit");
                    mEventManager.sendEvent(SXRViewManager.this, IContextEvents.class, "onInit", SXRViewManager.this);
                }
            });

            if (mSplashScreen == null) {
                // No splash screen, notify main scene now.
                notifyMainSceneReady();

                mFrameHandler = normalFrames;
                firstFrame = splashFrames = null;
            } else {
                mFrameHandler = splashFrames;
                firstFrame = null;
            }
        }
    };

    private FrameHandler splashFrames = new FrameHandler() {
        boolean closing;
        @Override
        public void beforeDrawEyes() {
            // splash screen post-init animations
            long currentTime = doMemoryManagementAndPerFrameCallbacks();

            if (closing) {
                return;
            }

            final boolean timeoutExpired = (null == mSplashScreen
                    || (0 <= mMain.getSplashDisplayTime() && currentTime >= mSplashScreen.mTimeout));
            if (mSplashScreen != null && (timeoutExpired || mSplashScreen.closeRequested())) {
                if (mSplashScreen.closeRequested() || mMain.getSplashMode() == SXRMain.SplashMode.AUTOMATIC) {

                    closing = true;
                    Threads.spawnLow(new Runnable() {
                        @Override
                        public void run() {
                            runOnGlThread(new Runnable() {
                                public void run() {
                                    new SXROpacityAnimation(mSplashScreen, mMain.getSplashFadeTime(), 0) //
                                            .setOnFinish(new SXROnFinish() {

                                                @Override
                                                public void finished(SXRAnimation animation) {
                                                    mMainSceneLock.lock();
                                                    try {
                                                        mState = ViewManagerState.RUNNING;
                                                        setMainScene(mPendingMainScene);
                                                        mPendingMainScene = null;
                                                    } finally {
                                                        mMainSceneLock.unlock();
                                                    }
                                                    // Splash screen finishes. Notify main
                                                    // scene it is ready.
                                                    SXRViewManager.this.notifyMainSceneReady();
                                                    mSplashScreen = null;

                                                    mFrameHandler = normalFrames;
                                                    splashFrames = null;
                                                }
                                            }) //
                                            .start(getAnimationEngine());

                                    mSplashScreen = null;
                                }
                            });
                        }
                    });
                }
            }
        }
    };

    private final FrameHandler normalFrames = new FrameHandler() {

        public void beforeDrawEyes() {
            mMainScene.resetStats();
            doMemoryManagementAndPerFrameCallbacks();

            runOnTheFrameworkThread(new Runnable() {
                public void run() {
                    try {
                        mMain.onStep();
                    } catch (final Exception exc) {
                        Log.e(TAG, "Exception from onStep: %s", exc.toString());
                        exc.printStackTrace();
                    }
                }
            });

            getInputManager().updateGearControllers();
        }

        @Override
        public void afterDrawEyes() {
            mMainScene.updateStats();
        }
    };


    // Send onInit and onAfterInit events to main scene when it is ready.
    // When there is a splash screen, it is called after the splash screen has
    // completed.
    // If there is no splash screen, it is called after SXRMain.onInit()
    // returns.
    private void notifyMainSceneReady() {
        runOnTheFrameworkThread(new Runnable() {
            @Override
            public void run() {
                // Initialize the main scene
                getEventManager().sendEvent(mMainScene, ISceneEvents.class, "onInit", SXRViewManager.this, mMainScene);

                // Late-initialize the main scene
                getEventManager().sendEvent(mMainScene, ISceneEvents.class, "onAfterInit");
            }
        });
    }

    @Override
    public void runOnGlThread(Runnable runnable) {
        if (mGLThreadID == Thread.currentThread().getId()) {
            runnable.run();
        } else {
            mRunnables.add(runnable);
        }
    }

    @Override
    public void runOnGlThreadPostRender(int delayFrames, Runnable runnable) {
        synchronized (mRunnablesPostRender) {
            mRunnablesPostRender.put(runnable, delayFrames);
        }
    }

    protected void beforeDrawEyes() {
        SXRNotifications.notifyBeforeStep();
        mFrameHandler.beforeDrawEyes();
        makeShadowMaps(mMainScene.getNative(), getMainScene(), mRenderBundle.getShaderManager().getNative(),
                       mRenderBundle.getPostEffectRenderTextureA().getWidth(), mRenderBundle.getPostEffectRenderTextureA().getHeight());
    }

    protected void afterDrawEyes() {
        // Execute post-rendering tasks (after drawing eyes, but
        // before afterDrawEyes handlers)
        synchronized (mRunnablesPostRender) {
            for (Iterator<Map.Entry<Runnable, Integer>> it = mRunnablesPostRender.entrySet().iterator(); it
                    .hasNext();) {
                Map.Entry<Runnable, Integer> entry = it.next();
                if (entry.getValue() <= 0) {
                    entry.getKey().run();
                    it.remove();
                } else {
                    entry.setValue(entry.getValue() - 1);
                }
            }
        }

        mFrameHandler.afterDrawEyes();
        finalizeUnreachableObjects();
        SXRNotifications.notifyAfterStep();
    }

    void cullAndRender(SXRRenderTarget renderTarget, SXRScene scene)
    {
        cullAndRender(renderTarget.getNative(), scene.getNative(), scene,
                mRenderBundle.getShaderManager().getNative(),
                mRenderBundle.getPostEffectRenderTextureA().getNative(),
                mRenderBundle.getPostEffectRenderTextureB().getNative());
    }

    @Override
    public IScriptManager getScriptManager() {
        return mScriptManager;
    }

    @Override
    public SXRShaderManager getShaderManager() {
        return mRenderBundle.getShaderManager();
    }

    protected SXRScreenshotCallback mScreenshotCenterCallback;
    protected SXRScreenshotCallback mScreenshotLeftCallback;
    protected SXRScreenshotCallback mScreenshotRightCallback;
    protected SXRScreenshot3DCallback mScreenshot3DCallback;

    @Override
    public void captureScreenCenter(SXRScreenshotCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null.");
        } else {
            mScreenshotCenterCallback = callback;
        }
    }

    @Override
    public void captureScreenLeft(SXRScreenshotCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null.");
        } else {
            mScreenshotLeftCallback = callback;
        }
    }

    @Override
    public void captureScreenRight(SXRScreenshotCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback should not be null.");
        } else {
            mScreenshotRightCallback = callback;
        }
    }

    @Override
    public void captureScreen3D(SXRScreenshot3DCallback callback) {
        mScreenshot3DCallback = callback;
    }

    protected void readRenderResult(SXRRenderTarget renderTarget, SXRViewManager.EYE eye, boolean useMultiview) {
        if (mReadbackBuffer == null) {
            final VrAppSettings settings = mApplication.getAppSettings();
            final VrAppSettings.EyeBufferParams eyeBufferParams = settings.getEyeBufferParams();
            mReadbackBufferWidth = eyeBufferParams.getResolutionWidth();
            mReadbackBufferHeight = eyeBufferParams.getResolutionHeight();

            mReadbackBuffer = ByteBuffer.allocateDirect(mReadbackBufferWidth * mReadbackBufferHeight * 4);
            mReadbackBuffer.order(ByteOrder.nativeOrder());
        }
        readRenderResultNative(mReadbackBuffer,renderTarget.getNative(), eye.ordinal(), useMultiview );
    }

    protected void returnScreenshotToCaller(final SXRScreenshotCallback callback, final int width, final int height) {
        // run the callback function in a background thread
        final byte[] byteArray = Arrays.copyOf(mReadbackBuffer.array(), mReadbackBuffer.array().length);
        Threads.spawn(new Runnable() {
            public void run() {
                final Bitmap capturedBitmap = ImageUtils.generateBitmapFlipV(byteArray, width, height);
                callback.onScreenCaptured(capturedBitmap);
            }
        });
    }

    protected void returnScreenshot3DToCaller(final SXRScreenshot3DCallback callback, final Bitmap[] bitmaps,
                                              final int width, final int height) {

        // run the callback function in a background thread
        Threads.spawn(new Runnable() {
            public void run() {
                final Bitmap[] bitmapArray = new Bitmap[6];
                Runnable[] threads = new Runnable[6];

                for (int i = 0; i < 6; i++) {
                    final int index = i;
                    threads[i] = new Runnable() {
                        public void run() {
                            final Bitmap bitmap = bitmaps[index];
                            bitmaps[index] = null;

                            synchronized (this) {
                                bitmapArray[index] = bitmap;
                                notify();
                            }
                        }
                    };
                }

                for (Runnable thread : threads) {
                    Threads.spawnLow(thread);
                }

                for (int i = 0; i < 6; i++) {
                    synchronized (threads[i]) {
                        if (bitmapArray[i] == null) {
                            try {
                                threads[i].wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                callback.onScreenCaptured(bitmapArray);
            }
        });
    }

    // capture 3D screenshot
    protected void capture3DScreenShot(SXRRenderTarget renderTarget, boolean isMultiview) {
        if (mScreenshot3DCallback == null) {
            return;
        }
        final Bitmap[] bitmaps = new Bitmap[6];
        renderSixCamerasAndReadback(mMainScene.getMainCameraRig(), bitmaps, renderTarget, isMultiview);
        returnScreenshot3DToCaller(mScreenshot3DCallback, bitmaps, mReadbackBufferWidth, mReadbackBufferHeight);

        mScreenshot3DCallback = null;
    }

    protected void captureRightEye(SXRRenderTarget renderTarget, boolean useMultiview) {
        captureEye(mScreenshotRightCallback, renderTarget, EYE.RIGHT, useMultiview);
        mScreenshotRightCallback = null;
    }

    protected void captureLeftEye(SXRRenderTarget renderTarget, boolean useMultiview) {
        captureEye(mScreenshotLeftCallback, renderTarget, EYE.LEFT, useMultiview);
        mScreenshotLeftCallback = null;
    }

    // capture screenshot of an eye
    private void captureEye(SXRScreenshotCallback callback, SXRRenderTarget renderTarget, SXRViewManager.EYE eye, boolean useMultiview) {
        if (null == callback) {
            return;
        }
        readRenderResult(renderTarget,eye,useMultiview);
        returnScreenshotToCaller(callback, mReadbackBufferWidth, mReadbackBufferHeight);
    }

    // capture center eye
    protected void captureCenterEye(SXRRenderTarget renderTarget, boolean isMultiview) {
        if (mScreenshotCenterCallback == null) {
            return;
        }

        // TODO: when we will use multithreading, create new camera using centercamera as we are adding posteffects into it
        final SXRCamera centerCamera = mMainScene.getMainCameraRig().getCenterCamera();
        final SXRMaterial postEffect = new SXRMaterial(this, SXRMaterial.SXRShaderType.VerticalFlip.ID);

        centerCamera.addPostEffect(postEffect);

        SXRRenderTexture posteffectRenderTextureB = null;
        SXRRenderTexture posteffectRenderTextureA = null;

        if(isMultiview) {
            posteffectRenderTextureA = mRenderBundle.getEyeCapturePostEffectRenderTextureA();
            posteffectRenderTextureB = mRenderBundle.getEyeCapturePostEffectRenderTextureB();
            renderTarget = mRenderBundle.getEyeCaptureRenderTarget();
            renderTarget.cullFromCamera(mMainScene, centerCamera ,mRenderBundle.getShaderManager());
            renderTarget.beginRendering(centerCamera);
        }
        else {
            posteffectRenderTextureA = mRenderBundle.getPostEffectRenderTextureA();
            posteffectRenderTextureB = mRenderBundle.getPostEffectRenderTextureB();
        }

        renderTarget.render(mMainScene,centerCamera, mRenderBundle.getShaderManager(), posteffectRenderTextureA, posteffectRenderTextureB);
        centerCamera.removePostEffect(postEffect);
        readRenderResult(renderTarget, EYE.MULTIVIEW, false);

        if(isMultiview)
            renderTarget.endRendering();

        final Bitmap bitmap = Bitmap.createBitmap(mReadbackBufferWidth, mReadbackBufferHeight, Bitmap.Config.ARGB_8888);
        mReadbackBuffer.rewind();
        bitmap.copyPixelsFromBuffer(mReadbackBuffer);
        final SXRScreenshotCallback callback = mScreenshotCenterCallback;
        Threads.spawn(new Runnable() {
            public void run() {
                callback.onScreenCaptured(bitmap);
            }
        });

        mScreenshotCenterCallback = null;
    }

    private void renderOneCameraAndAddToList(final SXRPerspectiveCamera centerCamera, final Bitmap[] bitmaps, int index,
                                             SXRRenderTarget renderTarget, SXRRenderTexture postEffectRenderTextureA, SXRRenderTexture postEffectRenderTextureB ) {

        renderTarget.cullFromCamera(mMainScene,centerCamera,mRenderBundle.getShaderManager());
        renderTarget.render(mMainScene,centerCamera,mRenderBundle.getShaderManager(),postEffectRenderTextureA, postEffectRenderTextureB);
        readRenderResult(renderTarget,EYE.CENTER, false);

        bitmaps[index] = Bitmap.createBitmap(mReadbackBufferWidth, mReadbackBufferHeight, Bitmap.Config.ARGB_8888);
        mReadbackBuffer.rewind();
        bitmaps[index].copyPixelsFromBuffer(mReadbackBuffer);
    }

    private void renderSixCamerasAndReadback(final SXRCameraRig mainCameraRig, final Bitmap[] bitmaps, SXRRenderTarget renderTarget, boolean isMultiview) {
        // temporarily create a center camera
        SXRPerspectiveCamera centerCamera = new SXRPerspectiveCamera(this);
        centerCamera.setFovY(90.0f);
        centerCamera.setRenderMask(SXRRenderData.SXRRenderMaskBit.Left | SXRRenderData.SXRRenderMaskBit.Right);
        SXRNode centerCameraObject = new SXRNode(this);

        centerCameraObject.attachCamera(centerCamera);
        centerCamera.addPostEffect(new SXRMaterial(this, SXRMaterial.SXRShaderType.VerticalFlip.ID));

        mainCameraRig.getOwnerObject().addChildObject(centerCameraObject);
        SXRRenderTexture posteffectRenderTextureB = null;
        SXRRenderTexture posteffectRenderTextureA = null;
        SXRTransform centerCameraTransform = centerCameraObject.getTransform();

        if(isMultiview) {
            renderTarget = mRenderBundle.getEyeCaptureRenderTarget();
            posteffectRenderTextureA = mRenderBundle.getEyeCapturePostEffectRenderTextureA();
            posteffectRenderTextureB = mRenderBundle.getEyeCapturePostEffectRenderTextureB();
            renderTarget.beginRendering(centerCamera);
        }
        else {
            posteffectRenderTextureA = mRenderBundle.getPostEffectRenderTextureA();
            posteffectRenderTextureB = mRenderBundle.getPostEffectRenderTextureB();
        }

        int index = 0;
        // render +x face
        centerCameraTransform.rotateByAxis(-90, 0, 1, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        // render -x face
        centerCameraTransform.rotateByAxis(180, 0, 1, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        // render +y face
        centerCameraTransform.rotateByAxis(-90, 0, 1, 0);
        centerCameraTransform.rotateByAxis(90, 1, 0, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        // render -y face
        centerCameraTransform.rotateByAxis(180, 1, 0, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        // render +z face
        centerCameraTransform.rotateByAxis(90, 1, 0, 0);
        centerCameraTransform.rotateByAxis(180, 0, 1, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        // render -z face
        centerCameraTransform.rotateByAxis(180, 0, 1, 0);
        renderOneCameraAndAddToList(centerCamera, bitmaps, index++, renderTarget, posteffectRenderTextureA, posteffectRenderTextureB);
        centerCameraObject.detachCamera();
        mainCameraRig.getOwnerObject().removeChildObject(centerCameraObject);
        if(isMultiview)
            renderTarget.endRendering();
    }

    protected void captureFinish() {
        if (mScreenshotLeftCallback == null && mScreenshotRightCallback == null
                && mScreenshotCenterCallback == null && mScreenshot3DCallback == null) {
            mReadbackBuffer = null;
        }
    }

    private void initScriptManager() {
        try
        {
            final Class<?> cls = Class.forName("com.samsungxr.script.SXRScriptManager");
            mScriptManager = (IScriptManager)cls.getConstructor(SXRContext.class).newInstance(this);
        }
        catch (Exception e) {
        }
    }

    SXRGearCursorController.ControllerReader mControllerReader;
    private IScriptManager mScriptManager = null;
    protected final SXRApplication mApplication;
    protected float mFrameTime;
    protected long mPreviousTimeNanos;

    protected FrameHandler mFrameHandler = firstFrame;

    protected List<SXRDrawFrameListener> mFrameListeners = new CopyOnWriteArrayList<SXRDrawFrameListener>();
    protected final Queue<Runnable> mRunnables = new LinkedBlockingQueue<Runnable>();
    protected final Map<Runnable, Integer> mRunnablesPostRender = new HashMap<Runnable, Integer>();

    protected SXRScene mMainScene;
    protected SXRScene mPendingMainScene;
    protected SXRScene mSensoredScene;

    protected SplashScreen mSplashScreen;

    private final SXREventManager mEventManager;
    protected final SXRInputManager mInputManager;
    protected SXRRenderBundle mRenderBundle;

    protected SXRMain mMain;

    protected ByteBuffer mReadbackBuffer;
    protected int mReadbackBufferWidth;
    protected int mReadbackBufferHeight;

    protected native void makeShadowMaps(long scene, SXRScene javaNode, long shader_manager, int width, int height);
    protected native void cullAndRender(long render_target, long scene, SXRScene javaNode, long shader_manager, long postEffectRenderTextureA, long postEffectRenderTextureB);
    private native static void readRenderResultNative(Object readbackBuffer, long renderTarget, int eye, boolean useMultiview);

    private static final String TAG = "SXRViewManager";

}

