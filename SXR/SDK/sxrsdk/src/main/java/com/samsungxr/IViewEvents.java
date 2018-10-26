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

import android.view.View;

import com.samsungxr.nodes.SXRViewNode;

/**
 * This interface defines the callback interface of an Android {@code View}
 * that is inflated by a {@link SXRViewNode}.
 */
public interface IViewEvents extends IEvents {
    /**
     * Called when the {@link View} is attached to the Android view hierarchy.
     * This is the recommended place to configure the view and set its listeners.
     *
     * @param sceneObject {@link SXRViewNode} whom the view is attached to.
     * @param view Android view attached to {@link SXRViewNode}
     */
    void onInitView(SXRViewNode sceneObject, View view);

    /**
     * Called when the Android starts to draw the {@link View} attached to {@link SXRViewNode}.
     * This is the recommended place to attach the {@link SXRViewNode} to the
     * scene, avoiding rendering of scene object with empty texture.
     *
     * @param sceneObject {@link SXRViewNode} whom the view is attached to.
     * @param view Android view attached to {@link SXRViewNode}
     */
    void onStartRendering(SXRViewNode sceneObject, View view);
}
