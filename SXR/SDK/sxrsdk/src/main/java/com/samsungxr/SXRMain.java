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

import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.io.SXRTouchPadGestureListener;
import com.samsungxr.script.IScriptFile;
import com.samsungxr.script.IScriptable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;

/**
 * Extend this class to create a SXRF application.
 * <p>
 * All methods are called from the GL thread so it is safe to make GL calls
 * either directly or indirectly (through SXRF methods). The GL thread runs at
 * {@linkplain Thread#MAX_PRIORITY top priority:} Android systems have many
 * processes running at any time, and all {@linkplain Thread#NORM_PRIORITY
 * default priority} threads compete with each other.
 */
public abstract class SXRMain implements IScriptEvents, IScriptable, IEventReceiver {
    /**
     * Default minimum time for splash screen to show: returned by
     * {@link #getSplashDisplayTime()}.
     */
    private static final float DEFAULT_SPLASH_DISPLAY_SECONDS = -1f;

    /**
     * Default fade-to-transparency time for the splash screen: returned by
     * {@link #getSplashFadeTime()}.
     */
    private static final float DEFAULT_SPLASH_FADE_SECONDS = 0.9f;

    /** Splash screen, distance from the camera. */
    private static final float DEFAULT_SPLASH_Z = -1.25f;

    private final SXREventReceiver mEventReceiver = new SXREventReceiver(this);
    private volatile SXRViewManager mViewManager;

    /*
     * Core methods, that you must override.
     */

    /**
     * Called before {@link #onInit(SXRContext)}.
     *
     * This is used for initializing plug-ins and other early components.
     */
    @Override
    public void onEarlyInit(SXRContext gvrContext) {
    }

    /**
     * Called when the GL surface is created, when your app is loaded.
     *
     * This is where you should build your initial scene graph. Any expensive
     * calls you make here are 'hidden' (in the sense that they won't cause the
     * app to skip any frames) but they <em>will</em> still affect app startup
     * time: use lazy-create patterns where you can, and/or use the asynchronous
     * resource loading methods in {@link SXRContext} instead of the synchronous
     * ones.
     *
     * @throws Throwable
     *             {@code onInit()} routines typically need to load various
     *             resources. Some of the Android resource-loading code throws
     *             exceptions (especially when you are loading files from the
     *             {@code assets} folder). If you don't catch these exceptions -
     *             and just let them propagate out of {@code onInit()} - SXRF
     *             will log the exception and shutdown your app.
     *
     *             <p>
     *             This is probably <em>not</em> the behavior you want if your
     *             resources may fail to load because of (say) network issues,
     *             but it is just fine for handling development-time issues like
     *             typing {@code "mesh.obi"} instead of {@code "mesh.obj"}.
     */
    @Override
    public void onInit(SXRContext gvrContext) throws Throwable {
    }

    /**
     * Called after {@code onInit()} has finished.
     *
     * This is where you do some post-processing of the initial scene graph
     * created in the method {@link #onInit(SXRContext)}, a listener added to
     * {@link SXREventReceiver} or a {@link com.samsungxr.script.IScriptFile} attached to this {@link
     * SXRMain} using {@link com.samsungxr.script.IScriptManager#attachScriptFile(IScriptable, IScriptFile)}.
     */
    @Override
    public void onAfterInit() {
    }

    /**
     * Called when the script is attached to a target using {@link com.samsungxr.script.IScriptManager#attachScriptFile(IScriptable, IScriptFile)}.
     */
    @Override
    public void onAttach(IScriptable target) {
    }

    /**
     * Called when the script is detached from the target using {@link com.samsungxr.script.IScriptManager#detachScriptFile(IScriptable)}.
     */
    @Override
    public void onDetach(IScriptable target) {
    }

