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
import android.util.DisplayMetrics;

import com.samsungxr.debug.SXRFPSTracer;
import com.samsungxr.debug.SXRMethodCallTracer;
import com.samsungxr.debug.SXRStatsLine;
import com.samsungxr.io.SXRGearCursorController;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.VrAppSettings;

/*
 * This is the most important part of gvrf.
 * Initialization can be told as 2 parts. A General part and the GL part.
 * The general part needs nothing special but the GL part needs a GL context.
 * Since something being done while the GL context creates a surface is time-efficient,
 * the general initialization is done in the constructor and the GL initialization is
 * done in onSurfaceCreated().
 * 
 * After the initialization, gvrf works with 2 types of threads.
 * Input threads, and a GL thread.
 * Input threads are about the sensor, joysticks, and keyboards. They send data to gvrf.
 * gvrf handles those data as a message. It saves the data, doesn't do something
 * immediately. That's because gvrf is built to do everything about the scene in the GL thread.
 * There might be some pros by doing some rendering related stuffs outside the GL thread,
 * but since I thought simplicity of the structure results in efficiency, I didn't do that.
 * 
 * Now it's about the GL thread. It lets the user handle the scene by calling the users SXRMain.onStep().
 * There are also SXRFrameListeners, SXRAnimationEngine, and Runnables but they aren't that special.
 */

class OvrViewManager extends SXRViewManager {

    private static final String TAG = Log.tag(OvrViewManager.class);

    protected OvrLensInfo mLensInfo;

    // Statistic debug info
    private SXRStatsLine mStatsLine;
    private SXRFPSTracer mFPSTracer;
    private SXRMethodCallTracer mTracerBeforeDrawEyes;
    private SXRMethodCallTracer mTracerAfterDrawEyes;
    private SXRMethodCallTracer mTracerDrawEyes;
    private SXRMethodCallTracer mTracerDrawEyes1;
    private SXRMethodCallTracer mTracerDrawEyes2;
    private SXRMethodCallTracer mTracerDrawFrame;
    private SXRMethodCallTracer mTracerDrawFrameGap;
    private SXRGearCursorController[] gearControllers;

