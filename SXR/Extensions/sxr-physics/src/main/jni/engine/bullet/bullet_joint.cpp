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
#include <glm/gtx/quaternion.hpp>
#include <glm/mat4x4.hpp>
#include <contrib/glm/gtc/type_ptr.hpp>

#include "objects/node.h"
#include "objects/components/component_types.h"
#include "objects/components/transform.h"
#include "objects/components/collider.h"
#include "bullet_world.h"
#include "bullet_joint.h"
#include "bullet_hingeconstraint.h"
#include "bullet_sliderconstraint.h"
#include "bullet_fixedconstraint.h"
#include "bullet_generic6dofconstraint.h"
#include "bullet_sxr_utils.h"
#include "util/sxr_log.h"

#include "BulletDynamics/Featherstone/btMultiBodyDynamicsWorld.h"
#include "BulletCollision/CollisionDispatch/btCollisionObject.h"
#include "BulletCollision/CollisionShapes/btEmptyShape.h"
#include "BulletCollision/BroadphaseCollision/btBroadphaseProxy.h"
#include "LinearMath/btDefaultMotionState.h"
#include "LinearMath/btScalar.h"
#include "LinearMath/btVector3.h"
#include "LinearMath/btTransform.h"


namespace sxr {

    BulletJoint::BulletJoint(float mass, int numBones)
            : PhysicsJoint(mass, numBones),
              mMultiBody(nullptr),
              mCollider(nullptr),
              mLink(nullptr),
              mBoneID(0),
              mConstraintsAdded(0)
    {
        mMultiBody = new btMultiBody(numBones - 1, mass, btVector3(0, 0, 0), (mass == 0), false);
        mMultiBody->setUserPointer(this);
        mMultiBody->setBaseMass(mass);
        mMultiBody->setCanSleep(false);
        mMultiBody->setHasSelfCollision(false);
        mWorld = nullptr;
    }

    BulletJoint::BulletJoint(BulletJoint* parent, int boneID, float mass)
            : PhysicsJoint(parent, boneID, mass),
              mMultiBody(nullptr),
              mCollider(nullptr),
              mBoneID(boneID),
              mConstraintsAdded(0)
    {
        mMultiBody = parent->getMultiBody();
        btMultibodyLink& link = mMultiBody->getLink(boneID - 1);
        link.m_mass = mass;
        link.m_parent = parent->getBoneID();
        link.m_userPtr = this;
        mLink = &link;
        mWorld = nullptr;
    }

    BulletJoint::BulletJoint(btMultiBody* multiBody)
            : PhysicsJoint(multiBody->getNumLinks(), multiBody->getBaseMass()),
              mMultiBody(multiBody),
              mLink(nullptr),
              mLinksAdded(0),
              mConstraintsAdded(0)
    {
        mMultiBody->setUserPointer(this);
        mWorld = nullptr;
    }

    BulletJoint::BulletJoint(btMultibodyLink* link)
            : PhysicsJoint(link->m_mass, 0),
              mMultiBody(nullptr),
              mLink(link),
              mLinksAdded(0),
              mConstraintsAdded(0)
    {
        link->m_userPtr = this;
        mWorld = nullptr;
    }

    BulletJoint::~BulletJoint()
    {
        destroy();
    }

    void BulletJoint::setMass(float mass)
    {
        if (mLink != nullptr)
        {
            mLink->m_mass = btScalar(mass);
        }
        else
        {
            mMultiBody->setBaseMass(btScalar(mass));
        }
    }

    void BulletJoint::setFriction(float friction)
    {
        if (mLink != nullptr)
        {
            mLink->m_jointFriction = btScalar(friction);
        }
    }

    float BulletJoint::getMass() const
    {
        return mLink ? mLink->m_mass :  mMultiBody->getBaseMass();
    }

    void BulletJoint::destroy()
    {
        if (mMultiBody != nullptr)
        {
            if (mLink != nullptr)
            {
                if (mCollider != nullptr)
                {
                    mLink->m_collider = nullptr;
                    delete mCollider;
                }
                mLink = nullptr;
            }
            else if (mCollider != nullptr)
            {
                mMultiBody->setBaseCollider(nullptr);
                delete mCollider;
            }
            delete mMultiBody;
            mMultiBody = nullptr;
        }
    }

