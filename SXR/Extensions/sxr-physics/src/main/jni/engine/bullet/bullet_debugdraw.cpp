
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
        "uniform mat4 u_mvp;\n"
        "void main()\n"
        "{\n"
        "\tgl_Position = u_mvp * vec4(a_position, 1);\n"
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
                                 "mat4 u_mvp", "", "float3 a_position float3 a_color",
                                 vertex_shader, fragment_shader);
    sxr::RenderPass* pass = new sxr::RenderPass();
    sxr::RenderData* rd = renderer->createRenderData();
    sxr::Mesh* mesh;

    mMaterial = renderer->createMaterial("mat4 u_mvp  float line_width", "");
    mScene = scene;
    mNode = new sxr::Node();
    mVertexBuffer = renderer->createVertexBuffer("float3 a_position float3 a_color", MAX_VERTICES);
    mesh = new sxr::Mesh(*mVertexBuffer);
    pass->set_material(mMaterial);
    pass->set_shader(shaderID, false);
    rd->set_mesh(mesh);
    rd->set_draw_mode(GL_LINES);
    rd->set_rendering_order(sxr::RenderData::Overlay);
    rd->add_pass(pass);
    mMaterial->setFloat("line_width", 5.0f);
    mesh->setVertexBuffer(mVertexBuffer);
    mNode->attachComponent(rd);
    mNode->set_enable(false);
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

    view_proj *= view;
    mNumVerts = 0;
    mMaterial->setFloatVec("u_mvp", glm::value_ptr(view_proj), 16);
}

void GLDebugDrawer::flushLines()
{
    mVertexBuffer->markDirty();
}

void GLDebugDrawer::drawLine(const btVector3 &from, const btVector3 &to, const btVector3 &color)
{
    if (mNumVerts + 12 > MAX_VERTICES)
    {
        return;
    }
    float* vertexData = (float*) mVertexBuffer->getVertexData();

    vertexData += mNumVerts * 12;
    ++mNumVerts;
    *vertexData++ = from.x();
    *vertexData++ = from.y();
    *vertexData++ = from.z();
    *vertexData++ = color.x();
    *vertexData++ = color.y();
    *vertexData++ = color.z();
    *vertexData++ = to.x();
    *vertexData++ = to.y();
    *vertexData++ = to.z();
    *vertexData++ = color.x();
    *vertexData++ = color.y();
    *vertexData = color.z();
}
