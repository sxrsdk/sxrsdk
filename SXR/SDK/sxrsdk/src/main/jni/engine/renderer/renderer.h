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

#ifndef RENDERER_H_
#define RENDERER_H_

#include <vector>
#include <memory>
#include <unordered_map>

#include "glm/glm.hpp"
#include "batch.h"
//#include "objects/eye_type.h"
#include "objects/mesh.h"
#include "objects/bounding_volume.h"
#include "shaders/shader_manager.h"
#include "batch_manager.h"

typedef unsigned long Long;

namespace sxr {
extern bool gUseMultiview;
struct RenderTextureInfo;
class Camera;
class Scene;
class Node;
class ShaderData;
class RenderData;
class RenderTarget;
class RenderTexture;
class Light;
class BitmapImage;
class CubemapImage;
class CompressedImage;
class FloatImage;
class VertexBuffer;
class IndexBuffer;
class UniformBlock;
class Image;
class RenderPass;
class Texture;
class RenderTarget;
class ShadowMap;

extern uint8_t *oculusTexData;
/*
 * These uniforms are commonly used in shaders.
 * They are calculated by the GearVRF renderer.
 */
struct ShaderUniformsPerObject {
    glm::mat4   u_model;        // Model matrix
    glm::mat4   u_proj;         // projection matrix
    glm::mat4   u_view;         // View matrix
    glm::mat4   u_view_[2];     // for multiview
    glm::mat4   u_view_inv;     // inverse of View matrix
    glm::mat4   u_view_inv_[2]; // inverse of View matrix
    glm::mat4   u_mv;           // ModelView matrix
    glm::mat4   u_mv_[2];       // ModelView matrix
    glm::mat4   u_mvp;          // ModelViewProjection matrix
    glm::mat4   u_mvp_[2];      // ModelViewProjection matrix
    glm::mat4   u_mv_it;        // inverse transpose of ModelView
    glm::mat4   u_mv_it_[2];    // inverse transpose of ModelView
    float       u_right;        // 1 = right eye, 0 = left
};

struct RenderState {
    int                     render_mask;
    int                     viewportX;
    int                     viewportY;
    int                     viewportWidth;
    int                     viewportHeight;
    bool                    lightsChanged;
    Scene*                  scene;
    jobject                 javaNode = nullptr;
    ShaderData*             material_override = nullptr;
    ShaderUniformsPerObject uniforms;
    ShaderManager*          shader_manager;
    ShadowMap*              shadow_map;
    bool                    is_shadow;
    bool                    is_multiview = false;
    Camera*                 camera;
    int                     sampleCount;
};
enum EYE{
    LEFT, RIGHT, MULTIVIEW
};
class Renderer {
public:
    void resetStats() {
        numberDrawCalls = 0;
        numberTriangles = 0;
    }
    bool isVulkanInstance(){
        return isVulkan_;
    }
    void freeBatch(Batch* batch){
        batch_manager->freeBatch(batch);
    }
    int getNumberDrawCalls() {
        return numberDrawCalls;
    }

    int getNumberTriangles() {
        return numberTriangles;
    }
    int incrementTriangles(int number=1){
        return numberTriangles += number;
    }
    int incrementDrawCalls(){
        return ++numberDrawCalls;
    }
    static Renderer* getInstance(std::string type =  " ");
    static void resetInstance(){
        //@todo fix for vulkan
        if (!isVulkan_) {
            delete instance;
            instance = NULL;
        }
    }
    static int getVulkanPropValue();
    virtual ShaderData* createMaterial(const char* uniform_desc, const char* texture_desc) = 0;
    virtual RenderData* createRenderData() = 0;
    virtual RenderData* createRenderData(RenderData*) = 0;
    virtual UniformBlock* createUniformBlock(const char* desc, int bindingPoint, const char* name, int numElems) = 0;
    virtual Image* createImage(int type, int format) = 0;
    virtual RenderPass* createRenderPass() = 0;
    virtual Texture* createTexture(int target = GL_TEXTURE_2D) = 0;
    virtual RenderTexture* createRenderTexture(int width, int height, int sample_count,
                                               int jcolor_format, int jdepth_format, bool resolve_depth,
                                               const TextureParameters* texture_parameters, int number_views) = 0;
    virtual RenderTexture* createRenderTexture(int width, int height, int sample_count, int layers, int jdepth_format) = 0;
    virtual RenderTexture* createRenderTexture(const RenderTextureInfo&)=0;
    virtual Shader* createShader(int id, const char* signature,
                                 const char* uniformDescriptor, const char* textureDescriptor,
                                 const char* vertexDescriptor, const char* vertexShader,
                                 const char* fragmentShader) = 0;
    virtual VertexBuffer* createVertexBuffer(const char* descriptor, int vcount) = 0;
    virtual IndexBuffer* createIndexBuffer(int bytesPerIndex, int icount) = 0;
    void updateTransforms(RenderState& rstate, UniformBlock* block, RenderData*);

