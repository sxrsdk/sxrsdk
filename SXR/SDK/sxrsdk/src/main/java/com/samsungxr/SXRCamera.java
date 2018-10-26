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

import java.util.HashSet;
import java.util.Set;

import com.samsungxr.utility.Colors;
import com.samsungxr.utility.Log;

import android.content.res.Resources;
import android.graphics.Color;

/**
 * A SXR camera 'takes a picture' of the scene graph: there are typically two of
 * them, one for each eye. You can {@linkplain #setRenderMask(int) specify which
 * eye(s)} this camera is serving; {@linkplain #setBackgroundColor(int) set a
 * background color}; and add any number of
 * {@linkplain #addPostEffect(SXRShaderData) 'post effects'} which are basically
 * (stock or custom) GL shaders, applied after the scene graph has been
 * rendered.
 */
public abstract class SXRCamera extends SXRComponent implements PrettyPrint {
    /**
     * We use a {@code Set}, not a {@code List}, because while
     * {@link #addPostEffect(SXRShaderData)} can add multiple copies of the same
     * post-effect, {@link #removePostEffect(SXRShaderData)} will remove all
     * copies. Since this collection is used only to maintain a hard reference
     * to the post-effects, there's no need to model the possible multiplicity
     * ... and removing a single item from a {@code Set} is cheaper than
     * removing all copies from a {@code List}.
     *
     * We make the collection {@code private} because descendant classes have no
     * APIs that care whether or not a particular post-effect is attached.
     */
    private SXRRenderData mPostEffects = null;

    protected SXRCamera(SXRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
    }

    protected SXRCamera(SXRContext gvrContext, long ptr, SXRNode owner) {
        super(gvrContext, ptr);
        setOwnerObject(owner);
    }


    static public long getComponentType() {
        return NativeCamera.getComponentType();
    }

    /** Get the background color as an Android {@link Color} */
    public int getBackgroundColor() {
        return Color.argb(Colors.glToByte(getBackgroundColorA()), //
                Colors.glToByte(getBackgroundColorR()), //
                Colors.glToByte(getBackgroundColorG()), //
                Colors.glToByte(getBackgroundColorB()));
    }

    /**
     * Set the background color.
     *
     * If you don't set the background color, the default is an opaque black:
     * {@link Color#BLACK}, 0xff000000.
     *
     * @param color
     *            An Android 32-bit (ARGB) {@link Color}, such as you get from
     *            {@link Resources#getColor(int)}
     */
    public void setBackgroundColor(int color) {
        setBackgroundColorR(Colors.byteToGl(Color.red(color)));
        setBackgroundColorG(Colors.byteToGl(Color.green(color)));
        setBackgroundColorB(Colors.byteToGl(Color.blue(color)));
        setBackgroundColorA(Colors.byteToGl(Color.alpha(color)));
    }

    /**
     * Sets the background color of the scene rendered by this camera.
     *
     * If you don't set the background color, the default is an opaque black.
     * Meaningful parameter values are from 0 to 1, inclusive: values
     * {@literal < 0} are clamped to 0; values {@literal > 1} are clamped to 1.
     */
    public void setBackgroundColor(float r, float g, float b, float a) {
        setBackgroundColorR(r);
        setBackgroundColorG(g);
        setBackgroundColorB(b);
        setBackgroundColorA(a);
    }

    /**
     * @return Red component of the background color. Value ranges from 0 to 1,
     *         inclusive.
     */
    public float getBackgroundColorR() {
        return NativeCamera.getBackgroundColorR(getNative());
    }

    /**
     * Set the red component of the background color.
     *
     * @param r
     *            Red component of the background color. Meaningful values are
     *            from 0 to 1, inclusive: values {@literal < 0} are clamped to
     *            0; values {@literal > 1} are clamped to 1.
     */
    public void setBackgroundColorR(float r) {
        NativeCamera.setBackgroundColorR(getNative(), r);
    }

    /**
     * @return Green component of the background color. Value ranges from 0 to
     *         1, inclusive.
     */
    public float getBackgroundColorG() {
        return NativeCamera.getBackgroundColorG(getNative());
    }

    /**
     * Set the green component of the background color.
     *
     * @param g
     *            Green component of the background color. Meaningful values are
     *            from 0 to 1, inclusive: values {@literal < 0} are clamped to
     *            0; values {@literal > 1} are clamped to 1.
     */
    public void setBackgroundColorG(float g) {
        NativeCamera.setBackgroundColorG(getNative(), g);
    }

    /**
     * @return Blue component of the background color. Value ranges from 0 to 1,
     *         inclusive.
     */
    public float getBackgroundColorB() {
        return NativeCamera.getBackgroundColorB(getNative());
    }

    /**
     * Set the blue component of the background color.
     *
     * @param b
     *            Blue component of the background color. Meaningful values are
     *            from 0 to 1, inclusive: values {@literal < 0} are clamped to
     *            0; values {@literal > 1} are clamped to 1.
     */
    public void setBackgroundColorB(float b) {
        NativeCamera.setBackgroundColorB(getNative(), b);
    }

    /**
     * @return Alpha component of the background color. Value ranges from 0 to
     *         1, inclusive.
     */
    public float getBackgroundColorA() {
        return NativeCamera.getBackgroundColorA(getNative());
    }

    /**
     * Set the alpha component of the background color.
     *
     * @param a
     *            Value of alpha component.
     */
    public void setBackgroundColorA(float a) {
        NativeCamera.setBackgroundColorA(getNative(), a);
    }

    /**
     * Get the current render mask
     *
     * @return A mask consisting of values from
     *         {@link SXRRenderData.SXRRenderMaskBit SXRRenderMaskBit}.
     */
    public int getRenderMask() {
        return NativeCamera.getRenderMask(getNative());
    }

