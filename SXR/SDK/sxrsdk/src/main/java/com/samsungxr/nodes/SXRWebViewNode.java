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

package com.samsungxr.nodes;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.SXRMaterial.SXRShaderType;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.webkit.WebView;

/**
 * {@linkplain SXRNode Scene object} that shows a web page, using the
 * Android {@link WebView}.
 */

@Deprecated
public class SXRWebViewNode extends SXRNode implements
        SXRDrawFrameListener {
    private static final int REFRESH_INTERVAL = 30; // frames

    private final Surface mSurface;
    private final SurfaceTexture mSurfaceTexture;
    private final WebView mWebView;
    private int mCount = 0;

    /**
     * Shows a web page on a {@linkplain SXRNode scene object} with an
     * arbitrarily complex geometry.
     * 
     * @param gvrContext
     *            current {@link SXRContext}
     * @param mesh
     *            a {@link SXRMesh} - see
     *            {@link SXRContext#loadMesh(com.samsungxr.SXRAndroidResource)}
     *            and {@link SXRContext#createQuad(float, float)}
     * @param webView
     *            an Android {@link WebView}
     */
    public SXRWebViewNode(SXRContext gvrContext, SXRMesh mesh,
            WebView webView) {
        super(gvrContext, mesh);
        mWebView = webView;
        gvrContext.registerDrawFrameListener(this);
        SXRTexture texture = new SXRExternalTexture(gvrContext);
        SXRMaterial material = new SXRMaterial(gvrContext, SXRShaderType.OES.ID);
        material.setMainTexture(texture);
        getRenderData().setMaterial(material);

        mSurfaceTexture = new SurfaceTexture(texture.getId());
        mSurface = new Surface(mSurfaceTexture);
        mSurfaceTexture.setDefaultBufferSize(mWebView.getWidth(),
                mWebView.getHeight());
    }

    /**
     * Shows a web page in a 2D, rectangular {@linkplain SXRNode scene
     * object.}
     * 
     * @param gvrContext
     *            current {@link SXRContext}
     * @param width
     *            the rectangle's width
     * @param height
     *            the rectangle's height
     * @param webView
     *            a {@link WebView}
     */
    public SXRWebViewNode(SXRContext gvrContext, float width,
            float height, WebView webView) {
        this(gvrContext, gvrContext.createQuad(width, height), webView);
    }

    @Override
    public void onDrawFrame(float frameTime) {
        if (++mCount > REFRESH_INTERVAL) {
            refresh();
            mCount = 0;
        }
    }

    /** Draws the {@link WebView} onto {@link #mSurfaceTexture} */
    private void refresh() {
        try {
            Canvas canvas = mSurface.lockCanvas(null);
            mWebView.draw(canvas);
            mSurface.unlockCanvasAndPost(canvas);
        } catch (Surface.OutOfResourcesException t) {
            Log.e("SXRWebBoardObject", "lockCanvas failed");
        }
        mSurfaceTexture.updateTexImage();
    }
}
