
#include "bullet_debugdraw.h"
#include "objects/scene.h"
#include "objects/components/camera.h"
#include "objects/components/perspective_camera.h"
#include "engine/renderer/renderer.h"
#include "shaders/shader_manager.h"
#include "objects/components/render_data.h"
#include "objects/render_pass.h"


GLDebugDrawer::GLDebugDrawer(sxr::Node* node)
: mNode(node)
{
    mMaterial = node->render_data()->material(0);
    mMesh = node->render_data()->mesh();
    mMaxVerts = node->render_data()->mesh()->getVertexCount();
    mPositions = new glm::vec3[mMaxVerts];
    mColors = new glm::vec3[mMaxVerts];
}

void GLDebugDrawer::setDebugMode(int p)
{
    mNode->set_enable(p != 0);
    mMode = p;
}

void GLDebugDrawer::clearLines()
{
    sxr::Camera* cam = sxr::Scene::main_scene()->main_camera_rig()->center_camera();
    glm::mat4 model = mNode->transform()->getLocalModelMatrix();
    glm::mat4 view = cam->getViewMatrix();
    glm::mat4 mvp = cam->getProjectionMatrix();

    mvp *= view;
    mvp *= model;
    mNumVerts = 0;
    mMaterial->setFloatVec("u_vp", glm::value_ptr(mvp), 16);
}

void GLDebugDrawer::flushLines()
{
    sxr::Renderer* renderer = sxr::Renderer::getInstance();
    sxr::VertexBuffer* vbuf = renderer->createVertexBuffer("float3 a_position float3 a_color", mNumVerts);
    vbuf->setFloatVec("a_position", (float*) &mPositions[0], mNumVerts * 3, 3);
    vbuf->setFloatVec("a_color", (float*) &mColors[0], mNumVerts * 3, 3);
    sxr::VertexBuffer* oldbuf = mMesh->getVertexBuffer();
    mMesh->setVertexBuffer(vbuf);
    mNode->dirtyHierarchicalBoundingVolume();
    sxr::BoundingVolume& bv = mNode->getBoundingVolume();
    if (oldbuf)
    {
        delete oldbuf;
    }
}

void GLDebugDrawer::drawLine(const btVector3 &from, const btVector3 &to, const btVector3 &color)
{
    if (mNumVerts + 2 > mMaxVerts)
    {
        return;
    }
    glm::vec3* positions = &mPositions[mNumVerts];
    glm::vec3* colors = &mColors[mNumVerts];

    mNumVerts += 2;
    positions->x = from.x();
    positions->y = from.y();
    positions->z = from.z();
    ++positions;
    colors->x = color.x();
    colors->y = color.y();
    colors->z = color.z();
    ++colors;
    positions->x = to.x();
    positions->y = to.y();
    positions->z = to.z();
    colors->x = color.x();
    colors->y = color.y();
    colors->z = color.z();
}
