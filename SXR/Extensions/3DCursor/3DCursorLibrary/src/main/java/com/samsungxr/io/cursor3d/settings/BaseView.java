/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.io.cursor3d.settings;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import com.samsungxr.SXRActivity;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.IViewEvents;
import com.samsungxr.nodes.SXRViewNode;

abstract class BaseView {
    private static final String TAG = BaseView.class.getSimpleName();
    private static final float DEFAULT_SCALE = 10.0f;
    public static final float QUAD_DEPTH = -13f;

    SXRScene scene;
    SXRContext context;
    Activity activity;
    private SXRViewNode layoutNode;

    BaseView(SXRContext context, SXRScene scene, int layoutID)
    {
        this(context, scene, layoutID, DEFAULT_SCALE);
    }

    BaseView(final SXRContext context,
             final SXRScene scene,
             final int layoutID,
             final float scale)
    {
        this.context = context;
        this.scene = scene;
        this.activity = context.getActivity();

        layoutNode = new SXRViewNode(context, layoutID, new IViewEvents() {
            @Override
            public void onInitView(SXRViewNode gvrViewNode, View view) {
                BaseView.this.onInitView(view);
            }

            @Override
            public void onStartRendering(SXRViewNode gvrViewNode, View view) {
                gvrViewNode.getTransform().setScale(scale, scale, 1.0f);
                BaseView.this.onStartRendering();
            }
        });

        layoutNode.setTextureBufferSize(1024);
    }

    abstract protected void onInitView(View view);
    abstract protected void onStartRendering();

    void render(final float x, final float y, final float z) {
        layoutNode.getTransform().setPosition(x, y, z);
        show();
    }

    void show() {
        scene.addNode(layoutNode);
    }

    void hide() {
        scene.removeNode(layoutNode);
    }

    void disable() {
        scene.removeNode(layoutNode);
    }

    void enable() {
        scene.addNode(layoutNode);
    }

    protected void setGestureDetector(GestureDetector detector) {
        layoutNode.setGestureDetector(detector);
    }
}
