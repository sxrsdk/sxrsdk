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
 * Bullet implementation of 3D world
 ***************************************************************************/

#ifndef BULLET_WORLD_H_
#define BULLET_WORLD_H_

#include "../physics_world.h"
#include "../physics_collidable.h"

#include <utility>
#include <map>
#include <BulletDynamics/ConstraintSolver/btPoint2PointConstraint.h>

class btDynamicsWorld;
class btCollisionConfiguration;
class btCollisionDispatcher;
class btConstraintSolver;
class btBroadphaseInterface;

namespace sxr {

class PhysicsConstraint;
class PhysicsRigidBody;
class BulletJoint;


class BulletWorld : public PhysicsWorld {
 public:
    BulletWorld(bool isMultiBody);

    virtual ~BulletWorld();

    bool isMultiBody();

    void addConstraint(PhysicsConstraint *constraint);

    void removeConstraint(PhysicsConstraint *constraint);

    void startDrag(Node *pivot_obj, PhysicsRigidBody *target,
                   float relx, float rely, float relz);

    void stopDrag();

    void addRigidBody(PhysicsRigidBody *body);

    void addRigidBody(PhysicsRigidBody *body, int collisiontype, int collidesWith);

    void removeRigidBody(PhysicsRigidBody *body);

    void addJoint(PhysicsJoint *joint);

    void removeJoint(PhysicsJoint *body);

    void step(float timeStep, int maxSubSteps);

    void listCollisions(std::list <ContactPoint> &contactPoints);

    int getUpdated(std::vector<PhysicsCollidable*>& bodies);

    void setGravity(float x, float y, float z);

    void setGravity(glm::vec3 gravity);

    void markUpdated(PhysicsCollidable* body);

    const glm::vec3& getGravity() const;

    btDynamicsWorld* getPhysicsWorld() const;

 private:
    void initialize(bool isMultiBody);

    void finalize();

    void setPhysicsTransforms();

    void getPhysicsTransforms();

 private:
    std::map<std::pair <long,long>, ContactPoint> prevCollisions;
    btDynamicsWorld* mPhysicsWorld;
    btCollisionConfiguration* mCollisionConfiguration;
    btCollisionDispatcher* mDispatcher;
    btConstraintSolver* mSolver;
    btBroadphaseInterface* mOverlappingPairCache;
    btPoint2PointConstraint* mDraggingConstraint;
    Node *mPivotObject;
    int mActivationState;
    bool mIsMultiBody;
    mutable glm::vec3 mGravity;
    std::vector<PhysicsCollidable*> mBodiesChanged;
    std::vector<BulletJoint*> mMultiBodies;
};

}

#endif /* BULLET_WORLD_H_ */
