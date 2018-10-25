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


import java.util.concurrent.ExecutionException;

/**
 * A RenderPass let one render the same scene object multiple times with different settings. This is useful to
 * achieve effects like outline in cartoon-ish rendering or computing addictive lights for instance.
 *
 * The benefit of using a render pass over duplicating the object and rendering twice is that like culling, transform and
 * skinning are performed only once.
 *
 * A render pass encapsulates a material and all rendering states that can be set per pass. 
 *
 *
 */
public class SXRRenderPass extends SXRHybridObject implements IRenderable {

    private SXRMaterial     mMaterial;
    private SXRMesh         mMesh;
    private SXRCullFaceEnum mCullFace;

    public enum SXRCullFaceEnum {
        /**
         * Tell Graphics API to discard back faces. This value is assumed by
         * default.
         */
        Back(0),

        /**
         * Tell Graphics API to discard front faces.
         */
        Front(1),

        /**
         * Tell Graphics API render both front and back faces.
         */
        None(2);

        private final int mValue;

        private SXRCullFaceEnum(int value) {
            mValue = value;
        }

        public static SXRCullFaceEnum fromInt(int value) {
            switch (value) {
                case 1:
                    return SXRCullFaceEnum.Front;

                case 2:
                    return SXRCullFaceEnum.None;

                default:
                    return SXRCullFaceEnum.Back;
            }
        }
        public int getValue() {
            return mValue;
        }
    }

    /**
     * Constructor.
     *
     * @param gvrContext
     *            Current {@link SXRContext}
     */
    public SXRRenderPass(SXRContext gvrContext) {
        super(gvrContext, NativeRenderPass.ctor());
        setMaterial(new SXRMaterial(gvrContext));
        mCullFace = SXRCullFaceEnum.Back;
        mMesh = null;
    }

    public SXRRenderPass(SXRContext gvrContext, SXRMaterial material) {
        super(gvrContext, NativeRenderPass.ctor());
        setMaterial(material);
        mCullFace = SXRCullFaceEnum.Back;
        mMesh = null;
    }

    /**
     * Set the {@link SXRShaderData material} for this pass.
     *
     * @param material
     *            The {@link SXRMaterial material} this {@link SXRRenderPass pass}
     *            will be rendered with.
     */
    public void setMaterial(SXRMaterial material)
    {
        mMaterial = material;
        NativeRenderPass.setMaterial(getNative(), material.getNative());
    }

    /**
     * @return The {@link SXRMesh mesh} being rendered.
     * This will be the mesh associated with the SXRRenderData
     * this render pass is associated with.
     */
    public SXRMesh getMesh() { return mMesh; }

    /**
     * Sets the mesh to render. Only SXRRenderData should
     * call this function - it is internal.
     *
     * @param mesh
     */
    void setMesh(SXRMesh mesh)
    {
        mMesh = mesh;
    }


    public boolean isLightEnabled() { return false; }

    /**
     * Set the native shader for this pass.
     * Native shaders are identified by unique integer IDs.
     *
     * @param shader
     *            The native shader this {@link SXRRenderPass pass}
     *            will be rendered with.
     */
    public void setShader(int shader, boolean useMultiview)
    {
        NativeRenderPass.setShader(getNative(), shader, useMultiview);
    }

    /**
     * @return The {@link SXRShaderData material} this {@link SXRRenderPass pass} will
     *         being rendered with.
     */
    public SXRMaterial getMaterial() {
        return mMaterial;
    }

    /**
     * Get the integer ID for the native shader used by this pass.
     */
    int getShader(boolean useMultiview)
    {
        return NativeRenderPass.getShader(getNative(), useMultiview);
    }


    /**
     * Set the {@link SXRCullFaceEnum face} to be culled when rendering this {@link SXRRenderPass pass}
     *
     * @param cullFace
     *            {@code SXRCullFaceEnum.Back} Tells Graphics API to discard
     *            back faces, {@code SXRCullFaceEnum.Front} Tells Graphics API
     *            to discard front faces, {@code SXRCullFaceEnum.None} Tells
     *            Graphics API to not discard any face
     */
    public void setCullFace(SXRCullFaceEnum cullFace) {
        mCullFace = cullFace;
        NativeRenderPass.setCullFace(getNative(), cullFace.getValue());
    }

    /**
     * @return The current {@link SXRCullFaceEnum face} to be culled.
     */
    public SXRCullFaceEnum getCullFace() {
        return mCullFace;
    }
}

class NativeRenderPass {

    static native long ctor();

    static native int getShader(long renderPass, boolean useMultiview);

    static native void setMaterial(long renderPass, long material);

    static native void setShader(long renderPass, int shader, boolean useMultiview);

    static native void setCullFace(long renderPass, int cullFace);
}
