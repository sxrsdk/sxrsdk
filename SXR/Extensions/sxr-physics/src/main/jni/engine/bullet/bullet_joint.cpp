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

    BulletJoint::BulletJoint(float mass, int numBones)
            : PhysicsJoint(mass, numBones),
              mMultiBody(nullptr),
              mCollider(nullptr),
              mLink(nullptr),
              mBoneID(0),
              mAxis(1, 0, 0),
              mSkeleton(nullptr),
              mWorld(nullptr),
              mJointType(JointType::baseJoint),
              mLinksAdded(0)
    {
        mMultiBody = new btMultiBody(numBones - 1, mass, btVector3(0, 0, 0), (mass == 0), false);
        mMultiBody->setUserPointer(this);
        mMultiBody->setBaseMass(mass);
        mMultiBody->setCanSleep(false);
        mMultiBody->setHasSelfCollision(false);
    }

    BulletJoint::BulletJoint(BulletJoint* parent, JointType jointType, int boneID, float mass)
            : PhysicsJoint(parent, jointType, boneID, mass),
              mMultiBody(nullptr),
              mCollider(nullptr),
              mBoneID(boneID),
              mAxis(1, 0, 0),
              mSkeleton(nullptr),
              mJointType(jointType),
              mWorld(nullptr),
              mLinksAdded(0)
    {
        mMultiBody = parent->getMultiBody();
        btMultibodyLink& link = mMultiBody->getLink(boneID - 1);
        link.m_mass = mass;
        link.m_parent = parent->getBoneID();
        link.m_userPtr = this;
        mLink = &link;
    }

    BulletJoint::BulletJoint(btMultiBody* multiBody)
            : PhysicsJoint(multiBody->getNumLinks(), multiBody->getBaseMass()),
              mMultiBody(multiBody),
              mAxis(1, 0, 0),
              mSkeleton(nullptr),
              mLink(nullptr),
              mWorld(nullptr),
              mLinksAdded(0)
    {
        mMultiBody->setUserPointer(this);
    }

    BulletJoint::~BulletJoint()
    {
        destroy();
    }

    PhysicsJoint* BulletJoint::getParent() const
    {
        if ((mLink == nullptr) || (mMultiBody == nullptr))
        {
            return nullptr;
        }
        int parentIndex = mLink->m_parent;
        btMultibodyLink& parentLink = mMultiBody->getLink(parentIndex);
        const void* p = parentLink.m_userPtr;
        return (PhysicsJoint*) p;
    }

    Skeleton* BulletJoint::getSkeleton() const
    {
        BulletJoint* root = static_cast<BulletJoint*> (mMultiBody->getUserPointer());
        return root->mSkeleton;
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
        Skeleton* skel = getSkeleton();

        if (skel)
        {
            const glm::mat4* m = skel->getWorldBoneMatrix(getBoneID());
            centerOfMassWorldTrans = convertTransform2btTransform(*m);
        }
        else
        {
            centerOfMassWorldTrans = convertTransform2btTransform(trans);
        }
    }

    void BulletJoint::updateWorldTransform()
    {
        btTransform t;
        Skeleton* skel = getSkeleton();
        Node* owner = owner_object();

        if (skel)
        {
            const glm::mat4* m = skel->getWorldBoneMatrix(getBoneID());
            t = convertTransform2btTransform(*m);
        }
        else
        {
            Transform* trans = owner->transform();

            t = convertTransform2btTransform(trans);
        }
        btVector3 pos = t.getOrigin();

        LOGE("BULLET: UPDATE %s, %f, %f, %f", owner->name().c_str(), pos.getX(), pos.getY(), pos.getZ());
        if (mLink == nullptr)
        {
            mMultiBody->setBaseWorldTransform(t);
        }
        if (mCollider)
        {
            mCollider->setWorldTransform(t);
        }
    }

    void BulletJoint::setWorldTransform(const btTransform& centerOfMassWorldTrans)
    {
        Node* owner = owner_object();
        btTransform aux; getWorldTransform(aux);
        btTransform physicBody = centerOfMassWorldTrans;
        btVector3 pos = physicBody.getOrigin();
        btQuaternion rot = physicBody.getRotation();
        Node* parent = owner->parent();
        Skeleton* skel = getSkeleton();
        float matrixData[16];

        centerOfMassWorldTrans.getOpenGLMatrix(matrixData);
        glm::mat4 worldMatrix(glm::make_mat4(matrixData));
        if (skel)
        {
            int boneID = getBoneID();
            int parentBoneID = mLink->m_parent + 1;
            glm::mat4& skelLocalMatrix = *skel->getLocalBoneMatrix(boneID);
            glm::mat4 localMatrix;

            if (boneID > 0)
            {
                const glm::mat4& parentWorld = *skel->getWorldBoneMatrix(parentBoneID);
                glm::mat4 parentInverseWorld(glm::inverse(parentWorld));

                localMatrix = parentInverseWorld * worldMatrix;
            }
            else
            {
                if (parent != nullptr)
                {
                    glm::mat4 parentWorld(parent->transform()->getModelMatrix(false));
                    glm::mat4 parentInverseWorld(glm::inverse(parentWorld));

                    localMatrix = parentInverseWorld * worldMatrix;
                }
                else
                {
                    glm::quat q(rot.getX(), rot.getY(), rot.getZ(), rot.getW());
                    glm::mat4 localMatrix = glm::mat4_cast(q);
                }
            }
            localMatrix[3] = skelLocalMatrix[3];
            skelLocalMatrix = localMatrix;
            LOGD("BULLET: JOINT %s %f, %f, %f", owner->name().c_str(),
                    localMatrix[3][0], localMatrix[3][1], localMatrix[3][2]);
        }
        else
        {
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
        mWorld->markUpdated(this);
    }

    Skeleton* BulletJoint::createSkeleton()
    {
        int numbones = mMultiBody->getNumLinks() + 1;
        int boneParents[numbones];
        const char* boneNames[numbones];

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
        Skeleton* skel = new Skeleton(boneParents, numbones);
        skel->updateBones(boneParents, boneNames, numbones);
        return skel;
    }

    void BulletJoint::applyTorque(float x, float y, float z)
    {
        if (mLink)
        {
            float torque[] = { x, y, z, 0 };
            mMultiBody->addJointTorqueMultiDof(getBoneID() - 1, torque);
            mMultiBody->addLinkTorque(getBoneID() - 1, btVector3(x, y, z));
        }
        else
        {
            mMultiBody->addBaseTorque(btVector3(x, y, z));
        }
    }

    void BulletJoint::applyTorque(float t)
    {
        if (mLink)
        {
            mMultiBody->addJointTorque(getBoneID() - 1, t);
        }
        else
        {
            btVector3 torque(t, 0, 0);
            mMultiBody->addBaseTorque(torque);
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
                switch (mJointType)
                {
                    case JointType::fixedJoint: setupFixed(); break;
                    case JointType::prismaticJoint: setupSlider(); break;
                    case JointType::revoluteJoint: setupHinge(); break;
                    default: setupSpherical(); break;
                }
            }
            else
            {
                mMultiBody->setBaseName(owner->name().c_str());
            }
        }
        updateWorldTransform();
    }

    void BulletJoint::setupFixed()
    {
        int parentIndex = mLink->m_parent;
        btMultibodyLink& parentLink = mMultiBody->getLink(parentIndex);
        const BulletJoint* jointA = reinterpret_cast<const BulletJoint*>(parentLink.m_userPtr);
        btTransform worldA;  jointA->getWorldTransform(worldA);
        btTransform worldB; getWorldTransform(worldB);
        btQuaternion rot(worldA.getRotation());
        btVector3 bodyACOM(worldA.getOrigin());
        btVector3 bodyBCOM(worldB.getOrigin());
        btVector3 diffCOM = bodyBCOM - bodyACOM;

        mMultiBody->setupFixed(getBoneID() - 1,
                                   mLink->m_mass,
                                   mLink->m_inertiaLocal,
                                   jointA->getBoneID() - 1,
                                   rot,
                                   diffCOM,
                                   btVector3(0, 0, 0),
                                   true);
        BulletJoint* root = reinterpret_cast<BulletJoint*> (mMultiBody->getUserPointer());
        if (root->isReady())
        {
            root->finalize();
        }
    }


    void BulletJoint::setupSpherical()
    {
        int parentIndex = mLink->m_parent;
        btMultibodyLink& parentLink = mMultiBody->getLink(parentIndex);
        const BulletJoint*  jointA = reinterpret_cast<const BulletJoint*>(parentLink.m_userPtr);
        btTransform   worldA;  jointA->getWorldTransform(worldA);
        btTransform   worldB; getWorldTransform(worldB);
        btQuaternion  rotA(worldA.getRotation());
        btVector3     posB(worldB.getOrigin());
        btMultibodyLink& link = mMultiBody->getLink(getBoneID() - 1);

        mMultiBody->setupSpherical(getBoneID() - 1,
                                   mLink->m_mass,
                                   mLink->m_inertiaLocal,
                                   jointA->getBoneID() - 1,
                                   rotA,
                                   btVector3(0, 0, 0),
                                   posB, true);
        BulletJoint* root = reinterpret_cast<BulletJoint*> (mMultiBody->getUserPointer());
        if (root->isReady())
        {
            root->finalize();
        }
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
        int parentIndex = mLink->m_parent;
        btMultibodyLink& parentLink = mMultiBody->getLink(parentIndex);
        const BulletJoint*  jointA = reinterpret_cast<const BulletJoint*>(parentLink.m_userPtr);
        btTransform   worldA; jointA->getWorldTransform(worldA);
        btTransform   worldB; getWorldTransform(worldB);
        btQuaternion  rotA(worldA.getRotation());
        btVector3     posB(worldB.getOrigin());
        btVector3     hingeAxis(mAxis.x, mAxis.y, mAxis.z);

        mMultiBody->setupRevolute(getBoneID() - 1,
                          mLink->m_mass,
                          mLink->m_inertiaLocal,
                          jointA->getBoneID() - 1,
                          rotA,
                          hingeAxis,
                          btVector3(0, 0, 0),
                          posB, true);
        BulletJoint* root = reinterpret_cast<BulletJoint*> (mMultiBody->getUserPointer());
        if (root->isReady())
        {
            root->finalize();
        }
    }

    void BulletJoint::setupSlider()
    {
        int parentIndex = mLink->m_parent;
        btMultibodyLink& parentLink = mMultiBody->getLink(parentIndex);
        const BulletJoint*  jointA = reinterpret_cast<const BulletJoint*>(parentLink.m_userPtr);
        btTransform   worldA; jointA->getWorldTransform(worldA);
        btTransform   worldB; getWorldTransform(worldB);
        btQuaternion  rotA(worldA.getRotation());
        btVector3     posB(worldB.getOrigin());
        btVector3     sliderAxis(posB);

        mMultiBody->setupPrismatic(getBoneID() - 1,
                                  mLink->m_mass,
                                  mLink->m_inertiaLocal,
                                  jointA->getBoneID() - 1,
                                  rotA,
                                  sliderAxis.normalized(),
                                  btVector3(0, 0, 0),
                                  posB,
                                  true);
        BulletJoint* root = static_cast<BulletJoint*> (mMultiBody->getUserPointer());
        if (root->isReady())
        {
            root->finalize();
        }
    }

    void BulletJoint::finalize()
    {
        mMultiBody->finalizeMultiDof();
        reinterpret_cast<btMultiBodyDynamicsWorld *>(mWorld->getPhysicsWorld())->addMultiBody(mMultiBody);
        mSkeleton = createSkeleton();
    }

    bool BulletJoint::isReady() const
    {
        if (mLink)
        {
            return (mLink->m_jointType != btMultibodyLink::eInvalid);
        }
        else
        {
            return mLinksAdded == mMultiBody->getNumLinks();
        }
    }

}
