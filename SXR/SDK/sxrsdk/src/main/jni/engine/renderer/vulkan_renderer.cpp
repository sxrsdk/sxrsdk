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

/***************************************************************************
 * Renders a scene, a screen.
 ***************************************************************************/

#include <vulkan/vulkan_index_buffer.h>
#include <vulkan/vulkan_vertex_buffer.h>
#include <vulkan/vk_cubemap_image.h>
#include <vulkan/vk_render_to_texture.h>
#include <vulkan/vk_render_target.h>
#include <vulkan/vk_light.h>
#include "renderer.h"
#include "glm/gtc/matrix_inverse.hpp"

#include "objects/scene.h"
#include "objects/components/skin.h"
#include "vulkan/vulkan_shader.h"
#include "vulkan_renderer.h"
#include "vulkan/vulkan_material.h"
#include "vulkan/vulkan_render_data.h"
#include "vulkan/vk_texture.h"
#include "vulkan/vk_bitmap_image.h"

#include <glslang/Include/Common.h>

namespace sxr {
ShaderData* VulkanRenderer::createMaterial(const char* uniform_desc, const char* texture_desc)
{
    return new VulkanMaterial(uniform_desc, texture_desc);
}

RenderTexture* VulkanRenderer::createRenderTexture(const RenderTextureInfo& renderTextureInfo) {
    return new VkRenderTexture(renderTextureInfo.fboWidth, renderTextureInfo.fboHeight, DEPTH_IMAGE | COLOR_IMAGE, 1, renderTextureInfo.multisamples);
}

RenderTexture* VulkanRenderer::createRenderTexture(int width, int height, int sample_count, int layers, int jdepth_format) {
    return new VkRenderTexture(width, height, DEPTH_IMAGE | COLOR_IMAGE, layers, sample_count);
}

Light* VulkanRenderer::createLight(const char* uniformDescriptor, const char* textureDescriptor)
{
    return new VKLight(uniformDescriptor, textureDescriptor);
}

RenderData* VulkanRenderer::createRenderData()
{
    return new VulkanRenderData();
}

RenderData* VulkanRenderer::createRenderData(RenderData* data)
{
    return new VulkanRenderData(*data);
}

RenderTarget* VulkanRenderer::createRenderTarget(Scene* scene) {
    return new VkRenderTarget(scene);
}

RenderTarget* VulkanRenderer::createRenderTarget(Scene* scene, int defaultViewportW, int defaultViewPortH) {
    return new VkRenderTarget(scene);
}

RenderTarget* VulkanRenderer::createRenderTarget(RenderTexture* renderTexture, bool isMultiview)
{
    return new VkRenderTarget(renderTexture, isMultiview);
}

RenderTarget* VulkanRenderer::createRenderTarget(RenderTexture* renderTexture, const RenderTarget* renderTarget)
{
    return new VkRenderTarget(renderTexture, renderTarget);
}

RenderPass* VulkanRenderer::createRenderPass(){
    return new VulkanRenderPass();
}

UniformBlock* VulkanRenderer::createUniformBlock(const char* desc, int binding, const char* name, int maxelems)
{
    if (maxelems <= 1)
    {
        return new VulkanUniformBlock(desc, binding, name);
    }

    return new VulkanUniformBlock(desc, binding, name, maxelems);
}

Image* VulkanRenderer::createImage(int type, int format)
{
    switch (type)
    {
        case Image::ImageType::BITMAP: return new VkBitmapImage(format);
        case Image::ImageType::CUBEMAP: return new VkCubemapImage(format);
            //    case Image::ImageType::FLOAT_BITMAP: return new GLFloatImage();
    }
    return NULL;
}

Texture* VulkanRenderer::createTexture(int target)
{
    // TODO: where to send the target
    return new VkTexture(static_cast<int>(VK_IMAGE_TYPE_2D));
}

RenderTexture* VulkanRenderer::createRenderTexture(int width, int height, int sample_count,
                                                   int jcolor_format, int jdepth_format, bool resolve_depth,
                                                   const TextureParameters* texture_parameters, int number_views)
{
    return new VkRenderTexture(width, height, DEPTH_IMAGE | COLOR_IMAGE, 1, sample_count);
}

Shader* VulkanRenderer::createShader(int id, const char* signature,
                                     const char* uniformDescriptor, const char* textureDescriptor,
                                     const char* vertexDescriptor, const char* vertexShader,
                                     const char* fragmentShader)
{
    return new VulkanShader(id, signature, uniformDescriptor, textureDescriptor, vertexDescriptor, vertexShader, fragmentShader);
}

VertexBuffer* VulkanRenderer::createVertexBuffer(const char* desc, int vcount)
{
    return new VulkanVertexBuffer(desc, vcount);
}

IndexBuffer* VulkanRenderer::createIndexBuffer(int bytesPerIndex, int icount)
{
    return new VulkanIndexBuffer(bytesPerIndex, icount);
}

bool VulkanRenderer::renderWithShader(RenderState& rstate, Shader* shader, RenderData* rdata, ShaderData* shaderData,  int pass)
{

    // Normal Rendering
    VulkanRenderData* vkRdata = static_cast<VulkanRenderData*>(rdata);
    UniformBlock& transformUBO = vkRdata->getTransformUbo();
    VulkanMaterial* vkmtl = static_cast<VulkanMaterial*>(shaderData);
    VulkanRenderPass * rp = vkRdata->getRenderPass(pass);

    // For Shadowmap just replace the shader
    ShaderData* curr_material = rstate.material_override;
    bool shadowFlag = false;
    if (rstate.is_shadow && curr_material) {
        const char *depthShaderName = (rdata->owner_object()->getComponent(Skin::getComponentType()) != nullptr)
                                      ? "SXRDepthShader$a_bone_weights$a_bone_indices"
                                      : "SXRDepthShader";
        shader = rstate.shader_manager->findShader(depthShaderName);
        if (shader == nullptr) {
            rstate.scene->makeDepthShaders(rstate.javaNode);
            shader = rstate.shader_manager->findShader(depthShaderName);
            if (shader == nullptr) {
                LOGE("Renderer::renderMesh cannot find depth shader %s", depthShaderName);
                return false;
            }
        }
        static_cast<VulkanShader*>(shader)->setDepthShaderFlag();

        rp = vkRdata->getShadowRenderPass();
        if(rp == nullptr){
            rp = (VulkanRenderPass*)createRenderPass();
            vkRdata->setShadowRenderPass(rp);
            shadowFlag = true;
        }else{
            shadowFlag = false;
        }
    }

    if (shader->usesMatrixUniforms())
    {
        updateTransforms(rstate, &transformUBO, rdata);
    }

    rdata->updateGPU(this, shader);
    LightList& lights = rstate.scene->getLights();
    vulkanCore_->InitLayoutRenderData(vkmtl, vkRdata, shader, lights);

    if(vkRdata->isDirty(pass) || shadowFlag) {
        VulkanMaterial* vkmtl = static_cast<VulkanMaterial*>(vkRdata->material(pass));
        vulkanCore_->InitDescriptorSetForRenderData(this, pass, shader, vkRdata, &lights,vkmtl);

        VkRenderPass render_pass = vulkanCore_->createVkRenderPass(NORMAL_RENDERPASS, rstate.sampleCount);

        std::string vkPipelineHashCode = std::to_string(shader) + vkRdata->getHashCode() +
                                         std::to_string(vkRdata->getRenderPass(pass)->getHashCode(rstate.is_multiview)) +
                                         std::to_string(rstate.sampleCount);

        VkPipeline pipeline = vulkanCore_->getPipeline(vkPipelineHashCode);
        if(pipeline == 0) {
            vkRdata->createPipeline(shader, this, pass, render_pass, rstate.sampleCount);
            vulkanCore_->addPipeline(vkPipelineHashCode, rp->m_pipeline);
        }
        else{
            if(!static_cast<VulkanShader*>(shader)->isDepthShader()){
                vkRdata->clearDirty();
            }
            rp->m_pipeline = pipeline;
        }
        if(shadowFlag)
            vkRdata->markDirty();
    }
    return true;
}

void VulkanRenderer::updatePostEffectMesh(Mesh* copy_mesh)
{
    float positions[] = { -1.0f, +1.0f, 1.0f,
                          +1.0f, -1.0f, 1.0f,
                          -1.0f, -1.0f, 1.0f,

                          +1.0f, +1.0f, 1.0f,
                          +1.0f, -1.0f, 1.0f,
                          -1.0f, +1.0f, 1.0f,
    };

    float uvs[] = { 0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 0.0f,

                    1.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
    };

    const int position_size = sizeof(positions)/ sizeof(positions[0]);
    const int uv_size = sizeof(uvs)/ sizeof(uvs[0]);

    copy_mesh->setVertices(positions, position_size);
    copy_mesh->setFloatVec("a_texcoord", uvs, uv_size);
}

void VulkanRenderer::renderRenderDataVector(RenderState& rstate,std::vector<RenderData*>& render_data_vector, std::vector<RenderData*>& render_data_list){
    for (auto rdata = render_data_vector.begin(); rdata != render_data_vector.end(); ++rdata)
    {
        if (!(rstate.render_mask & (*rdata)->render_mask()))
            continue;

        for(int curr_pass = 0; curr_pass < (*rdata)->pass_count(); curr_pass++) {
            ShaderData *curr_material = (*rdata)->material(curr_pass);
            Shader *shader = rstate.shader_manager->getShader((*rdata)->get_shader(rstate.is_multiview,curr_pass));
            if (shader == NULL)
            {
                LOGE("SHADER: shader not found");
                continue;
            }
            if (!rstate.is_shadow || (*rdata)->cast_shadows()) {
                if (!renderWithShader(rstate, shader, (*rdata), curr_material, curr_pass))
                    break;
            }
            if(curr_pass == (*rdata)->pass_count()-1)
                render_data_list.push_back((*rdata));
        }
    }
}

void VulkanRenderer::renderRenderTarget(Scene* scene, jobject javaNode, RenderTarget* renderTarget, ShaderManager* shader_manager,
                                        RenderTexture* post_effect_render_texture_a, RenderTexture* post_effect_render_texture_b,
                                        std::vector<RenderData*>* render_data_vector)
{
    std::vector<RenderData*> render_data_list;
    Camera* camera = renderTarget->getCamera();
    RenderState rstate = renderTarget->getRenderState();
    RenderData* post_effects = camera->post_effect_data();
    rstate.scene = scene;
    rstate.shader_manager = shader_manager;
    rstate.uniforms.u_view = camera->getViewMatrix();
    rstate.uniforms.u_proj = camera->getProjectionMatrix();
    rstate.uniforms.u_view_inv = glm::inverse(camera->getViewMatrix());
    rstate.javaNode = javaNode;
    rstate.shadow_map = nullptr;
    rstate.lightsChanged = false;

    if(vulkanCore_->isSwapChainPresent())
        rstate.uniforms.u_proj = glm::mat4(1,0,0,0,  0,-1,0,0, 0,0,0.5,0, 0,0,0.5,1) * rstate.uniforms.u_proj;

    LightList& lights = scene->getLights();

    int postEffectCount = 0;

    if (!rstate.is_shadow) {
        rstate.render_mask = camera->render_mask();
        rstate.uniforms.u_right = rstate.render_mask & RenderData::RenderMaskBit::Right;
        rstate.material_override = NULL;

        rstate.lightsChanged = lights.isDirty();
        if (lights.usingUniformBlock()) {
            rstate.shadow_map = lights.updateLightBlock(this);
        } else {
            LOGE("Vulkan only supports UBO Lighting");
        }
    }

    renderRenderDataVector(rstate,*render_data_vector,render_data_list);
    VkRenderTarget *vk_renderTarget = static_cast<VkRenderTarget *>(renderTarget);

    if ((post_effects != NULL) &&
        (post_effect_render_texture_a != nullptr) &&
        (post_effects->pass_count() > 0)) {

        VkRenderTexture* renderTexture = static_cast<VkRenderTexture*>(post_effect_render_texture_a);
        VkRenderTexture* input_texture = renderTexture;
        vulkanCore_->BuildCmdBufferForRenderData(render_data_list, camera, shader_manager,
                                                 nullptr, renderTexture, false, rstate.is_shadow);

        vulkanCore_->submitCmdBuffer(renderTexture->getFenceObject(), renderTexture->getCommandBuffer());
        vulkanCore_->waitForFence(renderTexture->getFenceObject());

        postEffectCount = post_effects->pass_count();
        // Call Post Effect
        for (int i = 0; i < postEffectCount-1; i++) {
            if (i % 2 == 0)
            {
                renderTexture = static_cast<VkRenderTexture*>(post_effect_render_texture_b);
            }
            else
            {
                renderTexture = static_cast<VkRenderTexture*>(post_effect_render_texture_a);
            }

            if(!renderPostEffectData(rstate,input_texture,post_effects,i))
                return;

            VkCommandBuffer cmdbuffer = renderTexture->getCommandBuffer();
            vulkanCore_->BuildCmdBufferForRenderDataPE(cmdbuffer, rstate.shader_manager,camera, post_effects, renderTexture, i);
            vulkanCore_->submitCmdBuffer(renderTexture->getFenceObject(),cmdbuffer);
            vulkanCore_->waitForFence(renderTexture->getFenceObject());
            input_texture = renderTexture;
        }

        render_data_list.clear();
        render_data_list.push_back(post_effects);

        if(!renderPostEffectData(rstate,input_texture,post_effects,postEffectCount - 1))
            return;

        vulkanCore_->BuildCmdBufferForRenderData(render_data_list, camera, shader_manager, renderTarget, nullptr, true, rstate.is_shadow);
        vulkanCore_->submitCmdBuffer(
                static_cast<VkRenderTexture *>(renderTarget->getTexture())->getFenceObject(),
                vk_renderTarget->getCommandBuffer());
    }
    else {
        vulkanCore_->BuildCmdBufferForRenderData(render_data_list, camera, shader_manager,
                                                 renderTarget, nullptr, false, rstate.is_shadow);
        vulkanCore_->submitCmdBuffer(
                static_cast<VkRenderTexture *>(renderTarget->getTexture())->getFenceObject(),
                vk_renderTarget->getCommandBuffer());

        // Wait for shadowmap to be rendered
        if(rstate.is_shadow){
            int success = 0;
            while(success != 1){
                success = vulkanCore_->waitForFence(static_cast<VkRenderTexture *>(renderTarget->getTexture())->getFenceObject());
            }
        }

    }

    // Presenting image to swapchain
    if(vulkanCore_->isSwapChainPresent()) {
        vulkanCore_->waitForFence(static_cast<VkRenderTexture *>(renderTarget->getTexture())->getFenceObject());
        vulkanCore_->PresentBackBuffer();
    }

}

    /**
     * Generate shadow maps for all the lights that cast shadows.
     * The scene is rendered from the viewpoint of the light using a
     * special depth shader (SXRDepthShader) to create the shadow map.
     * @see Renderer::renderShadowMap Light::makeShadowMap
     */
    void VulkanRenderer::makeShadowMaps(Scene* scene, jobject javaNode, ShaderManager* shader_manager)
    {
        scene->getLights().makeShadowMaps(scene, javaNode, shader_manager);
    }

}