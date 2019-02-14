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

#include <contrib/glm/gtc/type_ptr.hpp>
#include <contrib/glm/gtx/string_cast.hpp>
#include "objects/components/skin.h"
#include "renderer.h"
#include "objects/scene.h"
#include "shaders/shader.h"

namespace sxr {

#define NUM_SCENE_MATRICES (MODEL - PROJECTION)
#define MAX_MATRICES    64

RenderSorter::RenderSorter(const char* name, int numMatrices, bool forceTransformBlock)
    :   mMemoryPool(nullptr),
        mMaxElems(128),
        mCurBlock(nullptr),
        mTransBlockIndex(0),
        mName(name),
        mFrameNum(0),
        mForceTransformBlock(forceTransformBlock),
        mMaxMatricesPerBlock(0),
        mNumMatricesInBlock(0)
{
    int nbytes = sizeof(BlockHeader) + sizeof(Renderable) * mMaxElems;
    Renderer* renderer = Renderer::getInstance();
    mCurBlock = mMemoryPool = static_cast<BlockHeader*>(malloc(nbytes));
    mCurBlock->nextBlock = nullptr;
    LOGD("RENDER: allocate block %d bytes", nbytes);
    if (numMatrices == 0)
    {
        numMatrices = renderer->getMaxArraySize(sizeof(glm::mat4));
        if (numMatrices > MAX_MATRICES)
        {
            numMatrices = MAX_MATRICES;
        }
        mCurBlock->numElems = 0;
    }
    UniformBlock* transformBlock = renderer->createTransformBlock(numMatrices);
    mNumMatricesInBlock = NUM_SCENE_MATRICES;
    mTransBlockIndex = 0;
    mTransformBlocks.push_back(transformBlock);
    mMaxMatricesPerBlock = transformBlock->getNumElems();
}

RenderSorter::~RenderSorter()
{
    BlockHeader* block = mMemoryPool;

    while (block != nullptr)
    {
        BlockHeader* next = block->nextBlock;
        free(block);
        block = next;
    }
    mMemoryPool = mCurBlock = nullptr;
    for (auto it = mTransformBlocks.begin();
         it != mTransformBlocks.end();
         ++it)
    {
        UniformBlock* transformBlock = *it;
        if (transformBlock)
        {
            delete transformBlock;
        }
    }
    mTransformBlocks.clear();
}

RenderSorter::Renderable* RenderSorter::alloc()
{
    int n = mCurBlock->numElems;
    if (mCurBlock->numElems >= mMaxElems)
    {
        BlockHeader* newBlock = mCurBlock->nextBlock;

        if (newBlock == nullptr)
        {
            int nbytes = sizeof(BlockHeader) + sizeof(Renderable) * mMaxElems;
            newBlock = static_cast<BlockHeader*>(malloc(nbytes));
            newBlock->nextBlock = nullptr;
            mCurBlock->nextBlock = newBlock;
            LOGD("RENDER: allocate block %d bytes", nbytes);
        }
        newBlock->numElems = n = 0;
        mCurBlock = newBlock;
    }
    Renderable* item = (Renderable*) (mCurBlock + 1);
    ++(mCurBlock->numElems);
    item += n;
    item->reset();
    return item;
}

void RenderSorter::frustum_cull(RenderState& rstate,
                            glm::vec3 camera_position,
                            Node *object,
                            float frustum[6][4],
                            bool need_cull,
                            int planeMask)
{

    // frustumCull() return 3 possible values:
    // 0 when the HBV of the object is completely outside the frustum: cull itself and all its children out
    // 1 when the HBV of the object is intersecting the frustum but the object itself is not: cull it out and continue culling test with its children
    // 2 when the HBV of the object is intersecting the frustum and the mesh BV of the object are intersecting (inside) the frustum: render itself and continue culling test with its children
    // 3 when the HBV of the object is completely inside the frustum: render itself and all its children without further culling test
    int cullVal;

    if (!object->enabled())
    {
        return;
    }

    //allows for on demand calculation of the camera distance; usually matters
    //when transparent objects are in play
    RenderData* renderData = object->render_data();
    std::lock_guard < std::recursive_mutex > lock(object->getLock());

    if (need_cull)
    {
        cullVal = object->frustumCull(camera_position, frustum, planeMask);
        if (cullVal == 0)
        {
            object->setCullStatus(true);
            return;
        }
        if (cullVal == 3)
        {
            need_cull = false;
        }
    }
    object->setCullStatus(false);
    add(rstate, object, *Renderer::getInstance());

    const std::vector<Node*> children = object->children();
    for (auto it = children.begin(); it != children.end(); ++it)
    {
        frustum_cull(rstate, camera_position, *it, frustum, need_cull, planeMask);
    }
}

Shader* RenderSorter::selectShader(const RenderState& rstate, Renderable& r, Renderer& renderer)
{
    int shaderID = r.renderPass->get_shader();
    Shader* shader = nullptr;

    if (shaderID >= 0)
    {
        shader = rstate.shader_manager->getShader(shaderID, rstate);
    }
    return shader;
}

/**
 * Adds a scene object to the list of renderables.
 * @param rstate RenderState
 * @param object Node to add
 */
void RenderSorter::add(RenderState& rstate, Node* object, Renderer& renderer)
{
    RenderData* rdata = object->render_data();

    if (rdata == nullptr)
    {
        return;
    }
    Mesh* geometry = rdata->mesh();

    if (geometry == nullptr)
    {
        return;
    }
    RenderPass* rpass = rdata->pass(0);
    RenderModes& modes = rpass->render_modes();

    if (modes.getRenderMask() == 0)
    {
        return;
    }

    Renderable* r = alloc();
    Skin* skin = (Skin*) object->getComponent(Skin::getComponentType());

    r->hasBones = skin ? 1 : 0;
    r->mesh = geometry;
    r->mvp = object->transform()->getModelMatrix();
    r->renderData = rdata;
    r->nextLevel = nullptr;
    r->nextSibling = nullptr;
    r->transformBlock = nullptr;
    r->renderPass = rpass;
    r->renderModes = modes;
    r->material = rpass->material();
    r->shader = selectShader(rstate, *r, renderer);
    ++mVisibleElems;
}


RenderSorter::Renderable* RenderSorter::add(RenderState& rstate, Renderable& r, Renderer& renderer)
{
    if (r.renderData != nullptr)
    {
        Renderable* elem = alloc();

        *elem = r;
        elem->mesh = r.renderData->mesh();
        elem->renderModes = r.renderPass->render_modes();
        elem->mvp = glm::mat4();
        elem->matrixOffset = -1;
        elem->nextLevel = nullptr;
        elem->nextSibling = nullptr;
        elem->transformBlock = nullptr;
        elem->shader = selectShader(rstate, *elem, renderer);
        ++mVisibleElems;
        return elem;
    }
    return nullptr;
}

void RenderSorter::updateTransform(RenderState& rstate, Renderable& r, Renderer& renderer)
{
    int numMatrices = r.shader->getOutputBufferSize();

    rstate.u_matrices[MODEL] = r.mvp;   // r.mvp is the model matrix from the cull step
    rstate.u_matrices[MVP] = rstate.u_matrices[VIEW_PROJ] * rstate.u_matrices[MODEL];
    r.transformBlock = nullptr;
    r.matrixOffset = -1;
    if (r.shader->usesMatrixUniforms())
    {
        numMatrices = r.shader->calcMatrix(rstate.u_matrices, mOutputMatrices);
        if (numMatrices == 0)
        {
            if (mForceTransformBlock)
            {
                numMatrices = 1;
                mOutputMatrices[0] = rstate.u_matrices[MVP];
            }
            else
            {
                r.mvp = rstate.u_matrices[MVP];
                return;
            }
        }
    }
    if (numMatrices > 0)
    {
        updateTransformBlock(r, renderer, numMatrices, (const float*) mOutputMatrices);
#ifdef DEBUG_TRANSFORM
        for (int i = 0; i < numMatrices; ++i)
        {
            std::string s = glm::to_string(mOutputMatrices[i]);
            LOGV("TRANSFORM: output matrix %d %s", i, s.c_str());
        }
#endif
    }
}

UniformBlock* RenderSorter::updateTransformBlock(
        Renderable& r,
        Renderer& renderer,
        int numMatrices,
        const float* matrixData)
{
    UniformBlock* transformBlock = mTransformBlocks[mTransBlockIndex];

    if (numMatrices + mNumMatricesInBlock >= mMaxMatricesPerBlock)
    {
        ++mTransBlockIndex;
        if (mTransBlockIndex < mTransformBlocks.size())
        {
            transformBlock = mTransformBlocks[mTransBlockIndex];
        }
        else
        {
            transformBlock = renderer.createTransformBlock(mMaxMatricesPerBlock);
            mTransformBlocks.push_back(transformBlock);
        }
        mNumMatricesInBlock = NUM_SCENE_MATRICES;
    }
    r.matrixOffset = mNumMatricesInBlock;
    mNumMatricesInBlock += numMatrices;
    r.transformBlock = transformBlock;
    transformBlock->setRange(r.matrixOffset, matrixData, numMatrices);
#ifdef DEBUG_TRANSFORM
    LOGV("TRANSFORM: using transform block #%d matrix offset = %d", mTransBlockIndex, r.matrixOffset);
#endif
    return transformBlock;
}

/**
 * After the cull stage determines which objects are visible,
 * this function determines which meshes are renderable.
 * This stage may create and compile shaders if necessary.
 * It updates the uniform blocks used to accumulate transforms
 * and sorts the Renderables in an optimal order for rendering.
 * @param rstate    RenderState to use for rendering
 */
void RenderSorter::validate(RenderState& rstate, Renderer& renderer)
{
    BlockHeader* block = mMemoryPool;

    while (block != nullptr)
    {
        Renderable* cur = (Renderable*) (block + 1);
        int n = block->numElems;
        block = block->nextBlock;
        for (int i = 0; i < n; ++i)
        {
            Renderable& r = *cur;

            if (--mVisibleElems < 0)
            {
                return;
            }
            if (isValid(rstate, r, renderer))
            {
                if (r.shader->usesMatrixUniforms())
                {
                    updateTransform(rstate, r, renderer);
                }
                renderer.validate(r);
                merge(cur);
            }
            ++cur;
        }
    }
}

/**
 * After the cull stage determines which objects are visible,
 * this function determines which meshes are renderable.
 * This stage may create and compile shaders if necessary.
 * It updates the uniform blocks used to accumulate transforms
 * and sorts the Renderables in an optimal order for rendering.
 * @param rstate    RenderState to use for rendering
 */
void RenderSorter::sort(RenderState& rstate, Renderer& renderer)
{
    mRenderList.nextSibling = nullptr;
    mRenderList.nextLevel = nullptr;
    rstate.javaEnv = nullptr;
    int detach = renderer.getJavaEnv(&rstate.javaEnv);

    /*
     * Generate required shaders and update the transforms
     * required for each shader
     */
    validate(rstate, renderer);
    /*
     * Update the transform blocks in the GPU
     */
    for (auto it = mTransformBlocks.begin(); it != mTransformBlocks.end(); ++it)
    {
        UniformBlock* transformBlock = *it;
        if ((transformBlock != nullptr) && (transformBlock->getNumElems() > NUM_SCENE_MATRICES))
        {
            glm::mat4* matrices = &rstate.u_matrices[PROJECTION];
            int nbytes = transformBlock->getNumElems() * transformBlock->getElemSize();
            transformBlock->setRange(0, matrices, NUM_SCENE_MATRICES);
            transformBlock->updateGPU(&renderer, 0, nbytes);
            transformBlock->setNumElems(NUM_SCENE_MATRICES);
        }
    }
    mTransBlockIndex = 0;
    mNumMatricesInBlock = NUM_SCENE_MATRICES;
    if (rstate.javaEnv)
    {
        if (detach)
        {
            renderer.detachJavaEnv();
        }
        rstate.javaEnv = nullptr;
    }
#ifdef DEBUG_RENDER
    dump();
#endif
}

void RenderSorter::merge(Renderable* item)
{
    mergeByShader(&mRenderList, item);
}


void RenderSorter::addListhead(Renderable* cur)
{
    Renderable* firstitem = alloc();

    *firstitem = *cur;
    cur->renderPass = nullptr;
    cur->renderData = nullptr;
    cur->renderModes.setRenderMask(0);
    firstitem->nextLevel = nullptr;
    firstitem->nextSibling = nullptr;
    cur->nextLevel = firstitem;
#ifdef DEBUG_RENDER
    LOGV("RENDER: listhead [%p] -> %p", cur, firstitem);
#endif
}

void RenderSorter::mergeByShader(Renderable* prev, Renderable* item)
{
    Renderable* cur = prev->nextLevel;
    int itemShader = item->shader->getShaderID();

#ifdef DEBUG_RENDER
    Node* owner = item->renderData->owner_object();
    int itemOrder = item->renderModes.getRenderOrder();
    const char* name = (owner ? owner->name().c_str() : "");
#endif
/*
* Add this item at the front of the list?
*/
    if ((cur == nullptr) || (itemShader < cur->shader->getShaderID()))
    {
        item->nextSibling = cur;
        prev->nextLevel = item;
#ifdef DEBUG_RENDER
        LOGV("RENDER: Front shader: %s order = %d shader = %d material = %p",
             name, itemOrder, itemShader, item->material);
#endif
        return;
    }
    /*
     * Scan the list to see where it fits
     */
    while (cur != nullptr)
    {
        if (itemShader < cur->shader->getShaderID())
        {
            item->nextSibling = cur;
            prev->nextSibling = item;
#ifdef DEBUG_RENDER
            LOGV("RENDER: Middle shader: %s order = %d shader = %d material = %p",
                 name, itemOrder, itemShader, item->material);
#endif
            return;
        }
        prev = cur;
        cur = cur->nextSibling;
    }
    prev->nextSibling = item;
#ifdef DEBUG_RENDER
    LOGV("RENDER: End shader: %s order = %d shader = %d material = %p",
         name, itemOrder, itemShader, item->material);
#endif
}

bool RenderSorter::isValid(RenderState& rstate, Renderable& r, Renderer& renderer)
{
    RenderData* rdata = r.renderData;
    RenderPass* rpass = r.renderPass;
    bool dirty = false;

    if (r.shader == nullptr)
    {
        dirty = true;
    }
    else
    {
        dirty = rdata->isDirty() |
                rpass->isDirty() |
                r.material->isDirty(ShaderData::NEW_TEXTURE);
    }
    rpass->clearDirty();
    /*
     * If any of the render passes are dirty, their shaders
     * may need rebuilding. bindShader calls a Java function
     * to regenerate shader sources if necessary. We check
     * all the render passes to make sure they have valid shaders.
     */
    if (dirty && rstate.javaEnv)
    {
        rdata->bindShader(rstate.javaEnv, rstate);
        r.shader = selectShader(rstate, r, renderer);
    }
    if (r.shader)
    {
        rdata->clearDirty();
        return true;
    }
    return false;
}


/**
 * Renders everything accumulated by the cull process.
 * The Renderables have been sorted into a tree structure based on
 * how they should be subsequently rendered. For the main scene,
 * it is sorted on rendering order, shader used and other rendering
 * properties. The structure of the tree depends on the sorting
 * needs of the render target.
 * @param rstate    RenderState to use for rendering
 */
void RenderSorter::render(RenderState& rstate, Renderer& renderer)
{
    render(rstate, mRenderList, renderer);
}

/**
 * The Renderables have been sorted into a tree structure based on
 * how they should be subsequently rendered. For the main scene,
 * it is sorted on rendering order, shader used and other rendering
 * properties. The structure of the tree depends on the sorting
 * needs of the render target.
 *
 * This function is called recursively for each level in the tree.
 * It could be multithreaded on Vulkan.
 *
 * If the renderable does not store its matrices in a transform block
 * they will be loaded directly into shader uniforms. Otherwise,
 * the transform block with the matrices for the shader is used.
 * @param rstate    RenderState to use for rendering
 * @param r         Renderable to render
 */
void RenderSorter::render(RenderState& rstate, const Renderable& r, Renderer& renderer)
{
    if (r.renderPass)
    {
        renderer.render(rstate, r);
    }
    else
    {
        const Renderable* next = &r;
        if (r.nextLevel)
        {
            next = r.nextLevel;
            render(rstate, *next, renderer);
        }
        while ((next = next->nextSibling))
        {
            render(rstate, *next, renderer);
        }
    }
}

/*
 * Initialize transforms for a specific camera viewpoint
 */
void RenderSorter::init(RenderState& rstate)
{
    Scene* scene = rstate.scene;

    if (rstate.is_stereo)
    {
        Camera* leftcam = scene->main_camera_rig()->left_camera();
        Camera* rightcam = scene->main_camera_rig()->right_camera();

        rstate.u_matrices[PROJECTION] = leftcam->getProjectionMatrix();
        rstate.u_matrices[VIEW] = leftcam->getViewMatrix();
        rstate.u_matrices[VIEW + 1] = rightcam->getViewMatrix();
    }
    else
    {
        Camera* camera = rstate.camera;

        rstate.u_matrices[PROJECTION] = camera->getProjectionMatrix();
        rstate.u_matrices[VIEW] = camera->getViewMatrix();
        rstate.u_matrices[VIEW + 1] = camera->getViewMatrix();
        rstate.is_multiview = false;
    }
    rstate.u_matrices[VIEW_PROJ] = rstate.u_matrices[PROJECTION] * rstate.u_matrices[VIEW];
    rstate.u_matrices[VIEW_PROJ + 1] = rstate.u_matrices[PROJECTION] * rstate.u_matrices[VIEW + 1];
    rstate.u_matrices[VIEW_INVERSE] = inverse(rstate.u_matrices[VIEW]);
    rstate.u_matrices[VIEW_INVERSE + 1] = inverse(rstate.u_matrices[VIEW + 1]);
    rstate.javaEnv = nullptr;
    rstate.transform_block = nullptr;
    ++mFrameNum;
    clear();
#ifdef DEBUG_TRANSFORM
        std::string s = glm::to_string(rstate.u_matrices[VIEW]);
        LOGV("TRANSFORM: LEFT VIEW %s", s.c_str());
        s = glm::to_string(rstate.u_matrices[VIEW + 1]);
        LOGV("TRANSFORM: RIGHT VIEW %s", s.c_str());
#endif
}

void RenderSorter::clear()
{
#ifdef DEBUG_RENDER
    LOGV("RENDER: clear visible and render list");
#endif
    mCurBlock = mMemoryPool;
    mCurBlock->numElems = 0;
    mVisibleElems = 0;
    mRenderList.shader = nullptr;
    mRenderList.material = nullptr;
    mRenderList.transformBlock = nullptr;
    mRenderList.mesh = nullptr;
    mRenderList.renderData = nullptr;
    mRenderList.renderPass = nullptr;
    mRenderList.nextSibling = nullptr;
    mRenderList.nextLevel = nullptr;
}

/*
 * Perform view frustum culling from a specific camera viewpoint
 */
void RenderSorter::cull(RenderState& rstate)
{
    Scene* scene = rstate.scene;
    Camera* camera = rstate.camera;
    glm::mat4 view_matrix = camera->getViewMatrix();
    glm::mat4 vp_matrix = camera->getProjectionMatrix() * view_matrix;
    glm::vec3 campos(view_matrix[3]);

#ifdef DEBUG_RENDER
    LOGE("RENDER: %s frame %d %d meshes, %d triangles", mName.c_str(), mFrameNum, scene->getNumberDrawCalls(), scene->getNumberTriangles());
#endif
    init(rstate);
    // Travese all scene objects in the scene as a tree and do frustum culling at the same time if enabled
    // 1. Build the view frustum
    float frustum[6][4];
    build_frustum(frustum, (const float*) glm::value_ptr(vp_matrix));
    rstate.camera_position = campos;
    // 2. Iteratively execute frustum culling for each root object (as well as its children objects recursively)
    Node* object = scene->getRoot();
#ifdef DEBUG_CULL
     LOGD("FRUSTUM: start frustum culling for root %s\n", object->name().c_str());
#endif
    frustum_cull(rstate, campos, object, frustum, scene->get_frustum_culling(), 0);
#ifdef DEBUG_CULL
     LOGD("FRUSTUM: end frustum culling for root %s\n", object->name().c_str());
#endif
}

void RenderSorter::build_frustum(float frustum[6][4], const float *vp_matrix)
{
    float t;

    /* Extract the numbers for the RIGHT plane */
    frustum[0][0] = vp_matrix[3] - vp_matrix[0];
    frustum[0][1] = vp_matrix[7] - vp_matrix[4];
    frustum[0][2] = vp_matrix[11] - vp_matrix[8];
    frustum[0][3] = vp_matrix[15] - vp_matrix[12];

    /* Normalize the result */
    t = sqrt(
            frustum[0][0] * frustum[0][0] + frustum[0][1] * frustum[0][1]
                    + frustum[0][2] * frustum[0][2]);
    frustum[0][0] /= t;
    frustum[0][1] /= t;
    frustum[0][2] /= t;
    frustum[0][3] /= t;

    /* Extract the numbers for the LEFT plane */
    frustum[1][0] = vp_matrix[3] + vp_matrix[0];
    frustum[1][1] = vp_matrix[7] + vp_matrix[4];
    frustum[1][2] = vp_matrix[11] + vp_matrix[8];
    frustum[1][3] = vp_matrix[15] + vp_matrix[12];

    /* Normalize the result */
    t = sqrt(
            frustum[1][0] * frustum[1][0] + frustum[1][1] * frustum[1][1]
                    + frustum[1][2] * frustum[1][2]);
    frustum[1][0] /= t;
    frustum[1][1] /= t;
    frustum[1][2] /= t;
    frustum[1][3] /= t;

    /* Extract the BOTTOM plane */
    frustum[2][0] = vp_matrix[3] + vp_matrix[1];
    frustum[2][1] = vp_matrix[7] + vp_matrix[5];
    frustum[2][2] = vp_matrix[11] + vp_matrix[9];
    frustum[2][3] = vp_matrix[15] + vp_matrix[13];

    /* Normalize the result */
    t = sqrt(
            frustum[2][0] * frustum[2][0] + frustum[2][1] * frustum[2][1]
                    + frustum[2][2] * frustum[2][2]);
    frustum[2][0] /= t;
    frustum[2][1] /= t;
    frustum[2][2] /= t;
    frustum[2][3] /= t;

    /* Extract the TOP plane */
    frustum[3][0] = vp_matrix[3] - vp_matrix[1];
    frustum[3][1] = vp_matrix[7] - vp_matrix[5];
    frustum[3][2] = vp_matrix[11] - vp_matrix[9];
    frustum[3][3] = vp_matrix[15] - vp_matrix[13];

    /* Normalize the result */
    t = sqrt(
            frustum[3][0] * frustum[3][0] + frustum[3][1] * frustum[3][1]
                    + frustum[3][2] * frustum[3][2]);
    frustum[3][0] /= t;
    frustum[3][1] /= t;
    frustum[3][2] /= t;
    frustum[3][3] /= t;

    /* Extract the FAR plane */
    frustum[4][0] = vp_matrix[3] - vp_matrix[2];
    frustum[4][1] = vp_matrix[7] - vp_matrix[6];
    frustum[4][2] = vp_matrix[11] - vp_matrix[10];
    frustum[4][3] = vp_matrix[15] - vp_matrix[14];

    /* Normalize the result */
    t = sqrt(
            frustum[4][0] * frustum[4][0] + frustum[4][1] * frustum[4][1]
                    + frustum[4][2] * frustum[4][2]);
    frustum[4][0] /= t;
    frustum[4][1] /= t;
    frustum[4][2] /= t;
    frustum[4][3] /= t;

    /* Extract the NEAR plane */
    frustum[5][0] = vp_matrix[3] + vp_matrix[2];
    frustum[5][1] = vp_matrix[7] + vp_matrix[6];
    frustum[5][2] = vp_matrix[11] + vp_matrix[10];
    frustum[5][3] = vp_matrix[15] + vp_matrix[14];

    /* Normalize the result */
    t = sqrt(
            frustum[5][0] * frustum[5][0] + frustum[5][1] * frustum[5][1]
                    + frustum[5][2] * frustum[5][2]);
    frustum[5][0] /= t;
    frustum[5][1] /= t;
    frustum[5][2] /= t;
    frustum[5][3] /= t;
}

void RenderSorter::dump()
{
    if (mRenderList.nextLevel)
    {
        LOGD("RENDER: %s frame %d RenderList", mName.c_str(), mFrameNum);
        dump(mRenderList, "");
    }
}

void RenderSorter::dump(const Renderable& r, const std::string& pad) const
{
    std::string nextPad = pad + "    ";

    if (r.renderPass)
    {
        Node* owner = r.renderData ? r.renderData->owner_object() : nullptr;
        const char* name = (owner ? owner->name().c_str() : "");
        int order = r.renderModes.getRenderOrder();
        int shader = r.shader->getShaderID();

        LOGD("RENDER: %s [%p] %s order = %d dist = %f shader = %d material = %p",
             pad.c_str(), &r, name, order, r.distanceFromCamera, shader, r.material);
    }
    else
    {
        Renderable* next = r.nextLevel;

        if (next)
        {
            dump(*next, nextPad);
            while ((next = next->nextSibling))
            {
                dump(*next, nextPad);
            }
        }
    }
}

bool RenderSorter::findRenderable(const Renderable* root, const Renderable* findme) const
{
    if (root == findme)
    {
        return true;
    }
    Renderable* next = root->nextLevel;

    if (next)
    {
        if (findRenderable(next, findme))
        {
            return true;
        }
        while ((next = next->nextSibling))
        {
            if (findRenderable(next, findme))
            {
                return true;
            }
        }
    }
    return false;
}

}