    virtual void cullFromCamera(Scene *scene, jobject javaSceneObject, Camera* camera,
                                ShaderManager* shader_manager, std::vector<RenderData*>* render_data_vector);
    virtual void set_face_culling(int cull_face) = 0;

    virtual void renderRenderData(RenderState& rstate, RenderData* render_data);
    virtual RenderTarget* createRenderTarget(Scene*) = 0;
    virtual RenderTarget* createRenderTarget(Scene*, int defaultViewportW, int defaultViewportH) = 0;
    virtual RenderTarget* createRenderTarget(RenderTexture*, bool) = 0;
    virtual RenderTarget* createRenderTarget(RenderTexture*, const RenderTarget*) = 0;

    virtual void renderRenderTarget(Scene*, jobject javaSceneObject, RenderTarget* renderTarget, ShaderManager* shader_manager,
                                    RenderTexture* post_effect_render_texture_a, RenderTexture* post_effect_render_texture_b, std::vector<RenderData*>* render_data_vector)=0;
    virtual void restoreRenderStates(RenderData* render_data) = 0;
    virtual void setRenderStates(RenderData* render_data, RenderState& rstate) = 0;
    virtual Texture* createSharedTexture(int id) = 0;
    virtual bool renderWithShader(RenderState& rstate, Shader* shader, RenderData* renderData, ShaderData* shaderData, int) = 0;
    virtual void makeShadowMaps(Scene* scene, jobject javaSceneObject, ShaderManager* shader_manager) = 0;
    virtual Light* createLight(const char* uniformDescriptor, const char* textureDescriptor) = 0;
    virtual void occlusion_cull(RenderState& rstate, std::vector<Node*>* scene_objects, std::vector<RenderData*>* render_data_vector) = 0;
    virtual void updatePostEffectMesh(Mesh*) = 0;
    void addRenderData(RenderData *render_data, RenderState& rstate, std::vector<RenderData*>& renderList);
    void addRenderTarget(RenderTarget* renderTarget, EYE eye, int index){
        switch (eye) {
            case LEFT:
                mLeftRenderTarget[index] = renderTarget;
                break;
            case RIGHT:
                mRightRenderTarget[index] = renderTarget;
                break;
            case MULTIVIEW:
                mMultiviewRenderTarget[index] = renderTarget;
                break;
            default:
                LOGE("invalid Eye");
        }
    }
    RenderTarget* getRenderTarget(int index, int eye){
        switch (eye) {
            case LEFT:
                return mLeftRenderTarget[index];
            case RIGHT:
                return mRightRenderTarget[index];
            case MULTIVIEW:
                return mMultiviewRenderTarget[index];
            default:
                FAIL("invalid eye");
        }
        return nullptr;
    }
private:
    RenderTarget* mLeftRenderTarget[3];
    RenderTarget* mRightRenderTarget[3];
    RenderTarget* mMultiviewRenderTarget[3];
    static bool isVulkan_;

    void build_frustum(float frustum[6][4], const float *vp_matrix);
    void frustum_cull(glm::vec3 camera_position, Scene *scene, Node *object,
                      float frustum[6][4], std::vector<Node *>* scene_objects,
                      bool continue_cull, int planeMask);

    Renderer(const Renderer& render_engine) = delete;
    Renderer(Renderer&& render_engine) = delete;
    Renderer& operator=(const Renderer& render_engine) = delete;
    Renderer& operator=(Renderer&& render_engine) = delete;
    BatchManager* batch_manager;
    static Renderer* instance;

protected:
    Renderer();
    virtual ~Renderer(){
        if(batch_manager)
            delete batch_manager;
        batch_manager = NULL;
    }

    virtual void renderMesh(RenderState& rstate, RenderData* render_data) = 0;
    virtual void renderMaterialShader(RenderState& rstate, RenderData* render_data, ShaderData *material, Shader* shader) = 0;

    virtual bool occlusion_cull_init(RenderState& , std::vector<Node*>* scene_objects,  std::vector<RenderData*>* render_data_vector);
    virtual bool renderPostEffectData(RenderState& rstate, RenderTexture* input_texture, RenderData* post_effect, int pass);

    int numberDrawCalls;
    int numberTriangles;
    bool useStencilBuffer_ = false;
public:
    virtual void state_sort(std::vector<RenderData*>& render_data_vector) ;
    int numLights;
    void setUseStencilBuffer(bool enable) { useStencilBuffer_ = enable; }
    bool useStencilBuffer(){
        return  useStencilBuffer_;
    }

    static constexpr int MAX_LAYERS = 2;
    static constexpr int LAYER_NORMAL = 0;
    static constexpr int LAYER_CURSOR = 1;
};
extern Renderer* gRenderer;
}
#endif
