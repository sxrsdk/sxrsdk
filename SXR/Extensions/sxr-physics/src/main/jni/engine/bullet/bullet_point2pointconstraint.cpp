//
// Created by Juliana Figueira on 5/9/17.
//

#include "bullet_point2pointconstraint.h"
#include <BulletDynamics/ConstraintSolver/btPoint2PointConstraint.h>
#include "bullet_joint.h"
#include "bullet_rigidbody.h"
#include "bullet_world.h"

static const char tag[] = "PHYSICS";

namespace sxr {

    BulletPoint2PointConstraint::BulletPoint2PointConstraint(PhysicsCollidable* bodyA,
            const glm::vec3& pivotA, const glm::vec3& pivotB)
    {
        mPoint2PointConstraint = 0;
        mBodyA = reinterpret_cast<BulletRigidBody*>(bodyA);
        mBreakingImpulse = SIMD_INFINITY;
        mPivotA = pivotA;
        mPivotB = pivotB;
    };

    // This constructor is only used when loading physics from bullet file
    BulletPoint2PointConstraint::BulletPoint2PointConstraint(btPoint2PointConstraint *constraint)
    {
        mPoint2PointConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletPoint2PointConstraint::~BulletPoint2PointConstraint()
    {
        if (mPoint2PointConstraint)
        {
            delete mPoint2PointConstraint;
        }
    };


    void BulletPoint2PointConstraint::setBreakingImpulse(float impulse)
    {
        if (mPoint2PointConstraint)
        {
            mPoint2PointConstraint->setBreakingImpulseThreshold(impulse);
        }
        else
        {
            mBreakingImpulse = impulse;
        }
    }

    float BulletPoint2PointConstraint::getBreakingImpulse() const
    {
        if (mPoint2PointConstraint)
        {
            return mPoint2PointConstraint->getBreakingImpulseThreshold();
        }
        else
        {
            return mBreakingImpulse;
        }
    }


void BulletPoint2PointConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if (mPoint2PointConstraint == nullptr)
    {
        btVector3 pA(mPivotA.x, mPivotA.y, mPivotA.z);
        btVector3 pB(mPivotB.x, mPivotB.y, mPivotB.z);
        BulletRigidBody* bodyB = (BulletRigidBody *) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY);
        if (bodyB)
        {
            btRigidBody* rbB = bodyB->getRigidBody();
            btRigidBody* rbA = reinterpret_cast<BulletRigidBody*>(mBodyA)->getRigidBody();

            mPoint2PointConstraint = new btPoint2PointConstraint(*rbA, *rbB, pA, pB);
            mPoint2PointConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
        }
    }
}

}
