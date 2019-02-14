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

#include "render_target.h"
#include "engine/renderer/render_sorter.h"
#include "objects/textures/render_texture.h"
#include "objects/light.h" // for DEBUG_LIGHT
#include "objects/scene.h"
#include "objects/components/perspective_camera.h"

namespace sxr {

/**
 * Constructs a render target component which renders to a designated texture.
 * The scene will be rendered from the viewpoint of the scene object
 * the RenderTarget is attached to. Nothing will be rendered if
 * the render target is not attached to a scene object or
 * if it does not have a texture.
 *
 * If a RenderTarget is actually a ShadowMap, it is rendered
 * automatically by the lighting code. Otherwise, the
 * Java application is responsible for initiating rendering.
 *
 * @param texture RenderTexture to render to
 */
RenderTarget::RenderTarget(RenderTexture* tex, bool is_multiview, bool is_stereo)
:   Component(RenderTarget::getComponentType()),
    mNextRenderTarget(nullptr),
    mRenderTexture(tex)
{
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.is_stereo = is_stereo;
    mRenderState.is_multiview = is_multiview;
    if (nullptr != mRenderTexture)
    {
        mRenderState.sampleCount = mRenderTexture->getSampleCount();
        mDefaultHeight = mRenderTexture->height();
        mDefaultWidth = mRenderTexture->width();
    }
}

RenderTarget::RenderTarget(Scene* scene, int w, int h)
  :   Component(RenderTarget::getComponentType()),
      mNextRenderTarget(nullptr),
      mRenderTexture(nullptr),
      mDefaultWidth(w),
      mDefaultHeight(h)
{
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.is_multiview = false;
    mRenderState.camera = scene->main_camera_rig()->center_camera();
    mRenderState.is_stereo = true;
}

RenderTarget::RenderTarget(RenderTexture* tex, const RenderTarget* source)
    : Component(RenderTarget::getComponentType()),
      mNextRenderTarget(nullptr),
      mRenderTexture(tex)
{
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.is_stereo = source->mRenderState.is_stereo;
    mRenderState.is_multiview = source->mRenderState.is_multiview;
    mRenderState.sampleCount = tex->getSampleCount();
    mDefaultWidth = tex->width();
    mDefaultHeight = tex->height();
}

RenderTarget::~RenderTarget()
{
    if (mRenderSorter)
    {
        delete mRenderSorter;
        mRenderSorter = nullptr;
    }
};

void RenderTarget::cullFromCamera(Scene* scene, jobject javaSceneObject, Camera* camera, ShaderManager* shader_manager)
{
    checkGLError("RenderTarget::cullFromCamera");
    mRenderState.camera = camera;
    mRenderState.scene = scene;
    mRenderState.javaSceneObject = javaSceneObject;
    mRenderState.shader_manager = shader_manager;
    // TODO: lock scene graph matrices around cull
    mRenderSorter->cull(mRenderState);
    // TODO: can unlock matrices here - they are copied
    mRenderSorter->sort(mRenderState, *Renderer::getInstance());
    mRenderState.javaSceneObject = nullptr;
}

void RenderTarget::beginRendering(Renderer& renderer)
{
    if (mRenderTexture == nullptr)
    {
        return;
    }
    mRenderTexture->useStencil(renderer.useStencilBuffer());
    mRenderState.sampleCount = mRenderTexture->getSampleCount();
    if (-1 != mRenderState.camera->background_color_r())
    {
        mRenderTexture->setBackgroundColor(mRenderState.camera->background_color_r(),
                                           mRenderState.camera->background_color_g(),
                                           mRenderState.camera->background_color_b(), mRenderState.camera->background_color_a());
    }
    checkGLError("RenderTarget::beginRendering");
    mRenderTexture->beginRendering(&renderer);
}

void RenderTarget::endRendering(Renderer& renderer)
{
    if (mRenderTexture)
    {
        mRenderTexture->endRendering(&renderer);
        checkGLError("RenderTarget::endRendering");
    }
}

void RenderTarget::render(Renderer& r)
{
    mRenderSorter->render(mRenderState, r);
}

void RenderTarget::setRenderSorter(RenderSorter* sorter)
{
    mRenderSorter = sorter;
}

/**
 * Designates the RenderTexture this RenderTarget should render to.
 * @param RenderTexture to render to
 * @see #getTexture()
 * @see RenderTexture
 */
void RenderTarget::setTexture(RenderTexture* texture)
{
    mRenderTexture = texture;
    mRenderState.sampleCount = mRenderTexture->getSampleCount();
}

}
