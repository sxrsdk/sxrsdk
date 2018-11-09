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

    protected SXRMaterial mMaterial;
    protected SXRRenderTexture mTexture;
    protected SXRScene  mScene;
    protected SXRCamera mCamera;
    static public long getComponentType()
    {
        return NativeRenderTarget.getComponentType();
    }
    /**
     * Constructs a render target component which renders the given scene to a designated texture.
     * The objects in the scene are rendered from the viewpoint of the scene object
     * the SXRRenderTarget is attached to. Nothing is rendered if
     * the render target is not attached to a scene object.
     * You must call @{link #setEnable(true)} to initiate rendering.
     *
     * @param texture   SXRRenderTexture to render to.
     * @param scene     SXRScene to render.
     * @see #setEnable(boolean)
     */
    public SXRRenderTarget(SXRRenderTexture texture, SXRScene scene)
    {
        this(texture,scene,false);
    }
    public SXRRenderTarget(SXRContext gvrContext)
    {
        super(gvrContext,NativeRenderTarget.defaultCtor(gvrContext.getMainScene().getNative()));
        mScene = gvrContext.getMainScene();
    }

    public SXRRenderTarget(SXRContext gvrContext, int defaultViewportW, int defaultViewportH)
    {
        super(gvrContext,NativeRenderTarget.ctorViewport(gvrContext.getMainScene().getNative(),
                defaultViewportW, defaultViewportH));
        mScene = gvrContext.getMainScene();
    }

    public SXRCamera getCamera(){
        return mCamera;
    }
    public void setCamera(SXRCamera camera){
        mCamera = camera;
        NativeRenderTarget.setCamera(getNative(), camera.getNative());
    }
    public SXRRenderTarget(SXRRenderTexture texture, SXRScene scene, SXRRenderTarget renderTarget)
    {
        super(texture.getSXRContext(), NativeRenderTarget.ctor(texture.getNative(), renderTarget.getNative()));
        setEnable(false);
        mTexture = texture;
        mScene = scene;
        setMainScene(scene);
    }
    public SXRRenderTarget(SXRRenderTexture texture, SXRScene scene, boolean isMultiview)
    {
        super(texture.getSXRContext(), NativeRenderTarget.ctorMultiview(texture.getNative(),isMultiview));
        setEnable(false);
        mTexture = texture;
        mScene = scene;
        setMainScene(scene);
        setCamera(scene.getMainCameraRig().getCenterCamera());

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
    public void cullFromCamera(SXRScene scene, SXRCamera camera, SXRShaderManager shaderManager){
        NativeRenderTarget.cullFromCamera(scene.getNative(), scene, getNative(),camera.getNative(), shaderManager.getNative());
    }

    public void render(SXRScene scene, SXRCamera camera, SXRShaderManager shaderManager, SXRRenderTexture posteffectRenderTextureA, SXRRenderTexture posteffectRenderTextureB) {
        NativeRenderTarget.render(getNative(), camera.getNative(), shaderManager.getNative(), posteffectRenderTextureA.getNative(), posteffectRenderTextureB.getNative(), scene.getNative(), scene);
    }

    public void setMainScene(SXRScene scene){
        mScene = scene;
        NativeRenderTarget.setMainScene(getNative(),scene.getNative());
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
    static native long defaultCtor(long scene);
    static native long ctorViewport(long scene, int defaultViewportW, int defaultViewportH);
    static native long getComponentType();
    static native void setMainScene(long rendertarget, long scene);
    static native void beginRendering(long rendertarget, long camera);
    static native void endRendering(long rendertarget);
    static native long ctorMultiview(long texture, boolean isMultiview);
    static native void setCamera(long rendertarget, long camera);
    static native long ctor(long texture, long sourceRendertarget);
    static native void cullFromCamera(long scene, SXRScene javaNode, long renderTarget,long camera, long shader_manager );
    static native void render(long renderTarget, long camera, long shader_manager, long posteffectrenderTextureA, long posteffectRenderTextureB, long scene, SXRScene javaNode);
    static native void setTexture(long rendertarget, long texture);
    static native void attachRenderTarget(long renderTarget, long nextRenderTarget);
}
