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
// Created by c.bozzetto on 31/05/2017.
//
#include "objects/node.h"
#include "../physics_world.h"

#include <BulletDynamics/ConstraintSolver/btSliderConstraint.h>
#include <BulletDynamics/Featherstone/btMultiBodySliderConstraint.h>
#include <LinearMath/btTransform.h>

#include "bullet_rigidbody.h"
#include "bullet_joint.h"
#include "bullet_sliderconstraint.h"
#include "bullet_sxr_utils.h"

static const char tag[] = "BulletSliderConstrN";

namespace sxr {

    BulletSliderConstraint::BulletSliderConstraint(PhysicsCollidable* bodyA, const glm::vec3& pivotA, const glm::vec3& pivotB)
    {
        mBodyA = bodyA;
        mPivotA = pivotA;
        mPivotB = pivotB;
        mConstraint = nullptr;
        mBreakingImpulse = SIMD_INFINITY;

        // Default values from btSliderConstraint
        mLowerAngularLimit = 0.0f;
        mUpperAngularLimit = 0.0f;
        mLowerLinearLimit = 1.0f;
        mUpperLinearLimit = -1.0f;
    }

    BulletSliderConstraint::BulletSliderConstraint(btSliderConstraint* constraint)
    {
        mConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletSliderConstraint::BulletSliderConstraint(btMultiBodySliderConstraint* constraint)
    {
        mMBConstraint = constraint;
        // TODO: figure out how to assign to mBodyA
    }

    BulletSliderConstraint::~BulletSliderConstraint()
    {
        if (mConstraint)
        {
            delete mConstraint;
        }
        if (mMBConstraint)
        {
            delete mMBConstraint;
        }
    }

    void BulletSliderConstraint::setAngularLowerLimit(float limit)
    {
        if (mConstraint)
        {
            mConstraint->setLowerAngLimit(limit);
        }
        else
        {
            mLowerAngularLimit = limit;
        }
    }

    float BulletSliderConstraint::getAngularLowerLimit() const
    {
        if (mConstraint)
        {
            return mConstraint->getLowerAngLimit();
        }
        else
        {
            return mLowerAngularLimit;
        }
    }

    void BulletSliderConstraint::setAngularUpperLimit(float limit)
    {
        if (mConstraint)
        {
            mConstraint->setUpperAngLimit(limit);
        }
        else
        {
            mUpperAngularLimit = limit;
        }
    }

    float BulletSliderConstraint::getAngularUpperLimit() const
    {
        if (mConstraint)
        {
            return mConstraint->getUpperAngLimit();
        }
        else
        {
            return mUpperAngularLimit;
        }
    }

    void BulletSliderConstraint::setLinearLowerLimit(float limit)
    {
        if (mConstraint)
        {
            mConstraint->setLowerLinLimit(limit);
        }
        else
        {
            mLowerLinearLimit = limit;
        }
    }

    float BulletSliderConstraint::getLinearLowerLimit() const
    {
        if (mConstraint)
        {
            return mConstraint->getLowerLinLimit();
        }
        else
        {
            return mLowerLinearLimit;
        }
    }

    void BulletSliderConstraint::setLinearUpperLimit(float limit)
    {
        if (mConstraint)
        {
            mConstraint->setUpperLinLimit(limit);
        }
        else
        {
            mUpperLinearLimit = limit;
        }
    }

    void BulletSliderConstraint::setBreakingImpulse(float impulse)
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

    float BulletSliderConstraint::getBreakingImpulse() const
    {
        return mBreakingImpulse;
    }

    float BulletSliderConstraint::getLinearUpperLimit() const
    {
        if (mConstraint)
        {
            return mConstraint->getUpperLinLimit();
        }
        else
        {
            return mUpperLinearLimit;
        }
    }

void BulletSliderConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if ((mConstraint != nullptr) || (mMBConstraint != nullptr))
    {
        return;
    }
    BulletRigidBody* rigidBodyB = (BulletRigidBody*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY);
    btTransform  worldFrameA = convertTransform2btTransform(mBodyA->owner_object()->transform());
    btTransform  worldFrameB = convertTransform2btTransform(owner_object()->transform());
    btTransform  localFrameA = worldFrameB.inverse() * worldFrameA;
    btTransform  localFrameB = worldFrameA.inverse() * worldFrameB;
    btVector3    pivotA(mPivotA.x, mPivotA.y, mPivotA.z);
    btVector3    pivotB(mPivotB.x, mPivotB.y, mPivotB.z);
    btVector3    sliderAxisA = localFrameA.getOrigin();
    btVector3    sliderAxisB = localFrameB.getOrigin();
    int          typeA = mBodyA->getType();

    sliderAxisA.normalize();
    sliderAxisB.normalize();
    if (rigidBodyB)
    {
        btRigidBody *rbB = rigidBodyB->getRigidBody();

        if (typeA == COMPONENT_TYPE_PHYSICS_RIGID_BODY)
        {
            btRigidBody* rbA = static_cast<BulletRigidBody *>(mBodyA)->getRigidBody();
            btMatrix3x3 rotX2SliderAxis;
            btVector3 Xaxis(1, 0, 0);
            btVector3 negXaxis(-1, 0, 0);

            rotX2SliderAxis = btMatrix3x3(shortestArcQuatNormalize2(Xaxis, sliderAxisA));
            localFrameA.getBasis() *= rotX2SliderAxis;
            rotX2SliderAxis = btMatrix3x3(shortestArcQuatNormalize2(negXaxis, sliderAxisB));
            localFrameB.getBasis() *= rotX2SliderAxis;
            localFrameA.setOrigin(pivotA);
            localFrameB.setOrigin(pivotB);

            mConstraint = new btSliderConstraint(*rbA, *rbB, localFrameA, localFrameB, true);
            mConstraint->setLowerAngLimit(mLowerAngularLimit);
            mConstraint->setUpperAngLimit(mUpperAngularLimit);
            mConstraint->setLowerLinLimit(mLowerLinearLimit);
            mConstraint->setUpperLinLimit(mUpperLinearLimit);
            mConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
        }
        else if (typeA == COMPONENT_TYPE_PHYSICS_JOINT)
        {
            BulletJoint* jointA = static_cast<BulletJoint*>(mBodyA);
            btMultiBody* mbA = jointA->getMultiBody();
            mMBConstraint = new btMultiBodySliderConstraint(mbA, jointA->getJointIndex(), rbB,
                                                            pivotA, pivotB,
                                                            localFrameA.getBasis(), localFrameB.getBasis(),
                                                            sliderAxisB);
        }
        return;
    }

    BulletJoint* jointB = (BulletJoint*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_JOINT);
    if (jointB)
    {
        btMultiBody* mbB = jointB->getMultiBody();

        if (typeA == COMPONENT_TYPE_PHYSICS_RIGID_BODY)
        {
            BulletRigidBody* rigidBodyA = static_cast<BulletRigidBody*>(mBodyA);
            btRigidBody* rbA = rigidBodyA->getRigidBody();
            mMBConstraint = new btMultiBodySliderConstraint(mbB, jointB->getJointIndex(), rbA,
                                                            pivotB, pivotA,
                                                            localFrameB.getBasis(), localFrameA.getBasis(),
                                                            sliderAxisB);

        }
        else if (typeA == COMPONENT_TYPE_PHYSICS_JOINT)
        {
            BulletJoint* jointA = static_cast<BulletJoint*>(mBodyA);
            btMultiBody* mbA = jointA->getMultiBody();
            mMBConstraint = new btMultiBodySliderConstraint(mbA, jointA->getJointIndex(),
                                                            mbB, jointB->getJointIndex(),
                                                            pivotA, pivotB,
                                                            localFrameA.getBasis(), localFrameB.getBasis(),
                                                            sliderAxisA);
        }
    }
}

}