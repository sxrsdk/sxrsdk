//
// Created by Juliana Figueira on 5/9/17.
//
#include "bullet_joint.h"
#include "bullet_rigidbody.h"
#include "bullet_fixedconstraint.h"
#include <BulletDynamics/ConstraintSolver/btFixedConstraint.h>

static const char tag[] = "PHYSICS";

namespace sxr {

BulletFixedConstraint::BulletFixedConstraint(PhysicsCollidable* bodyA)
{
    mFixedConstraint = 0;
    mRigidBodyA = bodyA;
    mBreakingImpulse = SIMD_INFINITY;
}

BulletFixedConstraint::BulletFixedConstraint(btFixedConstraint *constraint)
{
    mFixedConstraint = constraint;
    mRigidBodyA = nullptr; // TODO: what should this be?
    constraint->setUserConstraintPtr(this);
}

BulletFixedConstraint::~BulletFixedConstraint() {
    if (0 != mFixedConstraint) {
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

void BulletFixedConstraint::updateConstructionInfo(PhysicsWorld* world)
{
    if (mFixedConstraint != nullptr)
    {
        return;
    }
    BulletRigidBody* bodyB = (BulletRigidBody*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY);

    if (bodyB)
    {
        btRigidBody* rbB = bodyB->getRigidBody();
        btRigidBody* rbA = reinterpret_cast<BulletRigidBody*>(mRigidBodyA)->getRigidBody();
        mFixedConstraint = new btFixedConstraint(*rbA, *rbB,
                                                 rbB->getWorldTransform(),
                                                 rbA->getWorldTransform());
        mFixedConstraint->setBreakingImpulseThreshold(mBreakingImpulse);
    }
    else
    {
        BulletJoint* jointB = (BulletJoint*) owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_JOINT);
        if (jointB)
        {
            jointB->setupFixed(this);
        }
    }
}
}