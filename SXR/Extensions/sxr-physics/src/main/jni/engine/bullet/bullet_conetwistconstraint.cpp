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

//
// Created by c.bozzetto on 06/06/2017.
//

#include "bullet_conetwistconstraint.h"
#include "bullet_rigidbody.h"

#include <BulletDynamics/ConstraintSolver/btConeTwistConstraint.h>
#include <LinearMath/btScalar.h>
#include <glm/glm.hpp>
#include <glm/mat3x3.hpp>

const char tag[] = "PHYSICS";

namespace sxr {
    BulletConeTwistConstraint::BulletConeTwistConstraint(PhysicsCollidable* bodyA,
                                                         const glm::vec3& pivotA,
                                                         const glm::vec3& pivotB,
                                                         const glm::mat3& bodyRotation,
                                                         const glm::mat3& coneRotation)
    {
        mConeTwistConstraint = 0;
        mBodyA = bodyA;
        mBreakingImpulse = SIMD_INFINITY;
        mPivotA = pivotA;
        mPivotB = pivotB;
        mBodyRotation = bodyRotation;
        mConeRotation = coneRotation;
        mSwingLimit = SIMD_PI * 0.25f;
        mTwistLimit = SIMD_PI;
    }

    BulletConeTwistConstraint::BulletConeTwistConstraint(btConeTwistConstraint *constraint)
    {
        mConeTwistConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletConeTwistConstraint::~BulletConeTwistConstraint()
    {
        if (mConeTwistConstraint)
        {
            delete mConeTwistConstraint;
        }
    }

    void BulletConeTwistConstraint::setSwingLimit(float limit)
    {
        if (0 != mConeTwistConstraint)
        {
            mConeTwistConstraint->setLimit(4, limit);
            mConeTwistConstraint->setLimit(5, limit);
        }
        else
        {
            mSwingLimit = limit;
        }
    }

    float BulletConeTwistConstraint::getSwingLimit() const
    {
        if (mConeTwistConstraint)
        {
            return mConeTwistConstraint->getLimit(4);
        }
        else
        {
            return mSwingLimit;
        }
    }

    void BulletConeTwistConstraint::setTwistLimit(float limit)
    {
        if (mConeTwistConstraint)
        {
            mConeTwistConstraint->setLimit(3, limit);
        }
        else
        {
            mTwistLimit = limit;
        }
    }

    float BulletConeTwistConstraint::getTwistLimit() const
    {
        if (mConeTwistConstraint)
        {
            return mConeTwistConstraint->getLimit(3);
        }
        else
        {
            return mTwistLimit;
        }
    }

    void BulletConeTwistConstraint::setBreakingImpulse(float impulse)
    {
        if (mConeTwistConstraint)
        {
            mConeTwistConstraint->setBreakingImpulseThreshold(impulse);
        }
        else
        {
            mBreakingImpulse = impulse;
        }
    }

    float BulletConeTwistConstraint::getBreakingImpulse() const
    {
        if (mConeTwistConstraint)
        {
            return mConeTwistConstraint->getBreakingImpulseThreshold();
        }
        else
        {
            return mBreakingImpulse;
        }
    }

void BulletConeTwistConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if (mConeTwistConstraint)
    {
        return;
    }

    btRigidBody* rbB = reinterpret_cast<BulletRigidBody*>(owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY))->getRigidBody();
    btRigidBody* rbA = reinterpret_cast<BulletRigidBody*>(mBodyA)->getRigidBody();
    btVector3 pA(mPivotA.x, mPivotA.y, mPivotA.z);
    btVector3 pB(mPivotB.x, mPivotB.y, mPivotB.z);

    btMatrix3x3 m((btScalar) mBodyRotation[0][0], (btScalar) mBodyRotation[0][1], (btScalar) mBodyRotation[0][2],
                  (btScalar) mBodyRotation[1][0], (btScalar) mBodyRotation[1][1], (btScalar) mBodyRotation[1][2],
                  (btScalar) mBodyRotation[2][0], (btScalar) mBodyRotation[2][1], (btScalar) mBodyRotation[2][2]);
    btTransform fA(m, pA);

    m.setValue((btScalar) mConeRotation[0][0], (btScalar) mConeRotation[0][1], (btScalar) mConeRotation[0][2],
               (btScalar) mConeRotation[1][0], (btScalar) mConeRotation[1][1], (btScalar) mConeRotation[1][2],
               (btScalar) mConeRotation[2][0], (btScalar) mConeRotation[2][1], (btScalar) mConeRotation[2][2]);

    btTransform fB(m, pB);

    mConeTwistConstraint = new btConeTwistConstraint(*rbA, *rbB, fA, fB);
    mConeTwistConstraint->setLimit(mSwingLimit, mSwingLimit, mTwistLimit);
    mConeTwistConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
}
}