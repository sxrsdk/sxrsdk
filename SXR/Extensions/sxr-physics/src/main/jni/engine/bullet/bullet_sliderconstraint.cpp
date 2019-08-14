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

    BulletSliderConstraint::BulletSliderConstraint(PhysicsCollidable* bodyA)
    {
        mBodyA = bodyA;
        mSliderConstraint = nullptr;
        mBreakingImpulse = SIMD_INFINITY;

        // Default values from btSliderConstraint
        mLowerAngularLimit = 0.0f;
        mUpperAngularLimit = 0.0f;
        mLowerLinearLimit = 1.0f;
        mUpperLinearLimit = -1.0f;
    }

    BulletSliderConstraint::BulletSliderConstraint(btSliderConstraint* constraint)
    {
        mSliderConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletSliderConstraint::~BulletSliderConstraint()
    {
        if (mSliderConstraint)
        {
            delete mSliderConstraint;
        }
    }

    void BulletSliderConstraint::setAngularLowerLimit(float limit)
    {
        if (mSliderConstraint)
        {
            mSliderConstraint->setLowerAngLimit(limit);
        }
        else
        {
            mLowerAngularLimit = limit;
        }
    }

    float BulletSliderConstraint::getAngularLowerLimit() const
    {
        if (mSliderConstraint)
        {
            return mSliderConstraint->getLowerAngLimit();
        }
        else
        {
            return mLowerAngularLimit;
        }
    }

    void BulletSliderConstraint::setAngularUpperLimit(float limit)
    {
        if (mSliderConstraint)
        {
            mSliderConstraint->setUpperAngLimit(limit);
        }
        else
        {
            mUpperAngularLimit = limit;
        }
    }

    float BulletSliderConstraint::getAngularUpperLimit() const
    {
        if (mSliderConstraint)
        {
            return mSliderConstraint->getUpperAngLimit();
        }
        else
        {
            return mUpperAngularLimit;
        }
    }

    void BulletSliderConstraint::setLinearLowerLimit(float limit)
    {
        if (mSliderConstraint)
        {
            mSliderConstraint->setLowerLinLimit(limit);
        }
        else
        {
            mLowerLinearLimit = limit;
        }
    }

    float BulletSliderConstraint::getLinearLowerLimit() const
    {
        if (mSliderConstraint)
        {
            return mSliderConstraint->getLowerLinLimit();
        }
        else
        {
            return mLowerLinearLimit;
        }
    }

    void BulletSliderConstraint::setLinearUpperLimit(float limit)
    {
        if (mSliderConstraint)
        {
            mSliderConstraint->setUpperLinLimit(limit);
        }
        else
        {
            mUpperLinearLimit = limit;
        }
    }

    void BulletSliderConstraint::setBreakingImpulse(float impulse)
    {
        if (mSliderConstraint)
        {
            mSliderConstraint->setBreakingImpulseThreshold(impulse);
        }
        else
        {
            mBreakingImpulse = impulse;
        }
    }

    float BulletSliderConstraint::getBreakingImpulse() const
    {
        if (mSliderConstraint)
        {
            return mSliderConstraint->getBreakingImpulseThreshold();
        }
        else
        {
            return mBreakingImpulse;
        }
    }

    float BulletSliderConstraint::getLinearUpperLimit() const
    {
        if (mSliderConstraint)
        {
            return mSliderConstraint->getUpperLinLimit();
        }
        else
        {
            return mUpperLinearLimit;
        }
    }

void BulletSliderConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if (mSliderConstraint != nullptr)
    {
        return;
    }
    BulletRigidBody* rigidBodyB = (BulletRigidBody*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY);
    if (rigidBodyB)
    {
        Transform* tB = owner_object()->transform();
        btMatrix3x3 rotB(btQuaternion(tB->rotation_x(), tB->rotation_y(), tB->rotation_z(), tB->rotation_w()));
        btTransform frameInB(rotB);
        btRigidBody* rbB = rigidBodyB->getRigidBody();
        btRigidBody* rbA = reinterpret_cast<BulletRigidBody*>(mBodyA)->getRigidBody();
        btTransform frameInA = convertTransform2btTransform(mBodyA->owner_object()->transform());
        btVector3 posA = frameInA.getOrigin();
        btVector3 posB(tB->position_x(), tB->position_y(), tB->position_z());
        btVector3 sliderAxis = posA - posB;
        btMatrix3x3 rotX2SliderAxis;
        btVector3 Xaxis(1, 0, 0);

        rotX2SliderAxis = btMatrix3x3(shortestArcQuatNormalize2(Xaxis, sliderAxis));
        frameInA.setOrigin(btVector3(0, 0, 0));
        frameInA.getBasis() *= rotX2SliderAxis;
        frameInB.getBasis() *= rotX2SliderAxis;
        mSliderConstraint = new btSliderConstraint(*rbA, *rbB, frameInA, frameInB, true);
        mSliderConstraint->setLowerAngLimit(mLowerAngularLimit);
        mSliderConstraint->setUpperAngLimit(mUpperAngularLimit);
        mSliderConstraint->setLowerLinLimit(mLowerLinearLimit);
        mSliderConstraint->setUpperLinLimit(mUpperLinearLimit);
        mSliderConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
    }
    else
    {
        BulletJoint* jointB = (BulletJoint*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_JOINT);

        if (jointB)
        {
            jointB->setupSlider(this);
        }
    }


}

}