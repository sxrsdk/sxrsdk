#pragma once

#include <glm/glm.hpp>
#include <glm/ext.hpp>
#include <glm/vec4.hpp>
#include "LinearMath/btIDebugDraw.h"
#include "objects/shader_data.h"
#include "engine/renderer/renderer.h"
#include "../../bullet3/include/LinearMath/btIDebugDraw.h"

namespace sxr
{
    class Scene;
    class ShaderManager;
    class Node;
    class ShaderData;
}

// Helper class; draws the world as seen by Bullet.
// This is very handy to see it Bullet's world matches yours
// How to use this class :
// Declare an instance of the class :
//
// dynamicsWorld->setDebugDrawer(&mydebugdrawer);
// Each frame, call it :
// mydebugdrawer.SetMatrices(ViewMatrix, ProjectionMatrix);
// dynamicsWorld->debugDrawWorld();

class GLDebugDrawer : public btIDebugDraw
{
public:
    GLDebugDrawer(sxr::Node*);

    virtual void drawLine(const btVector3& from, const btVector3& to, const btVector3& color);
    virtual void drawContactPoint(const btVector3 &, const btVector3 &, btScalar, int, const btVector3 &) {}
    virtual void reportErrorWarning(const char *) {}
    virtual void draw3dText(const btVector3 &, const char *) {}

    virtual void setDebugMode(int p);

    int getDebugMode() const
    {
        return mMode & (btIDebugDraw::DBG_DrawConstraints
//        | btIDebugDraw::DBG_DrawConstraintLimits
//        | btIDebugDraw::DBG_DrawContactPoints
        | btIDebugDraw::DBG_DrawAabb
        | btIDebugDraw::DBG_DrawWireframe);
    }

    virtual void clearLines();
    virtual void flushLines();

    const static int MAX_VERTICES;

private:
    sxr::Mesh*         mMesh;
    sxr::ShaderData*   mMaterial;
    sxr::Node*         mNode;
    glm::vec3*         mPositions;
    glm::vec3*         mColors;
    int                mNumVerts;
    int                mMode;
    int                mMaxVerts;
};
