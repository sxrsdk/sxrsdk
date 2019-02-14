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
/*
 * Modifications copyright (c) 2015, Samsung Research America
 */


package com.samsungxr;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.Choreographer;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * This SXRSurfaceView class extends from {@link GLSurfaceView} which is used
 * for OpenGL rendering. In SXR, SXRSurfaceView acts the same way
 * {@link GLSurfaceView} does to render object and scene. SXRSurfaceView
 * requires a valid {@link MonoscopicViewManager} which holds the main scene details to
 * be rendered
 */
class MonoscopicSurfaceView extends GLSurfaceView implements
        android.view.Choreographer.FrameCallback {
    /**
     * Constructs a SXRSurfaceView given by current SXR context without
     * MonoscopicViewManager
     *
     * @param context
     *            current context
     */
    public MonoscopicSurfaceView(Context context) {
        super(context);
    }

    /**
     * Constructs a {@link MonoscopicSurfaceView} given by current {@link SXRContext}
     * with {@link MonoscopicViewManager}
     *
     * @param context
     *            current context
     * @param viewManager
     *            a given {@link MonoscopicViewManager} object to be used in
     *            {@link MonoscopicSurfaceView}
     */
    public MonoscopicSurfaceView(Context context,
                                 MonoscopicViewManager viewManager,
                                 int width, int height) {
        super(context);
        /*
         * To access inputs by onKeyDown().
         */
        setFocusable(true);
        setKeepScreenOn(true);
        /*
         * Avoids reloading the application every time it pauses.
         */
        setEGLContextClientVersion(3);
        setPreserveEGLContextOnPause(true);
        setEGLContextFactory(new MonoscopicContextFactory());
        setEGLConfigChooser(new MonoscopicConfigChooser(8, 8, 8, 8, 24, 8));
        setRenderer(new MonoscopicSurfaceViewRenderer(viewManager));
        /*
         * requestRender() will be called efficiently with VSync.
         */
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        getHolder().setFixedSize(width, height);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        requestRender();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        holder.setFormat(PixelFormat.TRANSLUCENT);
        int token = holder.getSurface().hashCode();
        SXRRenderer.initialize(token);
        Choreographer.getInstance().removeFrameCallback(this);
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Choreographer.getInstance().removeFrameCallback(this);
        super.surfaceDestroyed(holder);
        int token = holder.getSurface().hashCode();
        SXRRenderer.reset(token);
    }
}

/*
 * Copyright (C) 2009 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

class MonoscopicContextFactory implements GLSurfaceView.EGLContextFactory {

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    @Override
    public EGLContext createContext(EGL10 egl, EGLDisplay display,
                                    EGLConfig eglConfig) {
        int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE };
        EGLContext context = egl.eglCreateContext(display, eglConfig,
                EGL10.EGL_NO_CONTEXT, attrib_list);
        return context;
    }

    @Override
    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        egl.eglDestroyContext(display, context);
    }
}

class MonoscopicConfigChooser implements GLSurfaceView.EGLConfigChooser {

    /**
     * Constructs a MonoscopicConfigChooser class with initial set-ups for EGL
     *
     * @param r
     *            size of red in bits
     * @param g
     *            size of green in bits
     * @param b
     *            size of blue in bits
     * @param a
     *            size of alpha in bits
     * @param depth
     *            size of depth buffer in bits
     * @param stencil
     *            size of stencil buffer in bits
     */
    public MonoscopicConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
        mRedSize = r;
        mGreenSize = g;
        mBlueSize = b;
        mAlphaSize = a;
        mDepthSize = depth;
        mStencilSize = stencil;
    }

    /*
     * This EGL config specification is used to specify 2.0 rendering. We use a
     * minimum size of 4 bits for red/green/blue, but will perform actual
     * matching in chooseConfig() below.
     */
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private final int[] s_configAttribs2 = { EGL10.EGL_RED_SIZE, 4,
            EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

        /*
         * Get the number of minimally matching EGL configurations
         */
        int[] num_config = new int[1];
        egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);

        int numConfigs = num_config[0];

        if (numConfigs <= 0) {
            throw new IllegalArgumentException("No configs match configSpec");
        }

        /*
         * Allocate then read the array of minimally matching EGL configs
         */
        EGLConfig[] configs = new EGLConfig[numConfigs];
        egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs,
                num_config);

        /*
         * Now return the "best" one
         */
        return chooseConfig(egl, display, configs);
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                  EGLConfig[] configs) {
        for (EGLConfig config : configs) {
            int d = findConfigAttrib(egl, display, config,
                    EGL10.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(egl, display, config,
                    EGL10.EGL_STENCIL_SIZE, 0);

            // We need at least mDepthSize and mStencilSize bits
            if (d < mDepthSize || s < mStencilSize)
                continue;

            // We want an *exact* match for red/green/blue/alpha
            int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE,
                    0);
            int g = findConfigAttrib(egl, display, config,
                    EGL10.EGL_GREEN_SIZE, 0);
            int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE,
                    0);
            int a = findConfigAttrib(egl, display, config,
                    EGL10.EGL_ALPHA_SIZE, 0);

            if (r == mRedSize && g == mGreenSize && b == mBlueSize
                    && a == mAlphaSize)
                return config;
        }
        return null;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                 EGLConfig config, int attribute, int defaultValue) {

        if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
            return mValue[0];
        }
        return defaultValue;
    }

    // Subclasses can adjust these values:
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;
    private int[] mValue = new int[1];
}