    /**
     * Constructs OvrViewManager object with SXRMain which controls GL
     * activities
     *
     * @param application
     *            Current activity object
     * @param gvrMain
     *            {@link SXRMain} which describes
     */
    OvrViewManager(SXRApplication application, SXRMain gvrMain, OvrXMLParser xmlParser) {
        super(application, gvrMain);

        // Apply view manager preferences
        SXRPreference prefs = SXRPreference.get();
        DEBUG_STATS = prefs.getBooleanProperty(SXRPreference.KEY_DEBUG_STATS, false);
        DEBUG_STATS_PERIOD_MS = prefs.getIntegerProperty(SXRPreference.KEY_DEBUG_STATS_PERIOD_MS, 1000);
        try {
            SXRStatsLine.sFormat = SXRStatsLine.FORMAT
                    .valueOf(prefs.getProperty(SXRPreference.KEY_STATS_FORMAT, SXRStatsLine.FORMAT.DEFAULT.toString()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        /*
         * Sets things with the numbers in the xml.
         */
        DisplayMetrics metrics = new DisplayMetrics();
        application.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final float INCH_TO_METERS = 0.0254f;
        int screenWidthPixels = metrics.widthPixels;
        int screenHeightPixels = metrics.heightPixels;
        float screenWidthMeters = (float) screenWidthPixels / metrics.xdpi * INCH_TO_METERS;
        float screenHeightMeters = (float) screenHeightPixels / metrics.ydpi * INCH_TO_METERS;
        VrAppSettings vrAppSettings = application.getAppSettings();
        mLensInfo = new OvrLensInfo(screenWidthPixels, screenHeightPixels, screenWidthMeters, screenHeightMeters,
                vrAppSettings);

        // Debug statistics
        mStatsLine = new SXRStatsLine("sxr-stats");

        mFPSTracer = new SXRFPSTracer("DrawFPS");
        mTracerDrawFrame = new SXRMethodCallTracer("drawFrame");
        mTracerDrawFrameGap = new SXRMethodCallTracer("drawFrameGap");
        mTracerBeforeDrawEyes = new SXRMethodCallTracer("beforeDrawEyes");
        mTracerDrawEyes = new SXRMethodCallTracer("drawEyes");
        mTracerDrawEyes1 = new SXRMethodCallTracer("drawEyes1");
        mTracerDrawEyes2 = new SXRMethodCallTracer("drawEyes2");
        mTracerAfterDrawEyes = new SXRMethodCallTracer("afterDrawEyes");

        mStatsLine.addColumn(mFPSTracer.getStatColumn());
        mStatsLine.addColumn(mTracerDrawFrame.getStatColumn());
        mStatsLine.addColumn(mTracerDrawFrameGap.getStatColumn());
        mStatsLine.addColumn(mTracerBeforeDrawEyes.getStatColumn());
        mStatsLine.addColumn(mTracerDrawEyes.getStatColumn());
        mStatsLine.addColumn(mTracerDrawEyes1.getStatColumn());
        mStatsLine.addColumn(mTracerDrawEyes2.getStatColumn());
        mStatsLine.addColumn(mTracerAfterDrawEyes.getStatColumn());

        mControllerReader = new OvrControllerReader(application, mApplication.getActivityNative().getNative());
        for (int i = 0; i < vrAppSettings.getNumControllers(); i++)
        {
            SXRGearCursorController gearController = new SXRGearCursorController(this, i);
            if (mInputManager.addExternalController(gearController))
            {
                gearController.attachReader(mControllerReader);
            }
        }

    }

    void onSurfaceChanged(int w, int h) {
        Log.v(TAG, "onSurfaceChanged");

        final VrAppSettings.EyeBufferParams.DepthFormat depthFormat = mApplication.getAppSettings().getEyeBufferParams().getDepthFormat();
        mApplication.getConfigurationManager().configureRendering(VrAppSettings.EyeBufferParams.DepthFormat.DEPTH_24_STENCIL_8 == depthFormat);

        boolean isMultiview = mApplication.getAppSettings().isMultiviewSet();
        int width = mApplication.getAppSettings().getFramebufferPixelsWide();
        int height= mApplication.getAppSettings().getFramebufferPixelsHigh();
        for(int i=0;i < 3; i++) {
            if(isMultiview){
                long renderTextureInfo = getRenderTextureInfo(mApplication.getActivityNative().getNative(), i, EYE.MULTIVIEW.ordinal());
                mRenderBundle.createRenderTarget(i, EYE.MULTIVIEW, new SXRRenderTexture(mApplication.getSXRContext(),  width , height,
                        SXRRenderBundle.getRenderTextureNative(renderTextureInfo)));
            }
            else {
                long renderTextureInfo = getRenderTextureInfo(mApplication.getActivityNative().getNative(), i, EYE.LEFT.ordinal());
                mRenderBundle.createRenderTarget(i, EYE.LEFT, new SXRRenderTexture(mApplication.getSXRContext(),  width , height,
                        SXRRenderBundle.getRenderTextureNative(renderTextureInfo)));
                renderTextureInfo = getRenderTextureInfo(mApplication.getActivityNative().getNative(), i, EYE.RIGHT.ordinal());
                mRenderBundle.createRenderTarget(i, EYE.RIGHT, new SXRRenderTexture(mApplication.getSXRContext(),  width , height,
                        SXRRenderBundle.getRenderTextureNative(renderTextureInfo)));
            }
        }

        mRenderBundle.createRenderTargetChain(isMultiview);

        initialize(mApplication.getActivityNative().getNative(),
                mRenderBundle.getShaderManager().getNative(),
                mRenderBundle.getPostEffectRenderTextureA().getNative(),
                mRenderBundle.getPostEffectRenderTextureB().getNative());
    }

    @Override
    protected void beforeDrawEyes() {
        if (DEBUG_STATS) {
            mStatsLine.startLine();

            mTracerDrawFrame.enter();
            mTracerDrawFrameGap.leave();

            mTracerBeforeDrawEyes.enter();
        }

        super.beforeDrawEyes();

        if (DEBUG_STATS) {
            mTracerBeforeDrawEyes.leave();
        }
    }

    /** Called once per frame */
    protected void onDrawFrame() {
        drawEyes(mApplication.getActivityNative().getNative(), getMainScene());
        afterDrawEyes();
    }

    @Override
    protected void afterDrawEyes() {
        if (DEBUG_STATS) {
            // Time afterDrawEyes from here
            mTracerAfterDrawEyes.enter();
        }

        super.afterDrawEyes();

        if (DEBUG_STATS) {
            mTracerAfterDrawEyes.leave();

            mTracerDrawFrame.leave();
            mTracerDrawFrameGap.enter();

            mFPSTracer.tick();
            mStatsLine.printLine(DEBUG_STATS_PERIOD_MS);

            mMainScene.addStatMessage(System.lineSeparator() + mStatsLine.getStats(SXRStatsLine.FORMAT.MULTILINE));
        }
    }

    private SXRRenderTarget getRenderTarget(final int eye, final int swapChainIndex, final boolean isMultiview) {
        final SXRViewManager.EYE rtEye;
        if (isMultiview) {
            rtEye = EYE.MULTIVIEW;
        } else {
            rtEye = 0 == eye ? EYE.LEFT : EYE.RIGHT;
        }
        return mRenderBundle.getRenderTarget(rtEye, swapChainIndex);
    }

    @SuppressWarnings("unused") //called from native
    void captureCenterEye(final int eye, final int swapChainIndex, final boolean isMultiview) {
        final SXRRenderTarget rt = getRenderTarget(eye, swapChainIndex, isMultiview);
        super.captureCenterEye(rt, isMultiview);
    }

    @SuppressWarnings("unused") //called from native
    void captureLeftEye(final int eye, final int swapChainIndex, final boolean isMultiview) {
        final SXRRenderTarget rt = getRenderTarget(eye, swapChainIndex, isMultiview);
        super.captureLeftEye(rt, isMultiview);
    }

    @SuppressWarnings("unused") //called from native
    void captureRightEye(final int eye, final int swapChainIndex, final boolean isMultiview) {
        final SXRRenderTarget rt = getRenderTarget(eye, swapChainIndex, isMultiview);
        super.captureRightEye(rt, isMultiview);
    }

    @SuppressWarnings("unused") //called from native
    void capture3DScreenShot(final int eye, final int swapChainIndex, final boolean isMultiview) {
        final SXRRenderTarget rt = getRenderTarget(eye, swapChainIndex, isMultiview);
        super.capture3DScreenShot(rt, isMultiview);
    }

    /**
     * Reset the Oculus head & controller poses
     */
    public void recenterPose() {
        recenterPose(mApplication.getActivityNative().getNative());
    }

    private native void initialize(long aNative, long materialShaderManager,
                                   long postEffectRenderTextureA, long postEffectRenderTextureB);
    private native long getRenderTextureInfo(long ptr, int index, int eye );
    private native void drawEyes(long ptr, SXRScene mainScene);
    private native void recenterPose(long ptr);
}
