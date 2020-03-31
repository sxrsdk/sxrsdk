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
class BulletRootJoint;

class BulletJoint : public PhysicsJoint
{
 public:
    BulletJoint(BulletJoint* parent, JointType type, int jointIndex, float mass);

    virtual ~BulletJoint() { }

    btMultiBody* getMultiBody() const { return mMultiBody; }

    btMultibodyLink* getLink() const { return &(mMultiBody->getLink(mJointIndex)); }

    virtual void setMass(float mass);

    virtual float getMass() const { return mMass; }

    virtual JointType getJointType() const { return mJointType; }

    virtual PhysicsJoint* getParent() const { return mParent; }

    virtual const glm::vec3& getAxis() const { return mAxis; }

    virtual const glm::vec3& getPivot() const { return mPivot; }

	virtual void setPivot(const glm::vec3& pivot) { mPivot = pivot; }

    virtual void setAxis(const glm::vec3& axis) { mAxis = axis; }

    virtual float getFriction() const;

    virtual void setFriction(float f);

    virtual void applyCentralForce(float x, float y, float z);

    virtual void applyTorque(float x, float y, float z);

    virtual void applyTorque(float t);

    int getJointIndex() const { return mJointIndex; }

    virtual void getWorldTransform(btTransform& t);

    virtual void setPhysicsTransform();

    virtual void getPhysicsTransform();

    virtual Skeleton* getSkeleton();

	virtual void updateConstructionInfo(PhysicsWorld* world);

    virtual BulletRootJoint* findRoot();

	void setCollisionProperties(int collisionGroup, int collidesWith);

protected:
    BulletJoint(float mass, int numBones);
    virtual void updateCollider(Node* owner);
    void setupSpherical();
    void setupHinge();
    void setupSlider();
    void setupFixed();

protected:
    BulletWorld*             mWorld;
    BulletJoint*             mParent;
    btMultiBodyLinkCollider* mCollider;
    btMultiBody*             mMultiBody;
    JointType                mJointType;
    glm::vec3                mAxis;
    glm::vec3                mPivot;
    int                      mJointIndex;
    float                    mMass;
};

class BulletRootJoint : public BulletJoint
{
public:
    BulletRootJoint(float mass, int numBones);

    BulletRootJoint(btMultiBody* multibody);

    virtual ~BulletRootJoint();

    virtual Skeleton* getSkeleton();

    virtual void updateConstructionInfo(PhysicsWorld* world);

    virtual void setMass(float mass);

    virtual void applyCentralForce(float x, float y, float z);

    virtual void applyTorque(float x, float y, float z);

    virtual void applyTorque(float t);

    void setPhysicsTransforms();

    void getPhysicsTransforms();

    virtual void setPhysicsTransform();

    virtual BulletRootJoint* findRoot();

	bool addLink(PhysicsJoint* joint, PhysicsWorld* world);

protected:
	virtual void updateCollider(Node* owner);
    void finalize();
    void destroy();
    Skeleton* createSkeleton();

protected:
    std::vector<BulletJoint*> mJoints;
    Skeleton*   mSkeleton;
    int         mNumJoints;
    int         mLinksAdded;
};
}

#endif /* BULLET_JOINT_H_ */