    /**
     * Called every frame.
     *
     * This is where you start animations, and where you add or change
     * {@linkplain SXRNode scene objects.} Keep this method as short as
     * possible, to avoid dropping any frames.
     *
     * <p>
     * This is the 3rd user-definable step in drawing a frame:
     * <ul>
     * <li>Process the {@link SXRContext#runOnGlThread(Runnable)} queue
     * <li>Run all
     * {@linkplain SXRContext#registerDrawFrameListener(SXRDrawFrameListener)
     * registered frame listeners}
     * <li><b>Call your {@code onStep()} handler</b>.
     * </ul>
     *
     * After these steps, {@link SXRViewManager} does stereo rendering and
     * applies the lens distortion.
     */
    @Override
    public void onStep() {
    }

    @Override
    public SXREventReceiver getEventReceiver() {
        return mEventReceiver;
    }

    /**
     * Handle back key press
     * @return true if handled by the app
     */
    public boolean onBackPress() {
        return false;
    }

    /**
     * Handle swipe events by implementing this method.
     * @see SXRActivity#enableGestureDetector()
     * @see SXRTouchPadGestureListener
     * @param action
     * @param vx
     */
    public void onSwipe(SXRTouchPadGestureListener.Action action, float vx) {}

    /**
     * Handle single tap events by implementing this method.
     * @see SXRActivity#enableGestureDetector()
     * @see SXRTouchPadGestureListener
     * @param event
     */
    public void onSingleTapUp(MotionEvent event) {}

    /*
     * Splash screen support: methods to call or overload to change the default
     * splash screen behavior
     */

    /**
     * Whether the splash screen should be displayed, and for how long.
     *
     * Returned by {@link #getSplashMode}.
     *
     * @since 1.6.4
     */
    public enum SplashMode {
        /**
         * The splash screen will be shown before
         * {@link SXRMain#onInit(SXRContext) onInit()} and hidden before the
         * first call to {@link SXRMain#onStep() onStep()}
         */
        AUTOMATIC,
        /**
         * The splash screen will be shown before
         * {@link SXRMain#onInit(SXRContext) onInit()} and will remain up
         * until you call {@link SXRMain#closeSplashScreen()}
         */
        MANUAL,
        /**
         * The splash screen will not be shown at all. The screen will go black
         * until your {@link SXRMain#onInit(SXRContext) onInit()} returns, at
         * which point it will show any objects you have created, over whatever
         * {@linkplain SXRCamera#setBackgroundColor(int) background color} you
         * have set.
         */
        NONE
    }

    /**
     * Override this method to change the splash mode from the default
     * {@linkplain SplashMode#AUTOMATIC automatic} mode.
     *
     * @return One of the {@link SplashMode} enums.
     *
     * @since 1.6.4
     */
    public SplashMode getSplashMode() {
        return SplashMode.AUTOMATIC;
    }

    /**
     * The minimum amount of time the splash screen will be visible, in seconds.
     *
     * Override this method to change the default.
     *
     * If the value returned by {@link #getSplashDisplayTime()} is positive then the splash screen
     * will stay up for  {@link #getSplashDisplayTime()} seconds in
     * {@linkplain SplashMode#AUTOMATIC AUTOMATIC} mode.
     * In {@linkplain SplashMode#MANUAL MANUAL} mode, the splash screen will stay
     * up for <em>at least</em> {@link #getSplashDisplayTime()} seconds (if positive):
     * {@link #closeSplashScreen()} will not take effect until the splash screen
     * times out, even if you call it long before that timeout.
     *
     * @return The minimum splash screen display time, in seconds. Any value less than 0 will
     * result in the splash screen closing as soon as the framework is ready to render the main
     * scene. The default value returned by this call is -1.
     */
    public float getSplashDisplayTime() {
        return DEFAULT_SPLASH_DISPLAY_SECONDS;
    }

    /**
     * Splash screen fade time, in seconds.
     *
     * Override this method to change the default.
     *
     * @return Splash screen fade-out animation duration
     */
    public float getSplashFadeTime() {
        return DEFAULT_SPLASH_FADE_SECONDS;
    }

