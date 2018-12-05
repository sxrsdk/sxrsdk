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

#ifndef FRAMEWORK_BULLET_SXR_UTILS_H
#define FRAMEWORK_BULLET_SXR_UTILS_H

#include "../physics_common.h"

#include "objects/components/sphere_collider.h"
#include "objects/components/box_collider.h"
#include "objects/components/mesh_collider.h"
#include "objects/components/capsule_collider.h"

#include <BulletCollision/CollisionShapes/btSphereShape.h>
#include <BulletCollision/CollisionShapes/btBoxShape.h>
#include <BulletCollision/CollisionShapes/btConvexHullShape.h>
#include <BulletDynamics/Dynamics/btRigidBody.h>

namespace sxr {
    btCollisionShape *convertCollider2CollisionShape(Collider *collider);

    btCollisionShape *convertSphereCollider2CollisionShape(SphereCollider *collider);

    btCollisionShape *convertCapsuleCollider2CollisionShape(CapsuleCollider *collider);

    btCollisionShape *convertBoxCollider2CollisionShape(BoxCollider *collider);

    btCollisionShape *convertMeshCollider2CollisionShape(MeshCollider *collider);

    btConvexHullShape *createConvexHullShapeFromMesh(Mesh *mesh);

    btTransform convertTransform2btTransform(const Transform *t);

    void convertBtTransform2Transform(btTransform bulletTransform, Transform *transform);

    inline btVector3 Common2Bullet(PhysicsVec3 const &pv) {
        return btVector3(pv.x, pv.y, pv.z);
    }

    inline btQuaternion Common2Bullet(PhysicsQuat const &quat) {
        return btQuaternion(quat.x, quat.y, quat.z, quat.w);
    }
}

#endif //FRAMEWORK_BULLET_SXR_UTILS_H
