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

    BulletJoint::BulletJoint(float mass, int numJoints)
    : PhysicsJoint(mass, numJoints),
      mMultiBody(nullptr),
      mCollider(nullptr),
      mParent(nullptr),
      mJointIndex(-1),
      mAxis(1, 0, 0),
      mPivot(0, 0, 0),
      mWorld(nullptr),
      mMass(mass),
      mJointType(JointType::sphericalJoint)
    {
    }

    BulletJoint::BulletJoint(BulletJoint* parent, JointType jointType, int jointIndex, float mass)
    : PhysicsJoint(parent, jointType, jointIndex, mass),
      mMultiBody(nullptr),
      mParent(parent),
      mCollider(nullptr),
      mJointIndex(jointIndex - 1),
      mAxis(1, 0, 0),
      mMass(mass),
      mPivot(0, 0, 0),
      mJointType(jointType),
      mWorld(nullptr)
    {
    }

    BulletRootJoint* BulletJoint::findRoot()
    {
        return mParent->findRoot();
    }

    void BulletJoint::setMass(float mass)
    {
        mMass = mass;
        if (mMultiBody)
        {
            mMultiBody->getLink(mJointIndex).m_mass = btScalar(mass);
        }
    }

    float  BulletJoint::getFriction() const
    {
        if (mMultiBody)
        {
            return mMultiBody->getLink(mJointIndex).m_jointFriction;
        }
        return 0;
    }

    void BulletJoint::setFriction(float friction)
    {
        if (mMultiBody)
        {
            mMultiBody->getLink(mJointIndex).m_jointFriction = btScalar(friction);
        }
    }


    Skeleton* BulletJoint::getSkeleton()
    {
        BulletRootJoint* root = findRoot();
        return root ? root->getSkeleton() : nullptr;
    }

    void BulletJoint::getWorldTransform(btTransform& t)
    {
        Node* owner = owner_object();
        Transform* trans = owner->transform();
        t = convertTransform2btTransform(trans);
    }

    void BulletJoint::setPhysicsTransform()
    {
        if (mCollider)
        {
            Node* owner = owner_object();
            Transform* trans = owner->transform();
            btTransform t = convertTransform2btTransform(trans);
            btVector3 pos = t.getOrigin();

            LOGE("BULLET: UPDATE %s, %f, %f, %f", owner->name().c_str(), pos.getX(), pos.getY(), pos.getZ());
            mCollider->setWorldTransform(t);
        }
    }

    void BulletJoint::getPhysicsTransform()
    {
        if (mCollider == nullptr)
        {
            return;
        }
        Node* owner = owner_object();
        btTransform t = mCollider->getWorldTransform();
        btVector3 pos = t.getOrigin();
        btQuaternion rot = t.getRotation();
        Node* parent = owner->parent();
        float matrixData[16];

        t.getOpenGLMatrix(matrixData);
        glm::mat4 worldMatrix(glm::make_mat4(matrixData));
        Transform* trans = owner->transform();
        if ((parent != nullptr) && (parent->parent() != nullptr))
        {
            glm::mat4 parentWorld(parent->transform()->getModelMatrix(false));
            glm::mat4 parentInverseWorld(glm::inverse(parentWorld));
            glm::mat4 localMatrix;

            localMatrix = parentInverseWorld * worldMatrix;
            trans->setModelMatrix(localMatrix);
        }
        else
        {
            trans->set_position(pos.getX(), pos.getY(), pos.getZ());
            trans->set_rotation(rot.getW(), rot.getX(), rot.getY(), rot.getZ());
        }
        LOGD("BULLET: JOINT %s %f, %f, %f", owner->name().c_str(), trans->position_x(), trans->position_y(), trans->position_z());
    }

    void BulletJoint::applyCentralForce(float x, float y, float z)
    {
        btVector3 force(x, y, z);
        if (mMultiBody)
        {
            mMultiBody->addLinkForce(getJointIndex(), force);
        }
    }

    void BulletJoint::applyTorque(float x, float y, float z)
    {
        if (mMultiBody)
        {
            float torque[] = { x, y, z, 0 };
            mMultiBody->addJointTorqueMultiDof(mJointIndex, torque);
            mMultiBody->addLinkTorque(mJointIndex, btVector3(x, y, z));
        }
    }

    void BulletJoint::applyTorque(float t)
    {
        if (mMultiBody)
        {
            btVector3 torque(t, 0, 0);
            mMultiBody->addBaseTorque(torque);
        }
    }

    void BulletJoint::updateConstructionInfo(PhysicsWorld* world)
    {
        Node* owner = owner_object();
        BulletJoint* parent = static_cast<BulletJoint*>(getParent());
        const char* name = owner->name().c_str();
        mWorld = static_cast<BulletWorld*>(world);
        mMultiBody = parent->getMultiBody();
        btMultibodyLink& link = mMultiBody->getLink(mJointIndex);

        link.m_linkName = name;
        link.m_jointName = name;
        updateCollider(owner);
        switch (mJointType)
        {
            case JointType::fixedJoint: setupFixed(); break;
            case JointType::prismaticJoint: setupSlider(); break;
            case JointType::revoluteJoint: setupHinge(); break;
            default: setupSpherical(); break;
        }
        setPhysicsTransform();
    }

    void BulletJoint::updateCollider(Node* owner)
    {
        btVector3 localInertia;
        btMultibodyLink& link = mMultiBody->getLink(mJointIndex);
        if (mCollider == nullptr)
        {
            Collider* collider = static_cast<Collider*>(owner->getComponent(COMPONENT_TYPE_COLLIDER));
            if (collider)
            {
                mCollider = new btMultiBodyLinkCollider(mMultiBody, mJointIndex);
                btCollisionShape* shape = convertCollider2CollisionShape(collider);
                btVector3 ownerScale;
                Transform* trans = owner->transform();

                mCollider->setCollisionShape(shape);
                mCollider->setIslandTag(0);
                mCollider->m_link = getJointIndex();
                ownerScale.setValue(trans->scale_x(), trans->scale_y(), trans->scale_z());
                mCollider->getCollisionShape()->setLocalScaling(ownerScale);
                shape->calculateLocalInertia(getMass(), localInertia);
                link.m_inertiaLocal = localInertia;
                link.m_collider = mCollider;
                mCollider->setUserPointer(this);
                mWorld->getPhysicsWorld()->addCollisionObject(mCollider);
            }
            else
            {
                LOGE("PHYSICS: joint %s does not have collider", owner_object()->name().c_str());
            }
        }
    }

    void BulletJoint::setCollisionProperties(int collisionGroup, int collidesWith)
    {
        if (mCollider)
        {
            mCollider->getBroadphaseHandle()->m_collisionFilterGroup = collisionGroup;
            mCollider->getBroadphaseHandle()->m_collisionFilterMask = collidesWith;
        }
    }

    void BulletJoint::setupFixed()
    {
        BulletJoint*       jointA = static_cast<BulletJoint*>(getParent());
        btVector3          pivotB(mPivot.x, mPivot.y, mPivot.z);
        btTransform        worldA;  jointA->getWorldTransform(worldA);
        btTransform        worldB; getWorldTransform(worldB);
        btQuaternion       rot(worldA.getRotation());
        btVector3          bodyACOM(worldA.getOrigin());
        btVector3          bodyBCOM(worldB.getOrigin());
        btVector3          diffCOM = bodyBCOM + pivotB - bodyACOM;
        btMultibodyLink&   link = mMultiBody->getLink(getJointIndex());

        mMultiBody->setupFixed(getJointIndex(),
                               link.m_mass,
                               link.m_inertiaLocal,
                               jointA->getJointIndex(),
                               rot,
                               diffCOM,
                               -pivotB,
                               true);
    }

    void BulletJoint::setupSpherical()
    {
        BulletJoint*       jointA = static_cast<BulletJoint*>(getParent());
        btVector3          pivotB(mPivot.x, mPivot.y, mPivot.z);
        btTransform        worldA;  jointA->getWorldTransform(worldA);
        btTransform        worldB; getWorldTransform(worldB);
        btQuaternion       rotA(worldA.getRotation());
        btVector3          bodyACOM(worldA.getOrigin());
        btVector3          bodyBCOM(worldB.getOrigin());
        btVector3          diffCOM = bodyBCOM + pivotB - bodyACOM;
        btMultibodyLink&   link = mMultiBody->getLink(getJointIndex());

        mMultiBody->setupSpherical(getJointIndex(),
                                   link.m_mass,
                                   link.m_inertiaLocal,
                                   jointA->getJointIndex(),
                                   rotA,
                                   diffCOM,
                                   -pivotB, true);
    }

    /***
     * The hinge joint is set up by choosing a hinge axis in the hinge coordinate system.
     * Below we choose the X axis. To map the SXR world coordinate system into the
     * hinge coordinate system we define a rotation frame with the hinge axis as X,
     * the vector between bodyB and its pivot as Y (up axis) and the remaining
     * axis is the cross between the two (normal to the plane defined by hinge
     * and pivot axes). This rotation (in quaternion form) is the rotParentToThis
     * argument to setupRevolute.
     *
     * The vector from bodyB's center to bodyB's pivot is supplied as the
     * bodyB pivot from the constraint (getPivot()). This vector is the
     * value for thisPivotToThisComOffset in setupRevolute.
     *
     * The parentComToThisPivotOffset argument is the difference between
     * bodyB center and bodyA center plus the bodyB pivot (the vector
     * from bodyA center to bodyB's pivot).
      */
    void BulletJoint::setupHinge()
    {
        BulletJoint*        jointA = static_cast<BulletJoint*>(getParent());
        btVector3           pivotB(mPivot.x, mPivot.y, mPivot.z);
        btTransform         worldA; jointA->getWorldTransform(worldA);
        btTransform         worldB; getWorldTransform(worldB);
        btQuaternion        rotA(worldA.getRotation());
        btVector3           bodyACOM(worldA.getOrigin());
        btVector3           bodyBCOM(worldB.getOrigin());
        btVector3           diffCOM = bodyBCOM + pivotB - bodyACOM;
        btVector3           hingeAxis(mAxis.x, mAxis.y, mAxis.z);
        btMultibodyLink&   link = mMultiBody->getLink(getJointIndex());

        mMultiBody->setupRevolute(getJointIndex(),
                          link.m_mass,
                          link.m_inertiaLocal,
                          jointA->getJointIndex(),
                          rotA,
                          hingeAxis.normalized(),
                          diffCOM,
                          -pivotB, true);
    }

    void BulletJoint::setupSlider()
    {
        BulletJoint*        jointA = static_cast<BulletJoint*>(getParent());
        btVector3           pivotB(mPivot.x, mPivot.y, mPivot.z);
        btTransform         worldA; jointA->getWorldTransform(worldA);
        btTransform         worldB; getWorldTransform(worldB);
        btQuaternion        rotA(worldA.getRotation());
        btVector3           bodyACOM(worldA.getOrigin());
        btVector3           bodyBCOM(worldB.getOrigin());
        btVector3           diffCOM = bodyBCOM + pivotB - bodyACOM;
        btVector3           sliderAxis(bodyBCOM - bodyACOM);
        btMultibodyLink&    link = mMultiBody->getLink(getJointIndex());

        mMultiBody->setupPrismatic(getJointIndex(),
                                  link.m_mass,
                                  link.m_inertiaLocal,
                                  jointA->getJointIndex(),
                                  rotA,
                                  sliderAxis.normalized(),
                                  diffCOM,
                                  -pivotB,
                                  true);
    }

}
