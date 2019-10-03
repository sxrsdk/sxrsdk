//
// Created by Juliana Figueira on 5/9/17.
//
#include "bullet_joint.h"
#include "bullet_rigidbody.h"
#include "bullet_fixedconstraint.h"
#include "bullet_sxr_utils.h"
#include <BulletDynamics/ConstraintSolver/btFixedConstraint.h>

static const char tag[] = "PHYSICS";

namespace sxr
{

    BulletFixedConstraint::BulletFixedConstraint(PhysicsCollidable *bodyA)
    {
        mFixedConstraint = 0;
        mBodyA = bodyA;
        mBreakingImpulse = SIMD_INFINITY;
    }

    BulletFixedConstraint::BulletFixedConstraint(btFixedConstraint *constraint)
    {
        mFixedConstraint = constraint;
        mBodyA = nullptr; // TODO: what should this be?
        constraint->setUserConstraintPtr(this);
    }

    BulletFixedConstraint::~BulletFixedConstraint()
    {
        if (0 != mFixedConstraint)
        {
            delete mFixedConstraint;
        }
    }

    void BulletFixedConstraint::setBreakingImpulse(float impulse)
    {
        if (0 != mFixedConstraint)
        {
            mFixedConstraint->setBreakingImpulseThreshold(impulse);
        }
        else
        {
            mBreakingImpulse = impulse;
        }
    }

    float BulletFixedConstraint::getBreakingImpulse() const
    {
        if (0 != mFixedConstraint)
        {
            return mFixedConstraint->getBreakingImpulseThreshold();
        }
        else
        {
            return mBreakingImpulse;
        }
    }

    void BulletFixedConstraint::updateConstructionInfo(PhysicsWorld *world)
    {
        if (mFixedConstraint != nullptr)
        {
            return;
        }
        BulletRigidBody* bodyB = reinterpret_cast<BulletRigidBody*>
                (owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY));

        if (bodyB)
        {
            btRigidBody *rbB = bodyB->getRigidBody();
            btRigidBody *rbA = reinterpret_cast<BulletRigidBody*>(mBodyA)->getRigidBody();
            btTransform  worldFrameA = convertTransform2btTransform(mBodyA->owner_object()->transform());
            btTransform  worldFrameB = convertTransform2btTransform(owner_object()->transform());
            btTransform  frameA((worldFrameB.inverse() * worldFrameA).getBasis());
            btTransform  frameB((worldFrameA.inverse() * worldFrameB).getBasis());

            mFixedConstraint = new btFixedConstraint(*rbA, *rbB, frameA, frameB);
            mFixedConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
        }
    }
}