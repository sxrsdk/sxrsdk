
#include "bullet_debugdraw.h"
#include "objects/scene.h"
#include "objects/components/camera.h"
#include "objects/components/perspective_camera.h"
#include "engine/renderer/renderer.h"
#include "shaders/shader_manager.h"
#include "objects/components/render_data.h"
#include "objects/render_pass.h"

static const char* vertex_shader =
        "precision mediump float;\n"
        "attribute vec3 a_position;\n"
        "attribute vec3 a_color;\n"
        "varying vec3 vertex_color;\n"
        "uniform mat4 u_vp;\n"
        "void main()\n"
        "{\n"
        "\tgl_Position = u_vp * vec4(a_position, 1);\n"
        "\tvertex_color = a_color;\n"
        "}";

static const char* fragment_shader =
        "precision highp float;\n"
        "varying vec3 vertex_color;\n"
        "void main()\n"
        "{\n"
        "    gl_FragColor = vec4(vertex_color, 1);\n"
        "}";

const int GLDebugDrawer::MAX_VERTICES = 5000;

GLDebugDrawer::GLDebugDrawer(sxr::Scene* scene, sxr::ShaderManager* sm)
{
    sxr::Renderer* renderer = sxr::Renderer::getInstance();
    int shaderID = sm->addShader("Bullet$a_position$a_color",
                                 "mat4 u_vp", "", "float3 a_position float3 a_color",
                                 vertex_shader, fragment_shader);
    sxr::RenderPass* pass = new sxr::RenderPass();
    sxr::RenderData* rd = renderer->createRenderData();
    sxr::Transform* trans = new sxr::Transform();

    mMaterial = renderer->createMaterial("mat4 u_vp  float line_width", "");
    mScene = scene;
    mNode = new sxr::Node();
    mPositions = new glm::vec3[MAX_VERTICES];
    mColors = new glm::vec3[MAX_VERTICES];
    mMesh = new sxr::Mesh("float3 a_position float3 a_color");
    pass->set_material(mMaterial);
    pass->set_shader(shaderID, false);
    rd->set_mesh(mMesh);
    rd->set_draw_mode(GL_LINES);
    rd->set_rendering_order(sxr::RenderData::Overlay);
    pass->set_cull_face(sxr::RenderPass::CullNone);
    rd->add_pass(pass);
    mMaterial->setFloat("line_width", 5.0f);
    mNode->attachComponent(trans);
    mNode->attachComponent(rd);
    mScene->addNode(mNode);
}

void GLDebugDrawer::setDebugMode(int p)
{
    mNode->set_enable(p != 0);
    mMode = p;
}

void GLDebugDrawer::clearLines()
{
    sxr::Camera* cam = mScene->main_camera_rig()->center_camera();
    glm::mat4 view = cam->getViewMatrix();
    glm::mat4 view_proj = cam->getProjectionMatrix();

    view_proj = view * view_proj;
    mNumVerts = 0;
    mMaterial->setFloatVec("u_vp", glm::value_ptr(view_proj), 16);
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
    if (mNumVerts + 2 > MAX_VERTICES)
    {
        return;
    }
    glm::vec3* positions = &mPositions[mNumVerts];
    glm::vec3* colors = &mColors[mNumVerts];

    mNumVerts += 2;
    positions->x = from.x();
    positions->y = from.y();
    positions->z = from.z();
    colors->x = color.x();
    colors->y = color.y();
    colors->z = color.z();
    positions->x = to.x();
    positions->y = to.y();
    positions->z = to.z();
    colors->x = color.x();
    colors->y = color.y();
    colors->z = color.z();
}
