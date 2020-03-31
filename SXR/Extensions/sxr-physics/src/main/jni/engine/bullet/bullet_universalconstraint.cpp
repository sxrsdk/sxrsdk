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
// Created by c.bozzetto on 09/06/2017.
//

#include "bullet_universalconstraint.h"
#include "bullet_joint.h"
#include "bullet_rigidbody.h"
#include "bullet_sxr_utils.h"

#include <BulletDynamics/Dynamics/btRigidBody.h>
#include <BulletDynamics/ConstraintSolver/btUniversalConstraint.h>
#include <glm/glm.hpp>
#include <glm/vec3.hpp>
#include "glm/gtc/type_ptr.hpp"

static const char tag[] = "PHYSICS";

namespace sxr {

    BulletUniversalConstraint::BulletUniversalConstraint(PhysicsCollidable* bodyA,
                    const glm::vec3& pivotB, const glm::vec3& axis1, const glm::vec3& axis2)
    {
        mConstraint = 0;
        mBodyA = bodyA;
        mBreakingImpulse = SIMD_INFINITY;
        mPivotB = pivotB;
        mAxis1 = axis1;
        mAxis2 = axis2;
    }

    BulletUniversalConstraint::BulletUniversalConstraint(btUniversalConstraint *constraint)
    {
        mConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletUniversalConstraint::~BulletUniversalConstraint()
    {
        if (mConstraint)
        {
            delete mConstraint;
        }
    }


    void BulletUniversalConstraint::setAngularLowerLimits(float limitX, float limitY, float limitZ)
    {
        if (mConstraint)
        {
            mConstraint->setAngularLowerLimit(btVector3(limitX, limitY, limitZ));
        }
        mAngularLowerLimits = glm::vec3(limitX, limitY, limitZ);
    }

    const glm::vec3&  BulletUniversalConstraint::getAngularLowerLimits() const
    {
        if (mConstraint)
        {
            btVector3 t;
            mConstraint->getAngularLowerLimit(t);
            mAngularLowerLimits.x = t.x();
            mAngularLowerLimits.y = t.y();
            mAngularLowerLimits.z = t.z();
        }
        return mAngularLowerLimits;
    }

    void BulletUniversalConstraint::setAngularUpperLimits(float limitX, float limitY, float limitZ)
    {
        if ( mConstraint)
        {
            mConstraint->setAngularUpperLimit(btVector3(limitX, limitY, limitZ));
        }
        mAngularUpperLimits = glm::vec3(limitX, limitY, limitZ);
    }

    const glm::vec3& BulletUniversalConstraint::getAngularUpperLimits() const
    {
        if (mConstraint)
        {
            btVector3 t;
            mConstraint->getAngularUpperLimit(t);
            mAngularUpperLimits.x = t.x();
            mAngularUpperLimits.y = t.y();
            mAngularUpperLimits.z = t.z();
        }
        return mAngularUpperLimits;
    }

    void BulletUniversalConstraint::setBreakingImpulse(float impulse)
    {
        if (mConstraint)
        {
            mConstraint->setBreakingImpulseThreshold(impulse);
        }
        else
        {
            mBreakingImpulse = impulse;
        }
    }

    float BulletUniversalConstraint::getBreakingImpulse() const
    {
        if (mConstraint)
        {
            return mConstraint->getBreakingImpulseThreshold();
        }
        else
        {
            return mBreakingImpulse;
        }
    }

void BulletUniversalConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if (mConstraint != nullptr)
    {
        return;
    }
    BulletRigidBody* bodyB = ((BulletRigidBody*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY));

    if (bodyB)
    {
        btRigidBody* rbB = bodyB->getRigidBody();
        BulletRigidBody* bodyA = reinterpret_cast<BulletRigidBody*>(mBodyA);
        btRigidBody* rbA = bodyA->getRigidBody();
        float        x, y, z;
        btVector3    p(mPivotB.x, mPivotB.y, mPivotB.z);
        btVector3    zaxis(mAxis1.x, mAxis1.y, mAxis1.z);
        btVector3    yaxis(mAxis2.x, mAxis2.y, mAxis2.z);

//        bodyA->getTranslation(x, y, z);
//        p += btVector3(x, y, z);
        mConstraint = new btUniversalConstraint(*rbA, *rbB, p, zaxis, yaxis);
        mConstraint->setAngularLowerLimit(Common2Bullet(mAngularLowerLimits));
        mConstraint->setAngularUpperLimit(Common2Bullet(mAngularUpperLimits));
        mConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
    }
}
}