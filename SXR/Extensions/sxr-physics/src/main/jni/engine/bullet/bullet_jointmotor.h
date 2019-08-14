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

#ifndef BULLET_JOINTMOTOR_H_
#define BULLET_JOINTMOTOR_H_

#include "../physics_jointmotor.h"


class btDynamicsWorld;
class btMultiBodyConstraint;
class btMultiBodyJointMotor;

namespace sxr {


class BulletJointMotor : public PhysicsJointMotor
{
public:

    BulletJointMotor(float maxImpulse);
    BulletJointMotor(btMultiBodyJointMotor* motor);
    virtual ~BulletJointMotor();

    virtual void setVelocityTarget(int dof, float v);
    virtual void setVelocityTarget(float x, float y, float z);
    virtual void setPositionTarget(int dof, float p);
    virtual void setPositionTarget(float px, float py, float pz);
    virtual void setPositionTarget(float px, float py, float pz, float pw);
    virtual void updateConstructionInfo(PhysicsWorld* world);
    virtual int   getConstraintType() const;
    virtual void* getUnderlying();
    virtual void  setBreakingImpulse(float impulse);
    virtual float getBreakingImpulse() const;

protected:
    float                   mMaxImpulse;
    float                   mVelocityTarget[4];
    float                   mPositionTarget[4];
    int                     mDOFCount;
    bool                    mSpherical;
    btMultiBodyConstraint*  mMotors[4];
};

}

#endif /* BULLET_JOINTMOTOR_H_ */
