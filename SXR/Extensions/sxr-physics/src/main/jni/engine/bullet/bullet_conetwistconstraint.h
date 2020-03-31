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
// Created by c.bozzetto on 06/06/2017.
//

#ifndef EXTENSIONS_BULLET_CONETWISTCONSTRAINT_H
#define EXTENSIONS_BULLET_CONETWISTCONSTRAINT_H

#include "../physics_conetwistconstraint.h"
#include "../physics_collidable.h"
#include <glm/glm.hpp>
#include <glm/mat3x3.hpp>

class btConeTwistConstraint;
namespace sxr {

    class PhysicsRigidBody;
    class BulletRigidBody;

    class BulletConeTwistConstraint : public PhysicsConeTwistConstraint
    {
    public:
        explicit BulletConeTwistConstraint(PhysicsCollidable* bodyA,
                                           const glm::vec3& pivotA,
                                           const glm::vec3& pivotB,
                                           const glm::vec3& coneAxis);

        BulletConeTwistConstraint(btConeTwistConstraint *constraint);

        virtual ~BulletConeTwistConstraint();

        void setSwingLimit(float limit);

        float getSwingLimit() const;

        void setTwistLimit(float limit);

        float getTwistLimit() const;

        void* getUnderlying()
        {
            return this->mConeTwistConstraint;
        }

        void setBreakingImpulse(float impulse);

        float getBreakingImpulse() const;

        void updateConstructionInfo(PhysicsWorld* world);
    private:

        btConeTwistConstraint* mConeTwistConstraint;
        float      mBreakingImpulse;
        glm::vec3  mConeAxis;
        float      mSwingLimit;
        float      mTwistLimit;
    };

}

#endif //EXTENSIONS_BULLET_CONETWISTCONSTRAINT_H
