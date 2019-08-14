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

#include "objects/node.h"
#include "objects/components/component_types.h"
#include "bullet_joint.h"
#include "bullet_jointmotor.h"
#include "bullet_sxr_utils.h"
#include "util/sxr_log.h"

#include "BulletDynamics/Featherstone/btMultiBodyDynamicsWorld.h"
#include "BulletDynamics/Featherstone/btMultiBodyJointMotor.h"
#include "BulletDynamics/Featherstone/btMultiBodySphericalJointMotor.h"
#include "BulletDynamics/Featherstone/btMultiBody.h"
#include "BulletDynamics/Featherstone/btMultiBodyLink.h"

#include "LinearMath/btScalar.h"
#include "LinearMath/btVector3.h"
#include "LinearMath/btTransform.h"
#include "LinearMath/btMatrix3x3.h"


namespace sxr {

    BulletJointMotor::BulletJointMotor(float maxImpulse)
            : PhysicsJointMotor()
    {
        mMotors[0] = nullptr;
        mMotors[1] = nullptr;
        mMotors[2] = nullptr;
        mMotors[3] = nullptr;
        mDOFCount = 0;
        mSpherical = false;
        mVelocityTarget[0] = 0;
        mVelocityTarget[1] = 0;
        mVelocityTarget[2] = 0;
        mPositionTarget[0] = 0;
        mPositionTarget[1] = 0;
        mPositionTarget[2] = 0;
        mMaxImpulse = maxImpulse;
    }

    BulletJointMotor::BulletJointMotor(btMultiBodyJointMotor* motor)
            : PhysicsJointMotor()
    {
        mSpherical = false;
        for (int i = 0; i < mDOFCount; ++i)
        {
            if (mMotors[i] == nullptr)
            {
                mMotors[i] = motor;
                return;
            }
        }
    }

    BulletJointMotor::~BulletJointMotor()
    {
        for (int i = 0; i < 3; ++i)
        {
            if (mMotors[i])
            {
                delete mMotors[i];
                // TODO: remove from Bullet world
            }
        }
    }

    void BulletJointMotor::setVelocityTarget(int dof, float v)
    {
        if ((dof >= 0) && (dof < 3))
        {
            btMultiBodyJointMotor* motor = static_cast<btMultiBodyJointMotor*> (mMotors[dof]);
            mVelocityTarget[dof] = v;
            if (motor != nullptr)
            {
                motor->setVelocityTarget(v);
            }
        }
    }

    void BulletJointMotor::setVelocityTarget(float vx, float vy, float vz)
    {
        btMultiBodyJointMotor* motor = static_cast<btMultiBodyJointMotor*> (mMotors[0]);

        mVelocityTarget[0] = vx;
        mVelocityTarget[1] = vy;
        mVelocityTarget[2] = vz;
        if (motor != nullptr)
        {
            if (mSpherical)
            {
                btMultiBodySphericalJointMotor* sm = static_cast<btMultiBodySphericalJointMotor*> (mMotors[0]);
                sm->setVelocityTarget(btVector3(vx, vy, vz));
                return;
            }
            motor->setVelocityTarget(vx);
        }
        motor = static_cast<btMultiBodyJointMotor*> (mMotors[1]);
        if (motor != nullptr)
        {
            motor->setVelocityTarget(vy);
        }
        motor = static_cast<btMultiBodyJointMotor*> (mMotors[2]);
        if (motor != nullptr)
        {
            motor->setVelocityTarget(vz);
        }
    }

    void BulletJointMotor::setPositionTarget(int dof, float p)
    {
        if ((dof >= 0) && (dof < 3))
        {
            btMultiBodyJointMotor* motor = static_cast<btMultiBodyJointMotor*> (mMotors[dof]);
            mPositionTarget[dof] = p;
            if (motor != nullptr)
            {
                motor->setPositionTarget(p);
            }
        }
    }

    void BulletJointMotor::setPositionTarget(float px, float py, float pz)
    {
        btMultiBodyJointMotor* motor = static_cast<btMultiBodyJointMotor*> (mMotors[0]);
        mPositionTarget[0] = px;
        mPositionTarget[1] = py;
        mPositionTarget[2] = pz;
        if (motor != nullptr)
        {
            motor->setPositionTarget(px);
        }
        motor = static_cast<btMultiBodyJointMotor*> (mMotors[1]);
        if (motor != nullptr)
        {
            motor->setPositionTarget(py);
        }
        motor = static_cast<btMultiBodyJointMotor*> (mMotors[2]);
        if (motor != nullptr)
        {
            motor->setPositionTarget(pz);
        }
    }

    void BulletJointMotor::setPositionTarget(float px, float py, float pz, float pw)
    {
        if (mSpherical)
        {
            btMultiBodySphericalJointMotor* motor = static_cast<btMultiBodySphericalJointMotor*> (mMotors[0]);
            motor->setPositionTarget(btQuaternion(px, py, pz, pw));
            return;
        }
        setPositionTarget(px, py, pz);
        btMultiBodyJointMotor* motor = static_cast<btMultiBodyJointMotor*> (mMotors[0]);
        mPositionTarget[3] = pw;
        if (motor != nullptr)
        {
            motor->setPositionTarget(pw);
        }
    }

    int   BulletJointMotor::getConstraintType() const { return -1; }

    void* BulletJointMotor::getUnderlying() { return mMotors[0]; }

    void  BulletJointMotor::setBreakingImpulse(float impulse) { }

    float BulletJointMotor::getBreakingImpulse() const { return mMaxImpulse; }

    void BulletJointMotor::updateConstructionInfo(PhysicsWorld* world)
    {
        Node* owner = owner_object();
        BulletJoint* joint = static_cast<BulletJoint*>(owner->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        if ((joint != nullptr) && (mDOFCount == 0))
        {
            mSpherical = joint->getLink()->m_jointType == btMultibodyLink::eSpherical;
            mDOFCount = (joint->getLink()->m_dofCount > 4) ? 4 : joint->getLink()->m_dofCount;
            if (mDOFCount == 1)
            {
                mMotors[0] = new btMultiBodyJointMotor(joint->getMultiBody(), joint->getBoneID() - 1, mVelocityTarget[0], mMaxImpulse);
                return;
            }
            for (int i = 0; i < mDOFCount; ++i)
            {
                if (mSpherical)
                {
                    mMotors[i] = new btMultiBodySphericalJointMotor(joint->getMultiBody(),
                                                           joint->getBoneID() - 1, mMaxImpulse);
                }
                else
                {
                    mMotors[i] = new btMultiBodyJointMotor(joint->getMultiBody(),
                                                           joint->getBoneID() - 1, i,
                                                           mVelocityTarget[i], mMaxImpulse);
                }
            }
        }

    }



}
