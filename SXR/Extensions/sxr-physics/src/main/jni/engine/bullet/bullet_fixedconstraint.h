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


#ifndef BULLET_FIXEDCONSTRAINT_H
#define BULLET_FIXEDCONSTRAINT_H

#include "../physics_fixedconstraint.h"
#include "../physics_collidable.h"

class btFixedConstraint;

namespace sxr {

    class PhysicsRigidBody;
    class BulletRigidBody;

    class BulletFixedConstraint : public PhysicsFixedConstraint
    {

    public:
        explicit BulletFixedConstraint(PhysicsCollidable* bodyA);

        BulletFixedConstraint(btFixedConstraint *constraint);

        virtual ~BulletFixedConstraint();

        void* getUnderlying() {
            return this->mFixedConstraint;
        }

        void setBreakingImpulse(float impulse);

        float getBreakingImpulse() const;

        void updateConstructionInfo(PhysicsWorld* world);

    private:
        btFixedConstraint* mFixedConstraint;
        PhysicsCollidable* mRigidBodyA;
        float mBreakingImpulse;
    };

}
#endif //BULLET_FIXEDCONSTRAINT_H
