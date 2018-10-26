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
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Gravity;

import com.samsungxr.SXRHybridObject.NativeCleanupHandler;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimationEngine;
import com.samsungxr.animation.SXRMaterialAnimation;
import com.samsungxr.animation.SXROnFinish;
import com.samsungxr.debug.DebugServer;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.periodic.SXRPeriodicEngine;
import com.samsungxr.nodes.SXRTextViewNode;
import com.samsungxr.script.IScriptManager;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.Threads;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Like the Android {@link Context} class, {@code SXRContext} provides core
 * services, and global information about an application environment.
 * {@code SXRContext} also holds the {@linkplain SXRScene main scene} and miscellaneous information
 * like {@linkplain #getFrameTime() the frame time.}
 * <ul>
 * <li>
 * The application <b>activity</b> resides in the context {@link #getActivity()}.
 * </li>
 * <li>
 * The current scene in the context contains all the displayable 3D objects {@link #getMainScene()}.
 * </li>
 * <li>
 * The <b>event receiver</b> in the context listens for events from scripting,
 * picking, the asset loader and dispatches to user callbacks {@link #getEventReceiver()}.
 * </li>
 * <li>
 * The context allows you to run code on either the Java or rendering thread
 * {@link #runOnGlThread(Runnable), {@link #runOnTheFrameworkThread(Runnable)}.
 * </li>
 * <li>
 * The <b>asset loader</b> in the context can load textures and models from a variety of sources
 * both synchronously and asynchronously {@link #getAssetLoader()}.
 * </li>
 * <li>
 * You can capture the 3D screen using context screen capture functions {@link #captureScreenCenter(SXRScreenshotCallback)}.
 * </li>
 * <li>
 *  The <b>shader manager</b> in the context lets you create custom shaders.
 * </ul>
 * @see SXRAssetLoader
 * @see SXREventReceiver
 * @see SXRScene
 */
public abstract class SXRContext implements IEventReceiver {
    private static final String TAG = Log.tag(SXRContext.class);

    private final SXRApplication mApplication;

    private SXREventReceiver mEventReceiver;
    /*
     * Fields and constants
     */

    // Debug and log level settings

    /**
     * Set to true for displaying statistics line.
     */
    public static boolean DEBUG_STATS = false;

    /**
     * Period of statistic log in milliseconds.
     */
    public static long DEBUG_STATS_PERIOD_MS = 1000;

    // Priorities constants, for asynchronous loading

    /**
     * SXRF can't use every {@code int} as a priority - it needs some sentinel
     * values. It will probably never need anywhere near this many, but raising
     * the number of reserved values narrows the 'dynamic range' available to
     * apps mapping some internal score to the {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY} range, and might change app behavior in subtle
     * ways that seem best avoided.
     * 
     * @since 1.6.1
     */
    public static final int RESERVED_PRIORITIES = 1024;

    /**
     * SXRF can't use every {@code int} as a priority - it needs some sentinel
     * values. A simple approach to generating priorities is to score resources
     * from 0 to 1, and then map that to the range {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY}.
     * 
     * @since 1.6.1
     */
    public static final int LOWEST_PRIORITY = Integer.MIN_VALUE
            + RESERVED_PRIORITIES;

    /**
     * SXRF can't use every {@code int} as a priority - it needs some sentinel
     * values. A simple approach to generating priorities is to score resources
     * from 0 to 1, and then map that to the range {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY}.
     * 
     * @since 1.6.1
     */
    public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;

    /**
     * The priority used by
     * {@link SXRAssetLoader#loadTexture(SXRAndroidResource)}
     * and
     * {@link #loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource)}
     *
     * @since 1.6.1
     * @deprecated use SXRAssetLoader.DEFAULT_PRIORITY instead
     */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * The ID of the GLthread. We use this ID to prevent non-GL thread from
     * calling GL functions.
     * 
     * @since 1.6.5
     */
    protected long mGLThreadID;

    /**
     * The default texture parameter instance for overloading texture methods
     * @deprecated use SXRAssetLoader.getDefaultTextureParameters instead
     */
    public final SXRTextureParameters DEFAULT_TEXTURE_PARAMETERS = new SXRTextureParameters(
            this);

    // true or false based on the support for anisotropy
    public boolean isAnisotropicSupported;

    // Max anisotropic value if supported and -1 otherwise
    public int maxAnisotropicValue = -1;

    // Debug server
    protected DebugServer mDebugServer;

    protected SXRAssetLoader mImporter = new SXRAssetLoader(this);
    /*
     * Methods
     */

    SXRContext(SXRApplication context) {
        mApplication = context;
        mEventReceiver = new SXREventReceiver(this);

        mHandlerThread = new HandlerThread("gvrf-main");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    /**
     * Get the Android {@link Context}, which provides access to system services
     * and to your application's resources. Since version 2.0.1, this is
     * actually your {@link SXRActivity} implementation, but you should probably
     * use the new {@link #getActivity()} method, rather than casting this
     * method to an {@code (Activity)} or {@code (SXRActivity)}.
     * 
     * @return An Android {@code Context}
     */
    public Context getContext() {
        return mApplication.getActivity();
    }

    /**
     * Get the Android {@link Activity} which launched your SXRF app.
     * 
     * An {@code Activity} is-a {@link Context} and so provides access to system
     * services and to your application's resources; the {@code Activity} class
     * also provides additional services, including
     * {@link Activity#runOnUiThread(Runnable)}.
     * 
     * @return The {@link Activity} which launched your SXRF app.
     */
    public Activity getActivity() {
        return mApplication.getActivity();
    }

    public SXRMain getMain() {
        return mApplication.getMain();
    }

    /**
     * Get the {@link SXRAssetLoader} to use for loading assets.
     * 
     * A {@code SXRAssetLoader} loads models asynchronously from your application's
     * local storage or the network.
     * 
     * 
     * @return The asset loader associated with this context.
     */
    public SXRAssetLoader getAssetLoader() {
        return mImporter;
    }
    
    /**
     * Get the event receiver for this context.
     * 
     * The context event receiver processes events raised on the context.
     * These include asset loading events (IAssetEvents)
     * 
     * @see IAssetEvents
     * @see IEventReceiver
     */
    public SXREventReceiver getEventReceiver() {
        return mEventReceiver;
    }

    /**
     * Creates a quad consisting of two triangles, with the specified width and
     * height.
     * 
     * @param width
     *            the quad's width
     * @param height
     *            the quad's height
     * @return A 2D, rectangular mesh with four vertices and two triangles
     */
    @SuppressWarnings("deprecation")
    public SXRMesh createQuad(float width, float height) {
        SXRMesh mesh = new SXRMesh(this);

        float[] vertices = { width * -0.5f, height * 0.5f, 0.0f, width * -0.5f,
                height * -0.5f, 0.0f, width * 0.5f, height * 0.5f, 0.0f,
                width * 0.5f, height * -0.5f, 0.0f };
        mesh.setVertices(vertices);

        final float[] normals = { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f };
        mesh.setNormals(normals);

        final float[] texCoords = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                1.0f };
        mesh.setTexCoords(texCoords);

        char[] triangles = { 0, 1, 2, 1, 3, 2 };
        mesh.setIndices(triangles);

        return mesh;
    }

    /**
     * Throws an exception if the current thread is not a GL thread.
     * 
     * @since 1.6.5
     * 
     */
    public void assertGLThread() {
        if (Thread.currentThread().getId() != mGLThreadID) {
            RuntimeException e = new RuntimeException(
                    "Should not run GL functions from a non-GL thread!");
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * To see if current thread is GL thread.
     * 
     * @return {@code true} if current thread is GL thread, {@code false} if
     * current thread is not GL thread
     */

    public boolean isCurrentThreadGLThread() {
        return Thread.currentThread().getId() == mGLThreadID;
    }

    /**
     * Get the current {@link SXRScene}, which contains the scene graph (a
     * hierarchy of {@linkplain SXRNode scene objects}) and the
     * {@linkplain SXRCameraRig camera rig}
     * 
     * @return A {@link SXRScene} instance, containing scene and camera
     *         information
     */
    public abstract SXRScene getMainScene();

    /** Set the current {@link SXRScene} */
    public abstract void setMainScene(SXRScene scene);

    /**
     * Start a debug server on the default TCP/IP port for the default number
     * of clients.
     */
    public DebugServer startDebugServer() {
        return startDebugServer(DebugServer.DEFAULT_DEBUG_PORT, DebugServer.NUM_CLIENTS);
    }

    /**
     * Start a debug server on a specified TCP/IP port, allowing a specified number
     * of concurrent clients.
     *
     * @param port
     *     The port number for the TCP/IP server.
     * @param maxClients
     *     The maximum number of concurrent clients.
     */
    public synchronized DebugServer startDebugServer(int port, int maxClients) {
        if (mDebugServer != null) {
            Log.e(TAG, "Debug server has already been started.");
            return mDebugServer;
        }

        mDebugServer = new DebugServer(this, port, maxClients);
        Threads.spawn(mDebugServer);
        return mDebugServer;
    }

    /**
     * Logs an error by sending an error event to all listeners.
     * 
     * Error events can be generated by any part of GearVRF,
     * from any thread. They are always sent to the event receiver
     * of the SXRContext.
     * 
     * @param message error message
     * @param sender object which had the error
     * @see IErrorEvents
     */
    public void logError(String message, Object sender) {
        getEventManager().sendEvent(this, IErrorEvents.class, "onError", new Object[] { message, sender });
    }
    
    /**
     * Stops the current debug server. Active connections are
     * not affected.
     */
    public synchronized void stopDebugServer() {
        if (mDebugServer == null) {
            Log.e(TAG, "Debug server is not running.");
            return;
        }

        mDebugServer.shutdown();
        mDebugServer = null;
    }

    /**
     * Returns the {@link SXRInputManager}.
     * 
     * @return A {@link SXRInputManager} to help the SXRf application interface
     *         with the input subsystem.
     * 
     */
    public abstract SXRInputManager getInputManager();

    /**
     * Returns the {@link SXREventManager}.
     *
     * @return A {@link SXREventManager} to help the SXRf framework and
     * applications to deliver events.
     *
     */
    public abstract SXREventManager getEventManager();

    /**
     * Returns the {@link IScriptManager}.
     *
     * @return A {@link SXRInputManager} to help the SXRf application to
     * create, load or execute scripts.
     *
     */
    public abstract IScriptManager getScriptManager();

    /**
     * The interval between this frame and the previous frame, in seconds: a
     * rough gauge of Frames Per Second.
     */
    public abstract float getFrameTime();

    /**
     * Enqueues a callback to be run in the GL thread.
     * 
     * This is how you take data generated on a background thread (or the main
     * (GUI) thread) and pass it to the coprocessor, using calls that must be
     * made from the GL thread (aka the "GL context"). The callback queue is
     * processed before any registered
     * {@linkplain #registerDrawFrameListener(SXRDrawFrameListener) frame
     * listeners}.
     * 
     * @param runnable
     *            A bit of code that must run on the GL thread
     */
    public abstract void runOnGlThread(Runnable runnable);

    /**
     * Enqueues a callback to be run in the GL thread after rendering a frame.
     *
     * This is how you take data generated on a background thread (or the main
     * (GUI) thread) and pass it to the coprocessor, using calls that must be
     * made from the GL thread (aka the "GL context"). The callback queue is
     * processed after a frame has been rendered.
     *
     * @param delayFrames
     *            Number of frames to delay the task. 0 means current frame.
     * @param runnable
     *            A bit of code that must run on the GL thread after rendering
     *            a frame.
     */
    public abstract void runOnGlThreadPostRender(int delayFrames, Runnable runnable);

    /**
     * Subscribes a {@link SXRDrawFrameListener}.
     * 
     * Each frame listener is called, once per frame, after any pending
     * {@linkplain #runOnGlThread(Runnable) GL callbacks} and before
     * {@link SXRMain#onStep()}.
     * 
     * @param frameListener
     *            A callback that will fire once per frame, until it is
     *            {@linkplain #unregisterDrawFrameListener(SXRDrawFrameListener)
     *            unregistered}
     */
    public abstract void registerDrawFrameListener(
            SXRDrawFrameListener frameListener);

    /**
     * Remove a previously-subscribed {@link SXRDrawFrameListener}.
     * 
     * @param frameListener
     *            An instance of a {@link SXRDrawFrameListener} implementation.
     *            Unsubscribing a listener which is not actually subscribed will
     *            not throw an exception.
     */
    public abstract void unregisterDrawFrameListener(
            SXRDrawFrameListener frameListener);

    /**
     * The {@linkplain SXRShaderManager object shader manager}
     * singleton.
     * 
     * Use the shader manager to define custom GL object shaders, which are used
     * to render a scene object's surface.
     * 
     * @return The {@linkplain SXRShaderManager shader manager}
     *         singleton.
     */
    public abstract SXRShaderManager getShaderManager();

    /**
     * The {@linkplain SXRAnimationEngine animation engine} singleton.
     * 
     * Use the animation engine to start and stop {@linkplain SXRAnimation
     * animations}.
     * 
     * @return The {@linkplain SXRAnimationEngine animation engine} singleton.
     */
    public SXRAnimationEngine getAnimationEngine() {
        return SXRAnimationEngine.getInstance(this);
    }

    /**
     * The {@linkplain SXRPeriodicEngine periodic engine} singleton.
     * 
     * Use the periodic engine to schedule {@linkplain Runnable runnables} to
     * run on the GL thread at a future time.
     * 
     * @return The {@linkplain SXRPeriodicEngine periodic engine} singleton.
     */
    public SXRPeriodicEngine getPeriodicEngine() {
        return SXRPeriodicEngine.getInstance(this);
    }

    /**
     * Register a method that is called every time SXRF creates a new
     * {@link SXRContext}.
     * 
     * Android apps aren't mapped 1:1 to Linux processes; the system may keep a
     * process loaded even after normal complete shutdown, and call Android
     * lifecycle methods to reinitialize it. This causes problems for (in
     * particular) lazy-created singletons that are tied to a particular
     * {@code SXRContext}. This method lets you register a handler that will be
     * called on restart, which can reset your {@code static} variables to the
     * compiled-in start state.
     * 
     * <p>
     * For example,
     * 
     * <pre>
     * 
     * static YourSingletonClass sInstance;
     * static {
     *     SXRContext.addResetOnRestartHandler(new Runnable() {
     * 
     *         &#064;Override
     *         public void run() {
     *             sInstance = null;
     *         }
     *     });
     * }
     * 
     * </pre>
     * 
     * <p>
     * SXRF will force an Android garbage collection after running any handlers,
     * which will free any remaining native objects from the previous run.
     * 
     * @param handler
     *            Callback to run on restart.
     */
    public synchronized static void addResetOnRestartHandler(Runnable handler) {
        sHandlers.add(handler);
    }

    protected synchronized static void resetOnRestart() {
        for (Runnable handler : sHandlers) {
            Log.d(TAG, "Running on-restart handler %s", handler);
            handler.run();
        }

        // We've probably just nulled-out a bunch of references, but many SXRF
        // apps do relatively little Java memory allocation, so it may actually
        // be a longish while before the recyclable references go stale.
        System.gc();

        // We do NOT want to clear sHandlers - the static initializers won't be
        // run again, even if the new run does recreate singletons.
    }

    private static final List<Runnable> sHandlers = new ArrayList<Runnable>();

    /**
     * Capture a 2D screenshot from the position in the middle of left eye and
     * right eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenCenter} until previous
     * {@code captureScreenCenter} callback has returned. Starting a new
     * {@code captureScreenCenter} before the previous
     * {@code captureScreenCenter} callback returned may cause out of memory
     * error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenCenter(SXRScreenshotCallback callback);

    /**
     * Capture a 2D screenshot from the position of left eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenLeft} until previous
     * {@code captureScreenLeft} callback has returned. Starting a new
     * {@code captureScreenLeft} before the previous {@code captureScreenLeft}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenLeft(SXRScreenshotCallback callback);

    /**
     * Capture a 2D screenshot from the position of right eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenRight} until previous
     * {@code captureScreenRight} callback has returned. Starting a new
     * {@code captureScreenRight} before the previous {@code captureScreenRight}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenRight(SXRScreenshotCallback callback);

    /**
     * Capture a 3D screenshot from the position of left eye. The 3D screenshot
     * is composed of six images from six directions (i.e. +x, -x, +y, -y, +z,
     * and -z).
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreen3D} until previous
     * {@code captureScreen3D} callback has returned. Starting a new
     * {@code captureScreen3D} before the previous {@code captureScreen3D}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     * 
     * @since 1.6.8
     */
    public abstract void captureScreen3D(SXRScreenshot3DCallback callback);

    private Object mTag;

    /**
     * Sets the tag associated with this context.
     * 
     * Tags can be used to store data within the context without
     * resorting to another data structure.
     *
     * @param tag an object to associate with this context
     * 
     * @see #getTag()
     * @since 3.0.0
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * Returns this context's tag.
     * 
     * @return the Object stored in this context as a tag,
     *         or {@code null} if not set
     * 
     * @see #setTag(Object)
     * @since 3.0.0
     */
    public Object getTag() {
        return mTag;
    }

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    /**
     * Execute on the so called framework thread. For now this is mostly for
     * internal use. To actually enable the use of this framework thread you
     * should derive from the SXRMain base class instead of SXRMain.
     */
    public void runOnTheFrameworkThread(final Runnable runnable) {
        mHandler.post(runnable);
    }

    /**
     * Show a toast-like message for 3 seconds
     *
     * @param message
     */
    public void showToast(final String message) {
        showToast(message, 3f);
    }

    /**
     * Show a toast-like message for the specified duration
     *
     * @param message
     * @param duration in seconds
     */
    public void showToast(final String message, float duration) {
        final float quadWidth = 1.2f;
        final SXRTextViewNode toastNode = new SXRTextViewNode(this, quadWidth, quadWidth / 5,
                message);

        toastNode.setTextSize(6);
        toastNode.setTextColor(Color.WHITE);
        toastNode.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        toastNode.setBackgroundColor(Color.DKGRAY);
        toastNode.setRefreshFrequency(SXRTextViewNode.IntervalFrequency.REALTIME);

        final SXRTransform t = toastNode.getTransform();
        t.setPositionZ(-1.5f);

        final SXRRenderData rd = toastNode.getRenderData();
        final float finalOpacity = 0.7f;
        rd.getMaterial().setOpacity(0);
        rd.setRenderingOrder(2 * SXRRenderData.SXRRenderingOrder.OVERLAY);
        rd.setDepthTest(false);

        final SXRCameraRig rig = getMainScene().getMainCameraRig();
        rig.addChildObject(toastNode);

        final SXRMaterialAnimation fadeOut = new SXRMaterialAnimation(rd.getMaterial(), duration / 4.0f) {
            @Override
            protected void animate(SXRHybridObject target, float ratio) {
                final SXRMaterial material = (SXRMaterial) target;
                material.setOpacity(finalOpacity - ratio * finalOpacity);
            }
        };
        fadeOut.setOnFinish(new SXROnFinish() {
            @Override
            public void finished(SXRAnimation animation) {
                rig.removeChildObject(toastNode);
            }
        });

        final SXRMaterialAnimation fadeIn = new SXRMaterialAnimation(rd.getMaterial(), 3.0f * duration / 4.0f) {
            @Override
            protected void animate(SXRHybridObject target, float ratio) {
                final SXRMaterial material = (SXRMaterial) target;
                material.setOpacity(ratio * finalOpacity);
            }
        };
        fadeIn.setOnFinish(new SXROnFinish() {
            @Override
            public void finished(SXRAnimation animation) {
                getAnimationEngine().start(fadeOut);
            }
        });

        getAnimationEngine().start(fadeIn);
    }

    /**
     * Our {@linkplain SXRReference references} are placed on this queue, once
     * they've been finalized
     */
    private ReferenceQueue<SXRHybridObject> mReferenceQueue = new ReferenceQueue<SXRHybridObject>();
    /**
     * We need hard references to {@linkplain SXRReference our references} -
     * otherwise, the references get garbage collected (usually before their
     * objects) and never get enqueued.
     */
    private Set<SXRReference> mReferenceSet = new HashSet<SXRReference>();

    protected final void finalizeUnreachableObjects() {
        SXRReference reference;
        while (null != (reference = (SXRReference)mReferenceQueue.poll())) {
            reference.close(mReferenceSet);
        }
    }

    /**
     *
     * @return
     */
    public SXRApplication getApplication() {
        return mApplication;
    }

    final static class UndertakerThread extends Thread {
        private final ReferenceQueue<SXRHybridObject> referenceQueue;
        private final Set<SXRReference> referenceSet;

        UndertakerThread(final ReferenceQueue<SXRHybridObject> referenceQueue, final Set<SXRReference> referenceSet, final String threadName) {
            super(threadName);
            this.referenceQueue = referenceQueue;
            this.referenceSet = referenceSet;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    SXRReference reference = (SXRReference)referenceQueue.remove();
                    reference.close(referenceSet);

                    synchronized (referenceSet) {
                        if (0 == referenceSet.size()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    //ignore; nobody has a handle to this thread, nobody can and is supposed to interrupt it
                }
            }
        }
    }

    void onDestroy() {
        if (null != mHandlerThread) {
            mHandlerThread.getLooper().quitSafely();
        }

        final String threadName = "Undertaker-" + Integer.toHexString(hashCode());
        new UndertakerThread(mReferenceQueue, mReferenceSet, threadName).start();

        mReferenceQueue = null;
        mReferenceSet = null;
    }

    static final class SXRReference extends PhantomReference<SXRHybridObject> {
        private long mNativePointer;
        private final List<NativeCleanupHandler> mCleanupHandlers;

        private SXRReference(SXRHybridObject object, long nativePointer, List<NativeCleanupHandler> cleanupHandlers, final ReferenceQueue<SXRHybridObject> referenceQueue) {
            super(object, referenceQueue);

            mNativePointer = nativePointer;
            mCleanupHandlers = cleanupHandlers;
        }

        private void close(final Set<SXRReference> referenceSet) {
            close(referenceSet, true);
        }

        private void close(final Set<SXRReference> referenceSet, boolean removeFromSet) {
            synchronized (referenceSet) {
                if (mNativePointer != 0) {
                    if (mCleanupHandlers != null) {
                        for (NativeCleanupHandler handler : mCleanupHandlers) {
                            handler.nativeCleanup(mNativePointer);
                        }
                    }
                    NativeHybridObject.delete(mNativePointer);
                    mNativePointer = 0;
                }

                if (removeFromSet) {
                    referenceSet.remove(this);
                }
            }
        }
    }

    final void registerHybridObject(SXRHybridObject gvrHybridObject, long nativePointer, List<NativeCleanupHandler> cleanupHandlers) {
        synchronized (mReferenceSet) {
            mReferenceSet.add(new SXRReference(gvrHybridObject, nativePointer, cleanupHandlers, mReferenceQueue));
        }
    }

    /**
     * Explicitly close()ing an object is going to be relatively rare - most
     * native memory will be freed when the owner-objects are garbage collected.
     * Doing a lookup in these rare cases means that we can avoid giving every @link
     * {@link SXRHybridObject} a hard reference to its {@link SXRReference}.
     */
    final SXRReference findReference(long nativePointer) {
        for (SXRReference reference : mReferenceSet) {
            if (reference.mNativePointer == nativePointer) {
                return reference;
            }
        }
        return null;
    }

}
