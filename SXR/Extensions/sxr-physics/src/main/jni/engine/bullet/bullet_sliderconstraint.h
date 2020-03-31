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

#ifndef EXTENSIONS_BULLET_SLIDERCONSTRAINT_H
#define EXTENSIONS_BULLET_SLIDERCONSTRAINT_H

#include "../physics_sliderconstraint.h"
#include "../physics_collidable.h"

class btSliderConstraint;
class btMultiBodySliderConstraint;

namespace sxr {

    class PhysicsRigidBody;
    class BulletRigidBody;

    class BulletSliderConstraint : public PhysicsSliderConstraint
    {
    public:
        BulletSliderConstraint(PhysicsCollidable* bodyA, const glm::vec3& pivotA, const glm::vec3& pivotB);

        BulletSliderConstraint(btSliderConstraint *constraint);

        BulletSliderConstraint(btMultiBodySliderConstraint *constraint);

        virtual ~BulletSliderConstraint();

        virtual void setAngularLowerLimit(float limit);

        virtual float getAngularLowerLimit() const;

        virtual void setAngularUpperLimit(float limit);

        virtual float getAngularUpperLimit() const;

        virtual void setLinearLowerLimit(float limit);

        virtual float getLinearLowerLimit() const;

        virtual void setLinearUpperLimit(float limit);

        virtual float getLinearUpperLimit() const;

        virtual void setBreakingImpulse(float impulse);

        virtual float getBreakingImpulse() const;

        void* getUnderlying() { return mMBConstraint ? static_cast<void*>(mMBConstraint) : static_cast<void*>(mConstraint); }

        virtual void updateConstructionInfo(PhysicsWorld*);


    private:
        btSliderConstraint* mConstraint;
        btMultiBodySliderConstraint* mMBConstraint;
        float mBreakingImpulse;
        float mLowerAngularLimit;
        float mUpperAngularLimit;
        float mLowerLinearLimit;
        float mUpperLinearLimit;
    };

}

#endif //EXTENSIONS_BULLET_SLIDERCONSTRAINT_H