    /**
     * Set the current render mask
     *
     * @param renderMask
     *            A mask consisting of values from
     *            {@link SXRRenderData.SXRRenderMaskBit SXRRenderMaskBit}.
     */
    public void setRenderMask(int renderMask) {
        NativeCamera.setRenderMask(getNative(), renderMask);
    }

    /**
     * Add a post-effect to this camera's render chain.
     *
     * Post-effects are GL shaders, applied to the texture (hardware bitmap)
     * containing the rendered scene graph. Each post-effect combines a shader
     * selector with a set of parameters: This lets you pass different
     * parameters to the shaders for each eye.
     *
     * @param postEffectData
     *            Post-effect to append to this camera's render chain
     */
    public void addPostEffect(SXRMaterial postEffectData) {
        SXRContext ctx = getSXRContext();

        if (mPostEffects == null)
        {
            mPostEffects = new SXRRenderData(ctx, postEffectData);
            SXRMesh dummyMesh = new SXRMesh(getSXRContext(),"float3 a_position float2 a_texcoord");
            mPostEffects.setMesh(dummyMesh);
            NativeCamera.setPostEffect(getNative(), mPostEffects.getNative());
            mPostEffects.setCullFace(SXRRenderPass.SXRCullFaceEnum.None);
        }
        else
        {
            SXRRenderPass rpass = new SXRRenderPass(ctx, postEffectData);
            rpass.setCullFace(SXRRenderPass.SXRCullFaceEnum.None);
            mPostEffects.addPass(rpass);
        }
    }

    /**
     * Remove (all instances of) a {@linkplain SXRShaderData post-effect} from
     * this camera's render chain
     *
     * @param postEffectData
     *            Post-effect to remove.
     */
    public void removePostEffect(SXRMaterial postEffectData) {
        if(mPostEffects == null)
            return;
        
        for (int i = 0; i < mPostEffects.getPassCount(); ++i)
        {
            SXRRenderPass pass = mPostEffects.getPass(i);
            if (postEffectData == pass.getMaterial())
            {
                mPostEffects.removePass(i);
            }
        }
    }

    /**
     * Replace the current {@link SXRTransform transform} for owner object of
     * the camera.
     *
     * @param transform
     *            New transform.
     */
    void attachTransform(SXRTransform transform) {
        if (getOwnerObject() != null) {
            getOwnerObject().attachTransform(transform);
        }
    }

    /**
     * Remove the object's (owner object of camera) {@link SXRTransform
     * transform}.
     *
     */
    void detachTransform() {
        if (getOwnerObject() != null) {
            getOwnerObject().detachTransform();
        }
    }

    /**
     * Get the {@link SXRTransform}.
     *
     *
     * @return The current {@link SXRTransform transform} of owner object of
     *         camera. Applying transform to owner object of camera moves it. If
     *         no transform is currently attached to the object, returns
     *         {@code null}.
     */
    public SXRTransform getTransform() {
        if (getOwnerObject() != null) {
            return getOwnerObject().getTransform();
        }
        return null;
    }

    /**
     * Add {@code child} as a child of this camera owner object.
     *
     * @param child
     *            {@link SXRNode Object} to add as a child of this camera
     *            owner object.
     */
    public void addChildObject(SXRNode child) {
        if (getOwnerObject() != null) {
            getOwnerObject().addChildObject(child);
        }
    }

    /**
     * Remove {@code child} as a child of this camera owner object.
     *
     * @param child
     *            {@link SXRNode Object} to remove as a child of this
     *            camera owner object.
     */
    public void removeChildObject(SXRNode child) {
        if (getOwnerObject() != null) {
            getOwnerObject().removeChildObject(child);
        }
    }

    /**
     * Get the number of child objects that belongs to owner object of this
     * camera.
     *
     * @return Number of {@link SXRNode objects} added as children of
     *         this camera owner object.
     */
    public int getChildrenCount() {
        if (getOwnerObject() != null) {
            return getOwnerObject().getChildrenCount();
        }
        return 0;
    }

    /**
     * Sets the view matrix of the Camera.
     * The view matrix is the inverse of the camera model matrix.
     * Normally it is computed automatically from the SXRTransform
     * attached to the SXRNode which owns the camera.
     * If the camera is NOT attached to a scene object,
     * you can use this call to set the view matrix so the
     * camera can be used with a SXRRenderTarget.
     *
     * @param matrix new view matrix.
     * @see SXRRenderTarget#setCamera(SXRCamera)
     */
    public void setViewMatrix(float[] matrix)
    {
        if (matrix.length != 16)
        {
            throw new IllegalArgumentException("Matrix size not equal to 16.");
        }
        NativeCamera.setViewMatrix(getNative(), matrix);
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        sb.append("backgroundColor: ");
        sb.append(String.format("0x%x", getBackgroundColor()));
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        sb.append("renderMask: ");
        sb.append(String.format("0x%x", getRenderMask()));
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        SXRTransform transform = getTransform();
        sb.append("transform: ");
        sb.append(transform);
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }
}

class NativeCamera {
    static native float getBackgroundColorR(long camera);

    static native long getComponentType();

    static native void setBackgroundColorR(long camera, float r);

    static native float getBackgroundColorG(long camera);

    static native void setBackgroundColorG(long camera, float g);

    static native float getBackgroundColorB(long camera);

    static native void setBackgroundColorB(long camera, float b);

    static native float getBackgroundColorA(long camera);

    static native void setBackgroundColorA(long camera, float a);

    static native int getRenderMask(long camera);

    static native void setRenderMask(long camera, int renderMask);

    static native void setPostEffect(long camera, long postEffectData);

    static native void setViewMatrix(long camera, float[] matrix);
}
