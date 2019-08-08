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
            float pivotInA[], float pivotInB[])
    {
        mPoint2PointConstraint = 0;
        mRigidBodyA = reinterpret_cast<BulletRigidBody*>(bodyA);
        mBreakingImpulse = SIMD_INFINITY;
        mPivotInA.x = pivotInA[0];
        mPivotInA.y = pivotInA[1];
        mPivotInA.z = pivotInA[2];
        mPivotInB.x = pivotInB[0];
        mPivotInB.y = pivotInB[1];
        mPivotInB.z = pivotInB[2];
    };

    // This constructor is only used when loading physics from bullet file
    BulletPoint2PointConstraint::BulletPoint2PointConstraint(btPoint2PointConstraint *constraint)
    {
        mPoint2PointConstraint = constraint;
        mRigidBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletPoint2PointConstraint::~BulletPoint2PointConstraint()
    {
        if (mPoint2PointConstraint)
        {
            delete mPoint2PointConstraint;
        }
    };

    void BulletPoint2PointConstraint::setPivotInA(const glm::vec3& pivot)
    {
        mPivotInA = pivot;

        btVector3 p(pivot.x, pivot.y, pivot.z);
        mPoint2PointConstraint->setPivotA(p);
    }

    void BulletPoint2PointConstraint::setPivotInB(const glm::vec3&  pivot)
    {
        mPivotInB = pivot;

        btVector3 p(pivot.x, pivot.y, pivot.z);
        mPoint2PointConstraint->setPivotB(p);
    }

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
        btVector3 pivotInA(mPivotInA.x, mPivotInA.y, mPivotInA.z);
        btVector3 pivotInB(mPivotInB.x, mPivotInB.y, mPivotInB.z);
        BulletRigidBody* bodyB = (BulletRigidBody *) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY);
        if (bodyB)
        {
            btRigidBody* rbB = bodyB->getRigidBody();
            btRigidBody* rbA = reinterpret_cast<BulletRigidBody*>(mRigidBodyA)->getRigidBody();

            mPoint2PointConstraint = new btPoint2PointConstraint(*rbA, *rbB, pivotInA, pivotInB);
            mPoint2PointConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
        }
    }
}

}