    void BulletJoint::getWorldTransform(btTransform& centerOfMassWorldTrans) const
    {
        Transform* trans = owner_object()->transform();
        centerOfMassWorldTrans = convertTransform2btTransform(trans);
    }

    void BulletJoint::updateWorldTransform()
    {
        Node* owner = owner_object();
        Transform* trans = owner->transform();
        btTransform t  = convertTransform2btTransform(trans);
        btVector3 pos = t.getOrigin();

        LOGE("BULLET: UPDATE %s, %f, %f, %f", owner->name().c_str(), pos.getX(), pos.getY(), pos.getZ());
        if (mLink == nullptr)
        {
            mMultiBody->setBaseWorldTransform(t);
        }
        else if (mCollider)
        {
            mCollider->setWorldTransform(t);
        }
    }

    void BulletJoint::setWorldTransform(const btTransform& centerOfMassWorldTrans)
    {
        Node* owner = owner_object();
        Transform* trans = owner->transform();
        btTransform aux; getWorldTransform(aux);
        btTransform physicBody = centerOfMassWorldTrans;
        btVector3 pos = physicBody.getOrigin();
        btQuaternion rot = physicBody.getRotation();
        Node* parent = owner->parent();
        float matrixData[16];

        centerOfMassWorldTrans.getOpenGLMatrix(matrixData);
        glm::mat4 worldMatrix(glm::make_mat4(matrixData));
        if ((parent != nullptr) && (parent->parent() != nullptr))
        {
            glm::mat4 parentWorld(parent->transform()->getModelMatrix(true));
            glm::mat4 parentInverseWorld(glm::inverse(parentWorld));
            glm::mat4 localMatrix;

            localMatrix = parentInverseWorld * worldMatrix;
            trans->setModelMatrix(localMatrix);
            LOGD("BULLET: JOINT %s %f, %f, %f", owner->name().c_str(), trans->position_x(), trans->position_y(), trans->position_z());
        }
        else
        {
            trans->set_position(pos.getX(), pos.getY(), pos.getZ());
            trans->set_rotation(rot.getW(), rot.getX(), rot.getY(), rot.getZ());
        }
        mWorld->markUpdated(this);
    }


    void BulletJoint::applyTorque(float x, float y, float z)
    {
        if (mLink)
        {
            //mMultiBody->addLinkTorque(getBoneID() - 1, btVector3(x, y, z));
            mMultiBody->addJointTorque(getBoneID() - 1, x);
        }
        else
        {
            mMultiBody->addBaseTorque(btVector3(x, y, z));
        }
    }

    void BulletJoint::applyCentralForce(float x, float y, float z)
    {
        if (mLink)
        {
            mMultiBody->addLinkForce(getBoneID() - 1, btVector3(x, y, z));
        }
        else
        {
            mMultiBody->addBaseForce(btVector3(x, y, z));
        }
    }

    void  BulletJoint::updateCollisionShapeLocalScaling()
    {
        btVector3 ownerScale;
        Node* owner = owner_object();
        if (owner)
        {
            Transform* trans = owner->transform();
            ownerScale.setValue(trans->scale_x(),
                                trans->scale_y(),
                                trans->scale_z());
        }
        else
        {
            ownerScale.setValue(1.0f, 1.0f, 1.0f);
        }
        mCollider->getCollisionShape()->setLocalScaling(ownerScale);
    }

