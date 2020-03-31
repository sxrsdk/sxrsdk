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
#include <math.h>
#include "glm/glm.hpp"
#include "glm/gtc/matrix_inverse.hpp"
#include "glm/gtx/quaternion.hpp"
#include "glm/mat4x4.hpp"
#include "glm/gtc/type_ptr.hpp"

#include "objects/node.h"
#include "objects/components/skeleton.h"
#include "objects/components/component_types.h"
#include "objects/components/transform.h"
#include "objects/components/collider.h"
#include "bullet_world.h"
#include "bullet_joint.h"
#include "bullet_hingeconstraint.h"
#include "bullet_sliderconstraint.h"
#include "bullet_fixedconstraint.h"
#include "bullet_generic6dofconstraint.h"
#include "bullet_jointmotor.h"
#include "bullet_sxr_utils.h"
#include "util/sxr_log.h"

#include "BulletDynamics/Featherstone/btMultiBodyDynamicsWorld.h"
#include "BulletDynamics/Featherstone/btMultiBodyJointMotor.h"
#include "BulletDynamics/Featherstone/btMultiBody.h"
#include "BulletDynamics/Featherstone/btMultiBodyLink.h"
#include "BulletDynamics/Featherstone/btMultiBodyLinkCollider.h"

#include "BulletCollision/CollisionDispatch/btCollisionObject.h"
#include "BulletCollision/CollisionShapes/btEmptyShape.h"
#include "BulletCollision/BroadphaseCollision/btBroadphaseProxy.h"
#include "LinearMath/btDefaultMotionState.h"
#include "LinearMath/btScalar.h"
#include "LinearMath/btVector3.h"
#include "LinearMath/btTransform.h"
#include "LinearMath/btMatrix3x3.h"


namespace sxr {

    BulletRootJoint::BulletRootJoint(float mass, int numJoints)
    : BulletJoint(mass, numJoints),
      mNumJoints(numJoints - 1),
      mLinksAdded(0)
    {
        mJointType = JointType::baseJoint;
        mJoints.reserve(mNumJoints);
        mJoints.resize(mNumJoints);
    }

    BulletRootJoint::BulletRootJoint(btMultiBody* multiBody)
    : BulletJoint(multiBody->getNumLinks(), multiBody->getBaseMass()),
      mLinksAdded(0)
    {
        mMultiBody = multiBody;
        mJoints.reserve(multiBody->getNumLinks());
        mJoints.resize(multiBody->getNumLinks());
        mMultiBody->setUserPointer(this);
    }

    BulletRootJoint::~BulletRootJoint()
    {
        destroy();
    }

    BulletRootJoint* BulletRootJoint::findRoot()
    {
        return this;
    }

    Skeleton* BulletRootJoint::getSkeleton()
    {
        Skeleton* skel = static_cast<Skeleton*>(owner_object()->getComponent(COMPONENT_TYPE_SKELETON));
        if (skel == nullptr)
        {
            return  createSkeleton();
        }
        return skel;
    }

    void BulletRootJoint::setMass(float mass)
    {
        mMass = mass;
        if (mMultiBody)
        {
            mMultiBody->setBaseMass(btScalar(mass));
        }
    }


    void BulletRootJoint::destroy()
    {
        if (mMultiBody != nullptr)
        {
            int numLinks = mMultiBody->getNumLinks();
            for (int i = 0; i < numLinks; ++i)
            {
                btMultibodyLink& link = mMultiBody->getLink(i);
                if (link.m_collider)
                {
                    delete link.m_collider;
                    link.m_collider = nullptr;
                }
            }
            delete mMultiBody;
            mMultiBody = nullptr;
        }
    }

    void BulletRootJoint::setPhysicsTransform()
    {
        Node* owner = owner_object();
        Transform* trans = owner->transform();
        btTransform  t = convertTransform2btTransform(trans);
        btVector3 pos = t.getOrigin();

        LOGE("BULLET ROOT JOINT: UPDATE %s, %f, %f, %f", owner->name().c_str(), pos.x(), pos.y(), pos.z());
        if (mMultiBody)
        {
            mMultiBody->setBaseWorldTransform(t);
        }
        if (mCollider)
        {
            mCollider->setWorldTransform(t);
        }
    }

    void BulletRootJoint::getPhysicsTransforms()
    {
        if (mMultiBody)
        {
            getPhysicsTransform();
            for (int j = 0; j < mNumJoints; ++j)
            {
                BulletJoint* joint = mJoints.at(j);
                joint->getPhysicsTransform();
            }
        }
    }

