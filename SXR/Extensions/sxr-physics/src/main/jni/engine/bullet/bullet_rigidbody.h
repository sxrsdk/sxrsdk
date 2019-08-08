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

#ifndef BULLET_RIGIDBODY_H_
#define BULLET_RIGIDBODY_H_

#include "../physics_rigidbody.h"
#include <BulletDynamics/Dynamics/btRigidBody.h>
#include <LinearMath/btMotionState.h>


class btDynamicsWorld;

namespace sxr {

class Node;
class BulletWorld;

class BulletRigidBody : public PhysicsRigidBody, btMotionState
{
 public:
    BulletRigidBody();

    BulletRigidBody(btRigidBody *rigidBody);

    virtual ~BulletRigidBody();

    btRigidBody *getRigidBody() const {
        return mRigidBody;
    }

    virtual void setSimulationType(SimulationType type);
    virtual SimulationType getSimulationType() const;

    virtual void setMass(float mass)
    {
        mConstructionInfo.m_mass = btScalar(mass);
    }

    virtual float getMass() const
    {
        return mConstructionInfo.m_mass;
    }

    virtual float getFriction() const;

    virtual void setFriction(float f);

    virtual void updateConstructionInfo(PhysicsWorld* world);

    void setCenterOfMass(Transform *t);

    void getRotation(float &w, float &x, float &y, float &z);

    void getTranslation(float &x, float &y, float &z);

    virtual void getWorldTransform(btTransform &worldTrans) const;

    virtual void setWorldTransform(const btTransform &worldTrans);

    void applyCentralForce(float x, float y, float z);

	void applyForce(float force_x, float force_y, float force_z,
			float rel_pos_x, float rel_pos_y, float rel_pos_z);

    void applyCentralImpulse(float x, float y, float z);

    void applyImpulse(float impulse_x, float impulse_y, float impulse_z,
                              float rel_pos_x, float rel_pos_y, float rel_pos_z);

    void applyTorque(float x, float y, float z);

    void applyTorqueImpulse(float x, float y, float z);

    void setGravity(float x, float y, float z);

    void setDamping(float linear, float angular);

    void setLinearVelocity(float x, float y, float z);

    void setAngularVelocity(float x, float y, float z);

    void setAngularFactor(float x, float y, float z);

    void setLinearFactor(float x, float y, float z);

    void setRestitution(float n);

    void setSleepingThresholds(float linear, float angular);

    void setCcdMotionThreshold(float n);

    void setCcdSweptSphereRadius(float n);

    void setContactProcessingThreshold(float n);

    void setIgnoreCollisionCheck(PhysicsRigidBody *collisionObj, bool ignore);

    void getGravity(float *v3) const;

    void getLinearVelocity(float *v3) const;

    void getAngularVelocity(float *v3) const;

    void getAngularFactor(float *v3) const;

    void getLinearFactor(float *v3) const;

    void getDamping(float &angular, float &linear) const;

    float getRestitution() const;

    float getCcdMotionThreshold() const;

    float getContactProcessingThreshold() const;

    float getCcdSweptSphereRadius() const;


    void reset(bool rebuildCollider);

private:

    void finalize();

    void updateCollisionShapeLocalScaling();


private:
    btRigidBody::btRigidBodyConstructionInfo mConstructionInfo;
    btRigidBody* mRigidBody;
    btTransform m_centerOfMassOffset;
    btTransform prevPos;
    btVector3 mScale;
    SimulationType mSimType;
    BulletWorld *mWorld;

    friend class BulletWorld;
};

}

#endif /* BULLET_RIGIDBODY_H_ */