    void BulletJoint::updateConstructionInfo(PhysicsWorld* world)
    {
        Node* owner = owner_object();
        mWorld = static_cast<BulletWorld*>(world);
        btVector3 localInertia;
        if (mCollider == nullptr)
        {
            Collider* collider = (Collider*) owner->getComponent(COMPONENT_TYPE_COLLIDER);
            if (collider)
            {
                mCollider = new btMultiBodyLinkCollider(mMultiBody, mBoneID);
                btCollisionShape* shape = convertCollider2CollisionShape(collider);
                bool isDynamic = getMass() > 0;
                int collisionFilterGroup = isDynamic ? int(btBroadphaseProxy::DefaultFilter) : int(btBroadphaseProxy::StaticFilter);
                int collisionFilterMask = isDynamic ? int(btBroadphaseProxy::AllFilter) : int(btBroadphaseProxy::AllFilter ^ btBroadphaseProxy::StaticFilter);

                mCollider->setCollisionShape(shape);
                mCollider->setIslandTag(0);
                mCollider->m_link = getBoneID() - 1;
                updateCollisionShapeLocalScaling();
                shape->calculateLocalInertia(getMass(), localInertia);

                if (mLink == nullptr)
                {
                    mMultiBody->setBaseCollider(mCollider);
                    mMultiBody->setBaseInertia(localInertia);
                }
                else
                {
                    mLink->m_inertiaLocal = localInertia;
                    mLink->m_collider = mCollider;
                }
                mCollider->setUserPointer(this);
                mWorld->getPhysicsWorld()->addCollisionObject(mCollider);
            }
            else
            {
                LOGE("PHYSICS: joint %s does not have collider", owner_object()->name().c_str());
            }
            if (mLink)
            {
                mLink->m_linkName = owner->name().c_str();
                mLink->m_jointName = owner->name().c_str();
                BulletJoint* root = static_cast<BulletJoint*> (mMultiBody->getUserPointer());

                ++(root->mLinksAdded);
            }
            else
            {
                mMultiBody->setBaseName(owner->name().c_str());
            }
        }
        updateWorldTransform();
    }

