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
// Created by c.bozzetto on 09/06/2017.
//

#ifndef EXTENSIONS_BULLET_UNIVERSALCONSTRAINT_H
#define EXTENSIONS_BULLET_UNIVERSALCONSTRAINT_H

#include "../physics_universalconstraint.h"
#include "../physics_collidable.h"
#include <glm/vec3.hpp>
#include <glm/mat3x3.hpp>

class btUniversalConstraint;
namespace sxr {

    class PhysicsRigidBody;
    class BulletRigidBody;

    class BulletUniversalConstraint : public PhysicsUniversalConstraint
    {
    public:
        BulletUniversalConstraint(PhysicsCollidable* bodyA, const glm::vec3& pivotB, const glm::vec3& axis1, const glm::vec3& axis2);

        BulletUniversalConstraint(btUniversalConstraint *constraint);

        virtual ~BulletUniversalConstraint();

        virtual void setAngularLowerLimits(float limitX, float limitY, float limitZ);

        virtual const glm::vec3& getAngularLowerLimits() const;

        virtual void setAngularUpperLimits(float limitX, float limitY, float limitZ);

        virtual const glm::vec3& getAngularUpperLimits() const;

        void* getUnderlying() { return mConstraint;}

        virtual void setBreakingImpulse(float impulse);

        virtual float getBreakingImpulse() const;

        virtual void updateConstructionInfo(PhysicsWorld* world);

    private:

        btUniversalConstraint*  mConstraint;
        float             mBreakingImpulse;
        mutable glm::vec3 mAngularLowerLimits;
        mutable glm::vec3 mAngularUpperLimits;
        glm::vec3         mAxis1;
        glm::vec3         mAxis2;
    };

}

#endif //EXTENSIONS_BULLET_GENERIC6DOFCONSTRAINT_H
