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

/**
 * A render target is a component which allows the scene to be rendered
 * into a texture from the viewpoint of a particular scene object.
 * SXRRenderTarget can initiate a DrawFrameListener which causes
 * the scene to be rendered into the texture every frame.
 * To initiate rendering you must call @{link #startListening()}.
 * A render target may also have a custom camera to allow control
 * over the projection matrix.
 * @see SXRRenderTexture
 * @see SXRShadowMap
 */
public class SXRRenderTarget extends SXRBehavior
{
    protected SXRRenderTexture mTexture;
    protected SXRScene  mScene;
    protected SXRCamera mCamera;
    protected boolean   mIsStereo;

    static public long getComponentType()
    {
        return NativeRenderTarget.getComponentType();
    }

    /**
     * Constructs a render target component which renders the given scene to a texture.
     * The objects in the scene are rendered from the viewpoint of the scene object
     * the SXRRenderTarget is attached to. Nothing is rendered if
     * the render target is not attached to a scene object.
     *
     * @param texture   {@link SXRRenderTexture} to render to.
     * @param scene     {@link SXRScene} to render.
     */
    public SXRRenderTarget(SXRRenderTexture texture, SXRScene scene)
    {
        super(scene.getSXRContext(),
              NativeRenderTarget.ctorMultiview(texture.getNative(), false, false));
        mScene = scene;
        mTexture = texture;
        mIsStereo = false;
        setCamera(scene.getMainCameraRig().getCenterCamera());
    }

    /**
     * Constructs a render target component which renders the main scene to a designated texture.
     * The objects in the scene are rendered from the viewpoint of the scene object
     * the SXRRenderTarget is attached to. Nothing is rendered if
     * the render target is not attached to a scene object.
     *
     * @param texture   SXRRenderTexture to render to.
     */
    public SXRRenderTarget(SXRRenderTexture texture, boolean isStereo)
    {
        super(texture.getSXRContext(),
              NativeRenderTarget.ctorMultiview(texture.getNative(), false, isStereo));
        mScene = texture.getSXRContext().getMainScene();
        mTexture = texture;
        mIsStereo = isStereo;
    }

    /**
     * Constructs a render target component which renders the given scene in stereo.
     * @param scene     {@link SXRScene} to render.
     * @param width     default width of viewport
     * @param height    default height of viewport
     */
    public SXRRenderTarget(SXRScene scene, int width, int height)
    {
        super(scene.getSXRContext(),
              NativeRenderTarget.ctorViewport(scene.getNative(), width, height));
        mScene = scene;
        mIsStereo = true;
    }

    /**
     * Constructs a render target component which renders to the same scene
     * as the specified render target. The objects in the scene are rendered
     * from the viewpoint of the scene's camera.
     *
     * @param texture       SXRRenderTexture to render to.
     * @param renderTarget  SXRRenderTarget to share
     */
    public SXRRenderTarget(SXRRenderTexture texture, SXRRenderTarget renderTarget)
    {
        super(texture.getSXRContext(),
              NativeRenderTarget.ctor(texture.getNative(), renderTarget.getNative()));
        setEnable(false);
        mTexture = texture;
        mIsStereo = renderTarget.isStereo();
    }

    public SXRRenderTarget(SXRRenderTexture texture, SXRScene scene, boolean isMultiview)
    {
        super(texture.getSXRContext(),
              NativeRenderTarget.ctorMultiview(texture.getNative(), isMultiview, true));
        setEnable(false);
        mTexture = texture;
        mScene = scene;
        mIsStereo = true;
    }

    public boolean isStereo() { return mIsStereo; }

    public void setStereo(boolean flag)
    {
        mIsStereo = flag;
        NativeRenderTarget.setStereo(getNative(), flag);
    }

    public SXRCamera getCamera(){
        return mCamera;
    }

    public void setCamera(SXRCamera camera)
    {
        mCamera = camera;
        NativeRenderTarget.setCamera(getNative(), camera.getNative());
    }

    public void attachRenderTarget(SXRRenderTarget renderTarget){
        NativeRenderTarget.attachRenderTarget(getNative(),renderTarget.getNative());
    }
    public void beginRendering(SXRCamera camera){
        NativeRenderTarget.beginRendering(getNative(), camera.getNative());
    }
    public void endRendering(){
        NativeRenderTarget.endRendering(getNative());
    }
    public void cullFromCamera(SXRScene scene, SXRCamera camera, SXRShaderManager shaderManager)
    {
        NativeRenderTarget.cullFromCamera(getNative(), scene.getNative(), scene, camera.getNative(), shaderManager.getNative());
    }

    public void render(SXRScene scene, SXRCamera camera, SXRShaderManager shaderManager, SXRRenderTexture posteffectRenderTextureA, SXRRenderTexture posteffectRenderTextureB) {
        NativeRenderTarget.render(getNative(), camera.getNative(), shaderManager.getNative(), posteffectRenderTextureA.getNative(), posteffectRenderTextureB.getNative(), scene.getNative(), scene);
    }

    /**
     * Internal constructor for subclasses.
     * @param ctx
     * @param nativePointer
     */
    protected SXRRenderTarget(SXRContext ctx, long nativePointer)
    {
        super(ctx, nativePointer);
    }

    /**
     * Sets the texture this render target will render to.
     * If no texture is provided, the render target will
     * not render anything.
     * @param texture SXRRenderTexture to render to.
     */
    public void setTexture(SXRRenderTexture texture)
    {
        mTexture = texture;
        NativeRenderTarget.setTexture(getNative(), texture.getNative());
    }

    /**
     * Gets the SXRRenderTexture being rendered to by this render target.
     * @return SXRRenderTexture used by the render target or null if none specified.
     * @see #setTexture(SXRRenderTexture)
     */
    public SXRRenderTexture getTexture()
    {
        return mTexture;
    }

    public void onDrawFrame(float frameTime)
    {
        getSXRContext().getApplication().getViewManager().cullAndRender(this, mScene);
    }
}

class NativeRenderTarget
{
    static native long ctor(long texture, long sourceRendertarget);
    static native long ctorViewport(long scene, int defaultViewportW, int defaultViewportH);
    static native long ctorMultiview(long texture, boolean isMultiview, boolean isStereo);
    static native long getComponentType();
    static native void beginRendering(long rendertarget, long camera);
    static native void endRendering(long rendertarget);
    static native void setCamera(long rendertarget, long camera);
    static native void setStereo(long rendertarget, boolean isStereo);
    static native void cullFromCamera(long renderTarget, long scene, SXRScene javaScene, long camera, long shader_manager);
    static native void render(long renderTarget, long camera, long shader_manager, long posteffectrenderTextureA, long posteffectRenderTextureB, long scene, SXRScene javaSceneObject);
    static native void setTexture(long rendertarget, long texture);
    static native void attachRenderTarget(long renderTarget, long nextRenderTarget);
}
