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
import com.samsungxr.SXRMaterial;
import com.samsungxr.utility.Log;

public class SXRConeNode extends SXRCylinderNode {

    @SuppressWarnings("unused")
    private static final String TAG = Log.tag(SXRConeNode.class);

    private static final int STACK_NUMBER = 10;
    private static final int SLICE_NUMBER = 36;
    private static final float BASE_RADIUS = 0.5f;
    private static final float TOP_RADIUS = 0.0f;
    private static final float HEIGHT = 1.0f;

    /**
     * Constructs a cone scene object with a height of 1, radius of 0.5, 10
     * stacks, and 36 slices.
     * 
     * The cone's triangles and normals are facing out and the same texture will
     * be applied to bottom, and side of the cone.
     * 
     * @param gvrContext
     *            current {@link SXRContext}
     */
    public SXRConeNode(SXRContext gvrContext) {
        super(gvrContext, BASE_RADIUS, TOP_RADIUS, HEIGHT, STACK_NUMBER,
                SLICE_NUMBER, true);
    }

    /**
     * Constructs a cone scene object with a height of 1, radius of 0.5, 10
     * stacks, and 36 slices.
     * 
     * The cone's triangles and normals are facing either in or out and the same
     * texture will be applied to bottom, and side of the cone.
     * 
     * @param gvrContext
     *            current {@link SXRContext}
     * 
     * @param facingOut
     *            whether the triangles and normals should be facing in or
     *            facing out.
     */
    public SXRConeNode(SXRContext gvrContext, boolean facingOut) {
        super(gvrContext, BASE_RADIUS, TOP_RADIUS, HEIGHT, STACK_NUMBER,
                SLICE_NUMBER, facingOut);
    }

    /**
     * Constructs a cone scene object with a height of 1, radius of 0.5, 10
     * stacks, and 36 slices.
     * 
     * The cone's triangles and normals are facing either in or out and the same
     * material will be applied to bottom, and side of the cone.
     * 
     * @param gvrContext
     *            current {@link SXRContext}
     * 
     * @param facingOut
     *            whether the triangles and normals should be facing in or
     *            facing out.
     * 
     * @param material
     *            the material for the cone.
     */
    public SXRConeNode(SXRContext gvrContext, boolean facingOut,
            SXRMaterial material) {
        super(gvrContext, BASE_RADIUS, TOP_RADIUS, HEIGHT, STACK_NUMBER,
                SLICE_NUMBER, facingOut, material);
    }
}
