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

package com.samsungxr.widgetplugin;

/**
 * SXRWidgetSceneObjectMeshInfo provides way to create SXRWidgetSceneObject
 * meshes quickly.
 */
public class SXRWidgetSceneObjectMeshInfo {

    /**
     * Following four variables correspond to top left and bottom right X and Y
     * world coordinates of SXRWidgetSceneObject, creating a rectangle
     */
    public float mTopLeftX;
    public float mTopLeftY;
    public float mBottomRightX;
    public float mBottomRightY;

    /**
     * Following two variables correspond to top left and bottom right X and Y
     * screen coordinates of parent libGDX GLSurfaceView which the app created
     * from which our SXRWidgetSceneObjects are created, kindly refer @SXRWidgetSceneObject
     * for more info on how these scene objects are created from the view
     */

    public int[] mTopLeftViewCoords;

    public int[] mBottomRightViewCoords;

    /**
     * Z is constant for easy mesh creation, app can transform widget after
     * creation to play with it.
     */

    public float mZ = 0.0f;

    public SXRWidgetSceneObjectMeshInfo(float topLeftX, float topLeftY,
            float bottomRightX, float bottomRightY, int[] topLeftViewCoords,
            int[] bottomRightViewCoords) {
        mTopLeftX = topLeftX;
        mTopLeftY = topLeftY;
        mBottomRightX = bottomRightX;
        mBottomRightY = bottomRightY;
        mTopLeftViewCoords = topLeftViewCoords;
        mBottomRightViewCoords = bottomRightViewCoords;

    }
}