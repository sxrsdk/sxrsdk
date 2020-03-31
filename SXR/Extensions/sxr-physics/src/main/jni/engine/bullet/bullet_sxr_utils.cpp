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

#include "bullet_sxr_utils.h"

#include <BulletCollision/CollisionShapes/btShapeHull.h>
#include <BulletCollision/CollisionShapes/btCapsuleShape.h>
#include <contrib/glm/gtc/type_ptr.hpp>
#include <BulletCollision/CollisionShapes/btEmptyShape.h>

#include "util/sxr_log.h"

namespace sxr {

btCollisionShape *convertCollider2CollisionShape(Collider *collider)
{
    if (collider == nullptr)
    {
        return nullptr;
    }
    if (collider->shape_type() == COLLIDER_SHAPE_BOX)
    {
        return convertBoxCollider2CollisionShape(static_cast<BoxCollider *>(collider));
    }
    else if (collider->shape_type() == COLLIDER_SHAPE_SPHERE)
    {
        return convertSphereCollider2CollisionShape(static_cast<SphereCollider *>(collider));
    }
    else if (collider->shape_type() == COLLIDER_SHAPE_MESH)
    {
        return convertMeshCollider2CollisionShape(static_cast<MeshCollider *>(collider));
    }
    else if (collider->shape_type() == COLLIDER_SHAPE_CAPSULE)
    {
        return convertCapsuleCollider2CollisionShape(static_cast<CapsuleCollider *>(collider));
    }
    return NULL;
}

btCollisionShape *convertSphereCollider2CollisionShape(SphereCollider *collider)
{
    Node* owner = collider->owner_object();
    Mesh* mesh;
    float radius = collider->get_radius();

    if (radius > 0)
    {
        return new btSphereShape(radius);
    }
    if ((owner->render_data() != nullptr) && ((mesh = owner->render_data()->mesh()) != nullptr))
    {
        BoundingVolume bv = mesh->getBoundingVolume();
        if (bv.radius() > 0)
        {
            return new btSphereShape(bv.radius());
        }
    }
    LOGE("PHYSICS: Sphere collider with zero volume");
    return nullptr;
}

btCollisionShape *convertCapsuleCollider2CollisionShape(CapsuleCollider *collider)
{
    btCollisionShape *shape = NULL;

    switch (collider->getDirection())
    {
        case CAPSULE_DIRECTION_Y:
            shape = new btCapsuleShape(collider->getRadius(), collider->getHeight());
            break;

        case CAPSULE_DIRECTION_X:
            shape = new btCapsuleShapeX(collider->getRadius(), collider->getHeight());
            break;

        case CAPSULE_DIRECTION_Z:
            shape = new btCapsuleShapeZ(collider->getRadius(), collider->getHeight());
            break;
    }
    return shape;
}

btCollisionShape* convertBoxCollider2CollisionShape(BoxCollider *collider)
{
    btCollisionShape *shape = NULL;
    Node* owner = collider->owner_object();
    btVector3 extents(collider->get_half_extents().x,
                      collider->get_half_extents().y,
                      collider->get_half_extents().z);
    Mesh* mesh;

    if (extents.length() > 0)
    {
        return new btBoxShape(extents);
    }
    if ((owner->render_data() != nullptr) && ((mesh = owner->render_data()->mesh()) != nullptr))
    {
        BoundingVolume bv = mesh->getBoundingVolume();
        if (bv.radius() > 0)
        {
            extents.setX((bv.max_corner().x - bv.min_corner().x) / 2.0f);
            extents.setY((bv.max_corner().y - bv.min_corner().y) / 2.0f);
            extents.setZ((bv.max_corner().z - bv.min_corner().z) / 2.0f);
            return new btBoxShape(extents);
        }
    }
    LOGE("PHYSICS: Box collider with zero volume");
    return nullptr;
}

btCollisionShape *convertMeshCollider2CollisionShape(MeshCollider *collider)
{
    btCollisionShape *shape = NULL;

    if (collider != NULL)
    {
        Mesh* mesh = collider->mesh();
        if (mesh == NULL)
        {
            Node* owner = collider->owner_object();
            if (owner == NULL)
            {
                return NULL;
            }
            RenderData* rdata = owner->render_data();
            if (rdata == NULL)
            {
                return NULL;
            }
            mesh = rdata->mesh();
            if (mesh == NULL)
            {
                return NULL;
            }
        }
        shape = createConvexHullShapeFromMesh(mesh);
    }
    return shape;
}

btConvexHullShape *createConvexHullShapeFromMesh(Mesh *mesh)
{
    btConvexHullShape *hull_shape = NULL;

    if (mesh != NULL)
    {
        btConvexHullShape *initial_hull_shape = NULL;
        btShapeHull *hull_shape_optimizer = NULL;
        unsigned short vertex_index;

        initial_hull_shape = new btConvexHullShape();
        mesh->getVertexBuffer()->forAllVertices("a_position", [initial_hull_shape](int iter, const float* v)
        {
            btVector3 vertex(v[0], v[1], v[2]);
            initial_hull_shape->addPoint(vertex);
        });

        btScalar margin(initial_hull_shape->getMargin());
        hull_shape_optimizer = new btShapeHull(initial_hull_shape);
        hull_shape_optimizer->buildHull(margin);

        hull_shape = new btConvexHullShape(
                (btScalar *) hull_shape_optimizer->getVertexPointer(),
                hull_shape_optimizer->numVertices());
    }
    else
    {
        LOGE("PHYSICS: createConvexHullShapeFromMesh(): NULL mesh object");
    }
    return hull_shape;
}

btTransform convertTransform2btTransform(Transform *t)
{
    glm::mat4 m;
    if (t->owner_object()->parent())
    {
        m = t->getModelMatrix(false);
    }
    else
    {
        m = t->getLocalModelMatrix();
    }
    return convertTransform2btTransform(m);
}

btTransform convertTransform2btTransform(const glm::mat4& m)
{
    glm::vec4 p(m[3]);
    glm::quat q = glm::quat_cast(m);
    btVector3 pos(p.x, p.y, p.z);
    btQuaternion rot(q.x, q.y, q.z, q.w);
    return btTransform(rot, pos);
}

}
