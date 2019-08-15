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
#include <contrib/glm/gtc/type_ptr.hpp>

#include "bullet_world.h"
#include "bullet_rigidbody.h"
#include "bullet_sxr_utils.h"
#include "objects/components/sphere_collider.h"
#include "util/sxr_log.h"

#include <BulletDynamics/Dynamics/btDynamicsWorld.h>
#include <BulletCollision/CollisionDispatch/btCollisionObject.h>
#include <BulletCollision/CollisionShapes/btEmptyShape.h>
#include <LinearMath/btDefaultMotionState.h>
#include <LinearMath/btTransform.h>


namespace sxr {

BulletRigidBody::BulletRigidBody()
        : mConstructionInfo(btScalar(0.0f), nullptr, new btEmptyShape()),
          mRigidBody(new btRigidBody(mConstructionInfo)),
          m_centerOfMassOffset(btTransform::getIdentity()),
          mScale(1.0f, 1.0f, 1.0f),
          mSimType(SimulationType::DYNAMIC)
{
    mRigidBody->setUserPointer(this);
    mWorld = nullptr;
}

BulletRigidBody::BulletRigidBody(btRigidBody* rigidBody)
        : mConstructionInfo(btScalar(0.0f), nullptr, nullptr),
          mRigidBody(rigidBody),
          m_centerOfMassOffset(btTransform::getIdentity()),
          mScale(1.0f, 1.0f, 1.0f),
          mSimType(SimulationType::DYNAMIC)
{
    mRigidBody->setUserPointer(this);
    mConstructionInfo.m_mass = rigidBody->isStaticObject() ? 0.f : 1.f / rigidBody->getInvMass();
    mSimType = SimulationType ::DYNAMIC;
    if (rigidBody->isStaticObject())
    {
        mSimType = SimulationType ::STATIC;
    }
    else if (rigidBody->isKinematicObject())
    {
        mSimType = SimulationType::KINEMATIC;
    }
    mWorld = nullptr;
}

BulletRigidBody::~BulletRigidBody() {
    finalize();
}

void BulletRigidBody::setSimulationType(PhysicsRigidBody::SimulationType type)
{
    mSimType = type;
    switch (type)
    {
        case SimulationType::DYNAMIC:
        mRigidBody->setCollisionFlags(mRigidBody->getCollisionFlags() &
                                      ~(btCollisionObject::CollisionFlags::CF_KINEMATIC_OBJECT |
                                        btCollisionObject::CollisionFlags::CF_STATIC_OBJECT));
        mRigidBody->setActivationState(ACTIVE_TAG);
        break;

        case SimulationType::STATIC:
        mRigidBody->setCollisionFlags((mRigidBody->getCollisionFlags() |
                                      btCollisionObject::CollisionFlags::CF_STATIC_OBJECT) &
                                      ~btCollisionObject::CollisionFlags::CF_KINEMATIC_OBJECT);
        mRigidBody->setActivationState(ISLAND_SLEEPING);
        break;

        case SimulationType::KINEMATIC:
        mRigidBody->setCollisionFlags((mRigidBody->getCollisionFlags() |
                                       btCollisionObject::CollisionFlags::CF_KINEMATIC_OBJECT) &
                                       ~btCollisionObject::CollisionFlags::CF_STATIC_OBJECT);
        mRigidBody->setActivationState(ISLAND_SLEEPING);
        break;
    }
}

BulletRigidBody::SimulationType BulletRigidBody::getSimulationType() const
{
    return mSimType;
}

void BulletRigidBody::updateConstructionInfo(PhysicsWorld* world)
{
    if (mConstructionInfo.m_collisionShape != nullptr)
    {
        // This rigid body was not loaded so its construction must be finished
        mWorld = static_cast<BulletWorld*>(world);
        Collider *collider = (Collider *) owner_object()->getComponent(COMPONENT_TYPE_COLLIDER);
        if (collider)
        {
            bool isDynamic = (getMass() != 0.f);
            if (mConstructionInfo.m_collisionShape)
            {
                delete mConstructionInfo.m_collisionShape;
            }
            mRigidBody->setMotionState(this);
            mRigidBody->setMassProps(mConstructionInfo.m_mass, mConstructionInfo.m_localInertia);
            mConstructionInfo.m_collisionShape = convertCollider2CollisionShape(collider);
            if (isDynamic)
            {
                mConstructionInfo.m_collisionShape->calculateLocalInertia(getMass(),
                        mConstructionInfo.m_localInertia);
            }
            else
            {
                mSimType = SimulationType ::STATIC;
            }
            mRigidBody->setCollisionShape(mConstructionInfo.m_collisionShape);
            mRigidBody->setMassProps(getMass(), mConstructionInfo.m_localInertia);
            mRigidBody->updateInertiaTensor();
            updateCollisionShapeLocalScaling();
        }
        else
        {
            LOGE("PHYSICS: Cannot attach rigid body without collider");
        }
    }

    mRigidBody->setMotionState(this);
    getWorldTransform(prevPos);
}

void BulletRigidBody::finalize() {

    if (mRigidBody->getCollisionShape()) {
        mConstructionInfo.m_collisionShape = 0;
        delete mRigidBody->getCollisionShape();
    }

    if (mRigidBody) {
        delete mRigidBody;
        mRigidBody = 0;
    }
}

void BulletRigidBody::getRotation(float &w, float &x, float &y, float &z) {
    btTransform trans;

    if (mRigidBody->getMotionState()) {
        mRigidBody->getMotionState()->getWorldTransform(trans);
    } else {
        trans = mRigidBody->getCenterOfMassTransform();
    }

    btQuaternion rotation = trans.getRotation();

    w = rotation.getW();
    z = rotation.getZ();
    y = rotation.getY();
    x = rotation.getX();
}

void BulletRigidBody::getTranslation(float &x, float &y, float &z) {
    btTransform trans;
    if (mRigidBody->getMotionState()) {
        mRigidBody->getMotionState()->getWorldTransform(trans);
    } else {
        trans = mRigidBody->getCenterOfMassTransform();
    }

    btVector3 pos = trans.getOrigin();

    z = pos.getZ();
    y = pos.getY();
    x = pos.getX();
}

void BulletRigidBody::setCenterOfMass(Transform *t) {
    mRigidBody->setCenterOfMassTransform(convertTransform2btTransform(t));
}

void BulletRigidBody::getWorldTransform(btTransform &centerOfMassWorldTrans) const {
    Transform* trans = owner_object()->transform();

    centerOfMassWorldTrans = convertTransform2btTransform(trans)
                             * m_centerOfMassOffset.inverse();
}

void BulletRigidBody::setWorldTransform(const btTransform &centerOfMassWorldTrans)
{
    Node* owner = owner_object();
    Transform* trans = owner->transform();
    btTransform aux; getWorldTransform(aux);

    if(std::abs(aux.getOrigin().getX() - prevPos.getOrigin().getX()) >= 0.1f ||
       std::abs(aux.getOrigin().getY() - prevPos.getOrigin().getY()) >= 0.1f ||
       std::abs(aux.getOrigin().getZ() - prevPos.getOrigin().getZ()) >= 0.1f)
    {
        mRigidBody->setWorldTransform(aux);
        prevPos = aux;
        //TODO: incomplete solution
    }
    else
    {
        btTransform physicBody = (centerOfMassWorldTrans * m_centerOfMassOffset);
        btVector3 pos = physicBody.getOrigin();
        btQuaternion rot = physicBody.getRotation();
        Node* parent = owner->parent();
        float matrixData[16];

        physicBody.getOpenGLMatrix(matrixData);
        glm::mat4 worldMatrix(glm::make_mat4(matrixData));
        if ((parent != nullptr) && (parent->parent() != nullptr))
        {
            glm::mat4 parentWorld(parent->transform()->getModelMatrix(true));
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
        prevPos = physicBody;
    }
    if (mSimType == DYNAMIC)
    {
        mWorld->markUpdated(this);
    }
}

void BulletRigidBody::applyCentralForce(float x, float y, float z)
{
    mRigidBody->applyCentralForce(btVector3(x, y, z));
    if (!mRigidBody->isActive())
    {
        mRigidBody->activate(true);
    }
}

void BulletRigidBody::applyForce(float force_x, float force_y, float force_z,
		float rel_pos_x, float rel_pos_y, float rel_pos_z)
{
	mRigidBody->applyForce(btVector3(force_x, force_y, force_z),
			btVector3(rel_pos_x, rel_pos_y, rel_pos_z));
    if (!mRigidBody->isActive())
    {
        mRigidBody->activate(true);
    }
}

void BulletRigidBody::applyCentralImpulse(float x, float y, float z)
{
    mRigidBody->applyCentralImpulse(btVector3(x, y, z));
    if (!mRigidBody->isActive())
    {
        mRigidBody->activate(true);
    }
}

void BulletRigidBody::applyImpulse(float impulse_x, float impulse_y, float impulse_z,
        float rel_pos_x, float rel_pos_y, float rel_pos_z)
{
        mRigidBody->applyImpulse(btVector3(impulse_x, impulse_y, impulse_z),
                               btVector3(rel_pos_x, rel_pos_y, rel_pos_z));
        if (!mRigidBody->isActive())
        {
            mRigidBody->activate(true);
        }
}

void BulletRigidBody::applyTorque(float x, float y, float z)
{
    mRigidBody->applyTorque(btVector3(x, y, z));
    if (!mRigidBody->isActive())
    {
        mRigidBody->activate(true);
    }
}

void BulletRigidBody::applyTorqueImpulse(float x, float y, float z)
{
    mRigidBody->applyTorqueImpulse(btVector3(x, y, z));
    if (!mRigidBody->isActive())
    {
        mRigidBody->activate(true);
    }
}

void  BulletRigidBody::updateCollisionShapeLocalScaling()
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
    mRigidBody->getCollisionShape()->setLocalScaling(mScale * ownerScale);
}


void BulletRigidBody::setGravity(float x, float y, float z) {
    mRigidBody->setGravity(btVector3(x, y, z));
}

void BulletRigidBody::setDamping(float linear, float angular)
{
    mRigidBody->setDamping(linear, angular);
}

void BulletRigidBody::setLinearVelocity(float x, float y, float z)
{
    mRigidBody->setLinearVelocity(btVector3(x, y, z));
}

void BulletRigidBody::setAngularVelocity(float x, float y, float z)
{
    mRigidBody->setAngularVelocity(btVector3(x, y, z));
}

void BulletRigidBody::setAngularFactor(float x, float y, float z)
{
    mRigidBody->setAngularFactor(btVector3(x, y, z));
}

void BulletRigidBody::setLinearFactor(float x, float y, float z)
{
    mRigidBody->setLinearFactor(btVector3(x, y, z));
}

void BulletRigidBody::setFriction(float n)
{
    mRigidBody->setFriction(n);
}

void BulletRigidBody::setRestitution(float n)
{
    mRigidBody->setRestitution(n);
}

void BulletRigidBody::setSleepingThresholds(float linear, float angular)
{
    mRigidBody->setSleepingThresholds(linear, angular);
}

void BulletRigidBody::setCcdMotionThreshold(float n)
{
    mRigidBody->setCcdMotionThreshold(n);
}

void BulletRigidBody::setCcdSweptSphereRadius(float n)
{
    mRigidBody->setCcdSweptSphereRadius(n);
}

void BulletRigidBody::setContactProcessingThreshold(float n)
{
    mRigidBody->setContactProcessingThreshold(n);
}

void BulletRigidBody::setIgnoreCollisionCheck(PhysicsRigidBody *collisionObj, bool ignore)
{
    mRigidBody->setIgnoreCollisionCheck(((BulletRigidBody *) collisionObj)->getRigidBody(), ignore);
}

void BulletRigidBody::getGravity(float *v3) const {
    btVector3 result = mRigidBody->getLinearFactor();
    v3[0] = result.getX();
    v3[1] = result.getY();
    v3[2] = result.getZ();
}

void BulletRigidBody::getDamping(float &angular, float &linear) const {
    linear = mRigidBody->getLinearDamping();
    angular = mRigidBody->getAngularDamping();
}

void BulletRigidBody::getLinearVelocity(float *v3) const {
    btVector3 result = mRigidBody->getLinearVelocity();
    v3[0] = result.getX();
    v3[1] = result.getY();
    v3[2] = result.getZ();
}

void BulletRigidBody::getAngularVelocity(float *v3) const {
    btVector3 result = mRigidBody->getAngularVelocity();
    v3[0] = result.getX();
    v3[1] = result.getY();
    v3[2] = result.getZ();
}

void BulletRigidBody::getAngularFactor(float *v3) const {
    btVector3 result = mRigidBody->getAngularFactor();
    v3[0] = result.getX();
    v3[1] = result.getY();
    v3[2] = result.getZ();
}

void BulletRigidBody::getLinearFactor(float *v3) const {
    btVector3 result = mRigidBody->getLinearFactor();
    v3[0] = result.getX();
    v3[1] = result.getY();
    v3[2] = result.getZ();
}

float  BulletRigidBody::getFriction() const {
    return mRigidBody->getFriction();
}

float  BulletRigidBody::getRestitution() const {
    return mRigidBody->getRestitution();
}

float  BulletRigidBody::getCcdMotionThreshold() const {
    return mRigidBody->getCcdMotionThreshold();
}

float  BulletRigidBody::getCcdSweptSphereRadius() const {
    return mRigidBody->getCcdSweptSphereRadius();
}

float  BulletRigidBody::getContactProcessingThreshold() const {
    return mRigidBody->getContactProcessingThreshold();
}

void BulletRigidBody::reset(bool rebuildCollider)
{
    if (nullptr == mWorld)
    {
        // Not added yet
        return;
    }

    int collisionFilterGroup = mRigidBody->getBroadphaseProxy()->m_collisionFilterGroup;
    int collisionFilterMask = mRigidBody->getBroadphaseProxy()->m_collisionFilterMask;
    mWorld->getPhysicsWorld()->removeRigidBody(mRigidBody);

    if (rebuildCollider)
    {
        Collider *collider = (Collider *) owner_object_->getComponent(COMPONENT_TYPE_COLLIDER);
        bool isDynamic = (getMass() != 0.f);
        delete mConstructionInfo.m_collisionShape;
        mConstructionInfo.m_collisionShape = convertCollider2CollisionShape(collider);
        if (isDynamic)
        {
            mConstructionInfo.m_collisionShape->calculateLocalInertia(getMass(),
                    mConstructionInfo.m_localInertia);
        }
        else
        {
            mSimType = SimulationType::STATIC;
        }
        mRigidBody->setCollisionShape(mConstructionInfo.m_collisionShape);
        mRigidBody->setMassProps(getMass(), mConstructionInfo.m_localInertia);
        mRigidBody->updateInertiaTensor();
    }

    updateCollisionShapeLocalScaling();
    mRigidBody->setMotionState(this);
    getWorldTransform(prevPos);
    mWorld->getPhysicsWorld()->addRigidBody(mRigidBody, collisionFilterGroup, collisionFilterMask);
}

}
