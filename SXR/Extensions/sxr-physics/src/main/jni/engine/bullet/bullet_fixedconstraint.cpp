//
// Created by Juliana Figueira on 5/9/17.
//
#include "bullet_joint.h"
#include "bullet_rigidbody.h"
#include "bullet_fixedconstraint.h"
#include "bullet_sxr_utils.h"
#include <BulletDynamics/ConstraintSolver/btFixedConstraint.h>
#include <BulletDynamics/Featherstone/btMultiBodyFixedConstraint.h>

static const char tag[] = "PHYSICS";

namespace sxr
{

    BulletFixedConstraint::BulletFixedConstraint(PhysicsCollidable* bodyA)
    {
        mConstraint = nullptr;
        mMBConstraint = nullptr;
        mBodyA = bodyA;
        mBreakingImpulse = SIMD_INFINITY;
    }

    BulletFixedConstraint::BulletFixedConstraint(btFixedConstraint* constraint)
    {
        mConstraint = constraint;
        mBodyA = static_cast<BulletRigidBody*>(constraint->getRigidBodyA().getUserPointer());
        constraint->setUserConstraintPtr(this);
    }

    BulletFixedConstraint::BulletFixedConstraint(btMultiBodyFixedConstraint* constraint)
    {
        mMBConstraint = constraint;
        mBodyA = nullptr;         // TODO: figure out how to assign to mBodyA

    }

    BulletFixedConstraint::~BulletFixedConstraint()
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

    void BulletFixedConstraint::setBreakingImpulse(float impulse)
    {
        if (mMBConstraint)
        {
            mMBConstraint->setMaxAppliedImpulse(impulse);
        }
        else if (mConstraint)
        {
            mConstraint->setBreakingImpulseThreshold(impulse);
        }
        mBreakingImpulse = impulse;
    }

    float BulletFixedConstraint::getBreakingImpulse() const
    {
        return mBreakingImpulse;
    }

    void BulletFixedConstraint::updateConstructionInfo(PhysicsWorld *world)
    {
        if ((mConstraint != nullptr) || (mMBConstraint != nullptr))
        {
            return;
        }
        btTransform  worldFrameA = convertTransform2btTransform(mBodyA->owner_object()->transform());
        btTransform  worldFrameB = convertTransform2btTransform(owner_object()->transform());
        BulletRigidBody* bodyB = static_cast<BulletRigidBody*>
                                    (owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_RIGID_BODY));
        btVector3    pA(mPivotA.x, mPivotA.y, mPivotA.z);
        btVector3    pB(mPivotB.x, mPivotB.y, mPivotB.z);
        btTransform  frameA = worldFrameB.inverse() * worldFrameA;
        btTransform  frameB = worldFrameA.inverse() * worldFrameB;

        frameA.setOrigin(pA);
        frameB.setOrigin(pB);
        if (bodyB)
        {
            if (mBodyA->getType() == COMPONENT_TYPE_PHYSICS_RIGID_BODY)
            {
                btRigidBody* rbB = bodyB->getRigidBody();
                btRigidBody* rbA = static_cast<BulletRigidBody*>(mBodyA)->getRigidBody();
                btFixedConstraint* constraint = new btFixedConstraint(*rbA, *rbB, frameA, frameB);
                constraint->setBreakingImpulseThreshold(mBreakingImpulse);
                mConstraint = constraint;
            }
            else if (mBodyA->getType() == COMPONENT_TYPE_PHYSICS_JOINT)
            {
                btRigidBody* rbB = bodyB->getRigidBody();
                BulletJoint* jointA = static_cast<BulletJoint*>(mBodyA);
                btMultiBody* mbA = jointA->getMultiBody();
                btMultiBodyFixedConstraint* constraint = new btMultiBodyFixedConstraint(mbA,
                                                                                        jointA->getJointIndex() - 1, rbB,
                                                            pA, pB, frameA.getBasis(), frameB.getBasis());
                mMBConstraint = constraint;
                constraint->setMaxAppliedImpulse(mBreakingImpulse);
            }
            return;
        }
        BulletJoint* jointB = static_cast<BulletJoint*>
                                (owner_object()->getComponent(COMPONENT_TYPE_PHYSICS_JOINT));
        if (jointB)
        {
            btMultiBody *mbB = jointB->getMultiBody();

            if (mBodyA->getType() == COMPONENT_TYPE_PHYSICS_JOINT)
            {
                BulletJoint *jointA = static_cast<BulletJoint *>(mBodyA);
                btMultiBody *mbA = jointA->getMultiBody();
                btMultiBodyFixedConstraint *constraint = new btMultiBodyFixedConstraint(
                        mbA, jointA->getJointIndex(),
                        mbB, jointB->getJointIndex(),
                        pA, pB,
                        frameA.getBasis(), frameB.getBasis());
                constraint->setMaxAppliedImpulse(mBreakingImpulse);
                mMBConstraint = constraint;
            }
            else if (mBodyA->getType() == COMPONENT_TYPE_PHYSICS_RIGID_BODY)
            {
                btRigidBody *rbA = static_cast<BulletRigidBody *>(mBodyA)->getRigidBody();
                btMultiBody *mbB = jointB->getMultiBody();
                btMultiBodyFixedConstraint *constraint = new btMultiBodyFixedConstraint(
                        mbB, jointB->getJointIndex(),
                        rbA, pB, pA,
                        frameB.getBasis(), frameA.getBasis());
                constraint->setMaxAppliedImpulse(mBreakingImpulse);
                mMBConstraint = constraint;
            }
        }
    }
}