    void BulletRootJoint::setPhysicsTransforms()
    {
        if (mMultiBody)
        {
            setPhysicsTransform();
            for (int j = 0; j < mNumJoints; ++j)
            {
                BulletJoint* joint = mJoints[j];
                joint->setPhysicsTransform();
            }
        }
    }


    Skeleton* BulletRootJoint::createSkeleton()
    {
        int numbones = mMultiBody->getNumLinks() + 1;
        int boneParents[numbones];
        const char* boneNames[numbones];
        Skeleton* skel = static_cast<Skeleton*>(owner_object()->getComponent(COMPONENT_TYPE_SKELETON));

        if (skel != nullptr)
        {
            return skel;
        }
        boneParents[0] = -1;
        boneNames[0] = owner_object()->name().c_str();
        for (int i = 1; i < numbones; ++i)
        {
            btMultibodyLink& link = mMultiBody->getLink(i - 1);
            const BulletJoint* j = reinterpret_cast<const BulletJoint*>(link.m_userPtr);
            Node* owner = j->owner_object();
            boneNames[i] = owner->name().c_str();
            boneParents[i] = link.m_parent + 1;
        }
        skel = new Skeleton(boneParents, numbones);
        skel->updateBones(boneParents, boneNames, numbones);
        owner_object()->attachComponent(skel);
        return skel;
    }

    void BulletRootJoint::applyCentralForce(float x, float y, float z)
    {
        btVector3 force(x, y, z);
        if (mMultiBody)
        {
            mMultiBody->addBaseForce(force);
        }
    }

    void BulletRootJoint::applyTorque(float x, float y, float z)
    {
        if (mMultiBody)
        {
            btVector3 torque(x, y, z);
            mMultiBody->addBaseTorque(torque);
        }
    }

    void BulletRootJoint::applyTorque(float t)
    {
        if (mMultiBody)
        {
            btVector3 torque(t, 0, 0);
            mMultiBody->addBaseTorque(torque);
        }
    }


    void BulletRootJoint::updateConstructionInfo(PhysicsWorld* world)
    {
        Node* owner = owner_object();
        mWorld = static_cast<BulletWorld*>(world);
        updateCollider(owner);
        mMultiBody->setBaseName(owner->name().c_str());
        setPhysicsTransform();
    }

    void BulletRootJoint::updateCollider(Node* owner)
    {
        btVector3 localInertia;
        if (mCollider == nullptr)
        {
            Collider* collider = (Collider*) owner->getComponent(COMPONENT_TYPE_COLLIDER);
            if (collider)
            {
                mCollider = new btMultiBodyLinkCollider(mMultiBody, -1);
                btCollisionShape* shape = convertCollider2CollisionShape(collider);
                btVector3 ownerScale;
                Transform* trans = owner->transform();

                mCollider->setCollisionShape(shape);
                mCollider->setIslandTag(0);
                ownerScale.setValue(trans->scale_x(),
                                    trans->scale_y(),
                                    trans->scale_z());
                mCollider->getCollisionShape()->setLocalScaling(ownerScale);
                shape->calculateLocalInertia(getMass(), localInertia);
                mCollider->setUserPointer(this);
                mMultiBody->setBaseCollider(mCollider);
                mMultiBody->setBaseInertia(localInertia);
                mWorld->getPhysicsWorld()->addCollisionObject(mCollider);
            }
            else
            {
                LOGE("PHYSICS: joint %s does not have collider", owner_object()->name().c_str());
            }
        }
    }

    void BulletRootJoint::finalize()
    {
        mMultiBody = new btMultiBody(mNumJoints, mMass, btVector3(0, 0, 0), (mMass == 0), false);
        mMultiBody->setUserPointer(this);
        mMultiBody->setCanSleep(false);
        mMultiBody->setHasSelfCollision(false);
        updateConstructionInfo(mWorld);
        for (int i = 0; i < mNumJoints; ++i)
        {
            BulletJoint* joint = mJoints[i];
            if (joint != nullptr)
            {
                joint->updateConstructionInfo(mWorld);
            }
        }
        mMultiBody->finalizeMultiDof();
        dynamic_cast<btMultiBodyDynamicsWorld *>(mWorld->getPhysicsWorld())->addMultiBody(mMultiBody);
    }

    bool BulletRootJoint::addLink(PhysicsJoint* joint, PhysicsWorld* world)
    {
        mWorld = static_cast<BulletWorld*>(world);
        if (joint == this)
        {
            if (mNumJoints == 0)
            {
                finalize();
                return true;
            }
            return false;
        }
        int linkIndex = joint->getJointIndex();
        mJoints[linkIndex] = static_cast<BulletJoint*>(joint);
        if ((++mLinksAdded == mNumJoints) && (mMultiBody == nullptr))
        {
            finalize();
            return true;
        }
        return false;
    }

}