    void BulletJoint::setupFixed(BulletFixedConstraint* constraint)
    {
        Node* owner = owner_object();
        Node* parent = owner->parent();
        BulletJoint* jointA = static_cast<BulletJoint*>(parent->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        glm::mat4 tA = parent->transform()->getModelMatrix(true);
        glm::mat4 tB = owner->transform()->getModelMatrix(true);
        glm::quat rotA(glm::quat_cast(tA));
        glm::quat rotB(glm::quat_cast(tB));
        glm::quat rotDiff(glm::inverse(rotA) * rotB);
        btVector3 bodyACOM(tA[3][0], tA[3][1], tA[3][2]);
        btVector3 bodyBCOM(tB[3][0], tB[3][1], tB[3][2]);
        btVector3 diffCOM = bodyBCOM - bodyACOM;
        btVector3 bodyACOM2bodyBpivot = diffCOM;

        mMultiBody->setupFixed(getBoneID() - 1,
                                   mLink->m_mass,
                                   mLink->m_inertiaLocal,
                                   jointA->getBoneID() - 1,
                                   btQuaternion(rotDiff.x, rotDiff.y, rotDiff.z, rotDiff.w),
                                   bodyACOM2bodyBpivot,
                                   btVector3(0,0, 0),
                                   true);
        addConstraint();
    }


    void BulletJoint::setupSpherical(BulletGeneric6dofConstraint* constraint)
    {
        Node* owner = owner_object();
        Node* parent = owner->parent();
        BulletJoint* jointA = static_cast<BulletJoint*>(parent->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        glm::mat4 tA = parent->transform()->getModelMatrix(true);
        glm::mat4 tB = owner->transform()->getModelMatrix(true);
        glm::vec3 pivotA(0, 0, 0);
        glm::quat rotA(glm::quat_cast(tA));
        glm::quat rotB(glm::quat_cast(tB));
        glm::quat rotDiff(glm::inverse(rotA) * rotB);
        btVector3 pA(pivotA.x, pivotA.y, pivotA.z);
        btVector3 bodyACOM(tA[3][0], tA[3][1], tA[3][2]);
        btVector3 bodyBCOM(tB[3][0], tB[3][1], tB[3][2]);
        btVector3 bodyACOM2bodyBpivot(pA - bodyBCOM);
        btVector3 bodyBpivot(bodyACOM + bodyACOM2bodyBpivot);
        btMultibodyLink& link = mMultiBody->getLink(getBoneID() - 1);

        if (constraint)
        {
            pivotA = constraint->getParentPivot();
        }
        mMultiBody->setupSpherical(getBoneID() - 1,
                                   mLink->m_mass,
                                   mLink->m_inertiaLocal,
                                   jointA->getBoneID() - 1,
                                   btQuaternion(rotDiff.x, rotDiff.y, rotDiff.z, rotDiff.w),
                                   bodyACOM2bodyBpivot,
                                   bodyBpivot);
        addConstraint();
    }


    void BulletJoint::setupHinge(BulletHingeConstraint* constraint)
    {
        Node* owner = owner_object();
        Node* parent = owner->parent();
        const glm::vec3& pivotA = constraint->getParentPivot();
        const glm::vec3& pivotB = constraint->getPivot();
        const glm::vec3& axis = constraint->getJointAxis();
        btVector3 pivotInB(pivotB.x, pivotB.y, pivotB.z);
        btVector3 axisIn(axis.x, axis.y, axis.z);
        BulletJoint* jointA = static_cast<BulletJoint*>(parent->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        glm::mat4 tA = parent->transform()->getModelMatrix(true);
        glm::mat4 tB = owner->transform()->getModelMatrix(true);
        btVector3 bodyACOM(tA[3][0], tA[3][1], tA[3][2]);
        btVector3 bodyBCOM(tB[3][0], tB[3][1], tB[3][2]);
        btVector3 bodyACOM2bodyBpivot = bodyBCOM + pivotInB - bodyACOM;

        mMultiBody->setupRevolute(getBoneID() - 1,
                          mLink->m_mass,
                          mLink->m_inertiaLocal,
                          jointA->getBoneID() - 1,
                          btQuaternion(0, 0, 0, 1),
                          axisIn,
                          bodyACOM2bodyBpivot,
                          pivotInB);
        mLink->m_jointLowerLimit = constraint->getLowerLimit();
        mLink->m_jointUpperLimit = constraint->getUpperLimit();
        addConstraint();
    }

    void BulletJoint::setupSlider(BulletSliderConstraint* constraint)
    {
        Node* owner = owner_object();
        Node* parent = owner->parent();
        BulletJoint* jointA = static_cast<BulletJoint*>(parent->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        glm::mat4 tA = parent->transform()->getModelMatrix(true);
        glm::mat4 tB = owner->transform()->getModelMatrix(true);
        btVector3 bodyACOM(tA[3][0], tA[3][1], tA[3][2]);
        btVector3 bodyBCOM(tB[3][0], tB[3][1], tB[3][2]);
        btVector3 diffCOM = bodyBCOM - bodyACOM;
        btVector3 bodyACOM2bodyBpivot = diffCOM;
        glm::vec3 jointAxis = PhysicsConstraint::findJointAxis(parent->transform(), owner->transform());

        mMultiBody->setupPrismatic(getBoneID() - 1,
                                  mLink->m_mass,
                                  mLink->m_inertiaLocal,
                                  jointA->getBoneID() - 1,
                                  btQuaternion(0, 0, 0, 1),
                                  btVector3(jointAxis.x, jointAxis.y, jointAxis.z),
                                  bodyACOM2bodyBpivot,
                                  btVector3(0, 0, 0), false);
        mLink->m_jointLowerLimit = constraint->getLinearLowerLimit();
        mLink->m_jointUpperLimit = constraint->getLinearUpperLimit();
        addConstraint();
    }

    void BulletJoint::addConstraint()
    {
        if (mWorld->isMultiBody())
        {
            BulletJoint* root = static_cast<BulletJoint*> (mMultiBody->getUserPointer());

            if (root != this)
            {
                ++(root->mConstraintsAdded);
                if (isReady())
                {
                    root->finalize();
                }
            }
        }
    }

    bool BulletJoint::validate()
    {
        int n = mMultiBody->getNumLinks();
        int numInvalid = 0;
        if (mLinksAdded < n)
        {
            return false;
        }
        for (int i = 0; i < n; ++i)
        {
            btMultibodyLink& link = mMultiBody->getLink(i);
            if (link.m_jointType == btMultibodyLink::eInvalid)
            {
                BulletJoint* j = (BulletJoint*) link.m_collider->getUserPointer();
                j->setupSpherical(nullptr);
                ++numInvalid;
            }
        }
        return false;
    }

    void BulletJoint::finalize()
    {
        btScalar q0 = -45.0f * M_PI / 180.0f;
        btQuaternion quat0(btVector3(1, 1, 0).normalized(), q0);
        quat0.normalize();
        mMultiBody->setJointPosMultiDof(0, quat0);
        mMultiBody->finalizeMultiDof();
        static_cast<btMultiBodyDynamicsWorld *>(mWorld->getPhysicsWorld())->addMultiBody(mMultiBody);
    }

    bool BulletJoint::isReady() const
    {
        if (mLink)
        {
            return (mLink->m_jointType != btMultibodyLink::eInvalid);
        }
        else
        {
            return mConstraintsAdded == mMultiBody->getNumLinks();
        }
    }

}
