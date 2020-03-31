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
// Created by c.bozzetto on 30/05/2017.
//

#ifndef EXTENSIONS_PHYSICS_HINGECONSTRAINT_H
#define EXTENSIONS_PHYSICS_HINGECONSTRAINT_H

#include "physics_constraint.h"
#include <glm/vec3.hpp>

namespace sxr {

    class PhysicsHingeConstraint : public PhysicsConstraint
    {
    public:
        virtual ~PhysicsHingeConstraint() {}

        virtual void setLimits(float lower, float upper) = 0;

        virtual float getLowerLimit() const = 0;

        virtual float getUpperLimit() const = 0;

        int getConstraintType() const { return PhysicsConstraint::hingeConstraint; }

        virtual const glm::vec3& getHingeAxis() = 0;

        virtual float getBreakingImpulse() const = 0;

        virtual void setBreakingImpulse(float impulse) = 0;
    };

}

#endif //EXTENSIONS_PHYSICS_HINGECONSTRAINT_H
