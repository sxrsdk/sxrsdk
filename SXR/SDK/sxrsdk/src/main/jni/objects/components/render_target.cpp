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
#include "component.inl"
#include "objects/textures/render_texture.h"
#include "objects/light.h" // for DEBUG_LIGHT
#include "objects/scene.h"

namespace sxr {

/**
 * Constructs a render target component which renders to a designated texture.
 * The scene will be rendered from the viewpoint of the node
 * the RenderTarget is attached to. Nothing will be rendered if
 * the render target is not attached to a node or
 * if it does not have a texture.
 *
 * If a RenderTarget is actually a ShadowMap, it is rendered
 * automatically by the lighting code. Otherwise, the
 * Java application is responsible for initiating rendering.
 *
 * @param texture RenderTexture to render to
 */
RenderTarget::RenderTarget(RenderTexture* tex, bool is_multiview)
    : Component(RenderTarget::getComponentType()), mRenderTexture(tex)
{
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.material_override = NULL;
    mRenderState.is_multiview = is_multiview;
    if (nullptr != mRenderTexture) {
        mRenderState.sampleCount = mRenderTexture->getSampleCount();
    }
}
void RenderTarget::beginRendering(Renderer *renderer) {
    if(mRenderTexture == nullptr) {
        glViewport(0,0,mRenderState.viewportWidth,mRenderState.viewportHeight);
        return;
    }

    mRenderTexture->useStencil(renderer->useStencilBuffer());
    mRenderState.viewportWidth = mRenderTexture->width();
    mRenderState.viewportHeight = mRenderTexture->height();
    mRenderState.sampleCount = mRenderTexture->getSampleCount();
    if (-1 != mRenderState.camera->background_color_r())
    {
        mRenderTexture->setBackgroundColor(mRenderState.camera->background_color_r(),
                                           mRenderState.camera->background_color_g(),
                                           mRenderState.camera->background_color_b(), mRenderState.camera->background_color_a());
    }
    mRenderTexture->beginRendering(renderer);
}
void RenderTarget::endRendering(Renderer *renderer) {
    if(mRenderTexture == nullptr)
        return;
    mRenderTexture->endRendering(renderer);
}
RenderTarget::RenderTarget(Scene* scene)
: Component(RenderTarget::getComponentType()) {
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.material_override = NULL;
    mRenderState.is_multiview = false;
    mRenderState.scene = scene;

}

RenderTarget::RenderTarget(Scene* scene, int defaultViewportW, int defaultViewportH)
        : Component(RenderTarget::getComponentType())
{
    mRenderState.is_shadow = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.material_override = NULL;
    mRenderState.is_multiview = false;
    mRenderState.scene = scene;
    mRenderState.viewportWidth = defaultViewportW;
    mRenderState.viewportHeight = defaultViewportH;
}

/**
 * Constructs an empty render target without a render texture.
 * This component will not render anything until a RenderTexture
 * is provided.
 */
RenderTarget::RenderTarget()
:   Component(RenderTarget::getComponentType())
{
    mRenderState.is_multiview = false;
    mRenderState.shadow_map = nullptr;
    mRenderState.is_shadow = false;
    mRenderState.material_override = NULL;
}

void RenderTarget::cullFromCamera(Scene* scene, jobject javaNode, Camera* camera, Renderer* renderer, ShaderManager* shader_manager){
    renderer->cullFromCamera(scene, javaNode, camera,shader_manager, mRenderDataVector);
    scene->getLights().shadersRebuilt();
    renderer->state_sort(mRenderDataVector[Renderer::LAYER_NORMAL]);
    renderer->state_sort(mRenderDataVector[Renderer::LAYER_CURSOR]);
}

RenderTarget::~RenderTarget()
{
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
