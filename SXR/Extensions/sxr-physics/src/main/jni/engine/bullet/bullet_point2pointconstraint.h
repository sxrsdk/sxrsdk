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


#ifndef BULLET_POINT2POINTCONSTRAINT_H
#define BULLET_POINT2POINTCONSTRAINT_H

#include "../physics_point2pointconstraint.h"
#include "../physics_collidable.h"
#include <glm/vec3.hpp>

class btPoint2PointConstraint;
class btMultiBodyPoint2Point;

namespace sxr {

    class PhysicsRigidBody;
    class BulletRigidBody;

    class BulletPoint2PointConstraint : public PhysicsPoint2pointConstraint
    {

    public:
        BulletPoint2PointConstraint(PhysicsCollidable* bodyA, const glm::vec3& pivotA, const glm::vec3& pivotB);

        BulletPoint2PointConstraint(btPoint2PointConstraint* constraint);

        BulletPoint2PointConstraint(btMultiBodyPoint2Point* constraint);

        virtual ~BulletPoint2PointConstraint();

        virtual void* getUnderlying()
        {
            return mMBConstraint ? reinterpret_cast<void*>(mMBConstraint) : reinterpret_cast<void*>(mConstraint);
        }

        void setBreakingImpulse(float impulse);

        float getBreakingImpulse() const;

        void updateConstructionInfo(PhysicsWorld* world);

    private:
        btPoint2PointConstraint* mConstraint;
        btMultiBodyPoint2Point* mMBConstraint;
        float     mBreakingImpulse;
    };
}
#endif //BULLET_POINT2POINTCONSTRAINT_H