    /**
     * In {@linkplain SplashMode#MANUAL manual mode,} the splash screen will
     * stay up until you call this method.
     *
     * Calling {@link #closeSplashScreen()} before the
     * {@linkplain #getSplashDisplayTime() display time} has elapsed will set a
     * flag, but the splash screen will stay up until the timeout; after the
     * timeout, the splash screen will close as soon as you call
     * {@link #closeSplashScreen()}.
     *
     * @since 1.6.4
     */
    public final void closeSplashScreen() {
        mViewManager.closeSplashScreen();
    }

    /**
     * Override this method to supply a custom splash screen image.
     *
     * @param gvrContext
     *            The new {@link SXRContext}
     * @return Texture to display
     *
     * @since 1.6.4
     */
    public SXRTexture getSplashTexture(SXRContext gvrContext) {
        Bitmap bitmap = BitmapFactory.decodeResource( //
                gvrContext.getContext().getResources(), //
                R.drawable.__default_splash_screen__);
        SXRTexture tex = new SXRTexture(gvrContext);
        tex.setImage(new SXRBitmapImage(gvrContext, bitmap));
        return tex;
    }

    /**
     * Override this method to supply a custom splash screen mesh.
     *
     * The default is a 1x1 quad.
     *
     * @param gvrContext
     *            The new {@link SXRContext}
     * @return Mesh to use with {@link #getSplashTexture(SXRContext)} and
     *         {@link #getSplashShader(SXRContext)}.
     *
     * @since 1.6.4
     */
    public SXRMesh getSplashMesh(SXRContext gvrContext) {
        return gvrContext.createQuad(1f, 1f);
    }

    /**
     * Override this method to supply a custom splash screen shader.
     *
     * The default is the built-in {@linkplain SXRMaterial.SXRShaderType.Texture
     * unlit shader.}
     *
     * @param gvrContext
     *            The new {@link SXRContext}
     * @return Shader to use with {@link #getSplashTexture(SXRContext)} and
     *         {@link #getSplashMesh(SXRContext)}.
     *
     * @since 1.6.4
     */
    public SXRShaderId getSplashShader(SXRContext gvrContext) {
        return SXRMaterial.SXRShaderType.Texture.ID;
    }

    /**
     * Override this method to change the default splash screen size or
     * position.
     *
     * This method will be called <em>before</em> {@link #onInit(SXRContext)
     * onInit()} and before the normal render pipeline starts up. In particular,
     * this means that any {@linkplain SXRAnimation animations} will not start
     * until the first {@link #onStep()} and normal rendering starts.
     *
     * @param splashScreen
     *            The splash object created from
     *            {@link #getSplashTexture(SXRContext)},
     *            {@link #getSplashMesh(SXRContext)}, and
     *            {@link #getSplashShader(SXRContext)}.
     *
     * @since 1.6.4
     */
    public void onSplashScreenCreated(SXRNode splashScreen) {
        SXRTransform transform = splashScreen.getTransform();
        transform.setPosition(0, 0, DEFAULT_SPLASH_Z);
    }

    SplashScreen createSplashScreen() {
        if (getSplashMode() == SplashMode.NONE) {
            return null;
        }

        SplashScreen splashScreen = new SplashScreen(mViewManager, //
                getSplashMesh(mViewManager), //
                getSplashTexture(mViewManager), //
                getSplashShader(mViewManager), //
                this);
        splashScreen.getRenderData().setRenderingOrder(
                SXRRenderData.SXRRenderingOrder.OVERLAY);
        onSplashScreenCreated(splashScreen);
        return splashScreen;
    }

    void setViewManager(final SXRViewManager viewManager) {
        mViewManager = viewManager;
    }

    /**
     * Convenience method to get to the SXR context this instance is associated with
     * @return the context if initialized and ready, null otherwise
     */
    public final SXRContext getSXRContext() {
        return mViewManager;
    }
}
