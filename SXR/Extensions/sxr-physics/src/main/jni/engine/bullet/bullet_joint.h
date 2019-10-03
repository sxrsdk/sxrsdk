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

#ifndef BULLET_JOINT_H_
#define BULLET_JOINT_H_

#include <string>
#include "glm/mat4x4.hpp"
#include "../physics_joint.h"

#include <BulletDynamics/Featherstone/btMultiBody.h>
#include <BulletDynamics/Featherstone/btMultiBodyLink.h>
#include <BulletDynamics/Featherstone/btMultiBodyLinkCollider.h>
#include <LinearMath/btMotionState.h>

class btDynamicsWorld;
class btMultiBodyJointMotor;
class btMultiBodyConstraint;

namespace sxr {

class Node;
class Skeleton;
class BulletWorld;
class BulletHingeConstraint;
class BulletSliderConstraint;
class BulletFixedConstraint;
class BulletGeneric6dofConstraint;

class BulletJoint : public PhysicsJoint
{
 public:
    BulletJoint(float mass, int numBones);

    BulletJoint(BulletJoint* parent, JointType type, int boneID, float mass);

    BulletJoint(btMultiBody* multibody);

    virtual ~BulletJoint();

    btMultiBody* getMultiBody() const { return mMultiBody; }

    btMultibodyLink* getLink() const { return mLink; }

    virtual void setMass(float mass);

    virtual float getMass() const;

    virtual JointType getJointType() const { return mJointType; }

    virtual PhysicsJoint* getParent() const;

    virtual Skeleton* getSkeleton();

    virtual const glm::vec3& getAxis() const { return mAxis; }

    virtual const glm::vec3& getPivot() const { return mPivot; }

	virtual void setPivot(const glm::vec3& pivot) { mPivot = pivot; }

    virtual void setAxis(const glm::vec3& axis) { mAxis = axis; }

    virtual float getFriction() const { return mLink ? mLink->m_jointFriction : 0; }

    virtual void setFriction(float f);

    virtual void applyTorque(float x, float y, float z);

    virtual void applyTorque(float t);

    int getBoneID() const { return mBoneID; }

    virtual void getWorldTransform(btTransform &worldTrans) const;

    virtual void setWorldTransform(const btTransform &worldTrans);

    virtual void updateConstructionInfo(PhysicsWorld* world);

    bool isReady() const;

    void updateWorldTransform();

    void getLocalTransform(const btTransform &worldTrans, glm::mat4 worldMatrices[], glm::mat4 localMatrices[]);

    void setCollisionProperties(int collisionGroup, int collidesWith);

protected:
    void setupSpherical();
    void setupHinge();
    void setupSlider();
    void setupFixed();
    void finalize();
    void destroy();
    void updateCollisionShapeLocalScaling();
    Skeleton* createSkeleton();

protected:
    BulletWorld*             mWorld;
    Skeleton*                mSkeleton;
    btMultiBodyLinkCollider* mCollider;
    btMultiBody*             mMultiBody;
    btMultibodyLink*         mLink;
    JointType                mJointType;
    glm::vec3                mAxis;
    glm::vec3                mPivot;
    int                      mBoneID;
    int                      mLinksAdded;
};

}

#endif /* BULLET_JOINT_H_ */
