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

#ifndef EXTENSIONS_PHYSICS_CONSTRAINT_H
#define EXTENSIONS_PHYSICS_CONSTRAINT_H

#include "../objects/node.h"
#include "physics_world.h"

namespace sxr {

    class PhysicsConstraint : public Component
    {
    public:
        PhysicsConstraint() : Component(PhysicsConstraint::getComponentType()){}

        virtual ~PhysicsConstraint() {}

        static long long getComponentType()
        {
            return COMPONENT_TYPE_PHYSICS_CONSTRAINT;
        }

        virtual int   getConstraintType() const = 0;
        virtual void* getUnderlying() = 0;
        virtual void  setBreakingImpulse(float impulse) = 0;
        virtual float getBreakingImpulse() const = 0;
        virtual void  updateConstructionInfo(PhysicsWorld*) = 0;
        virtual void  addChildComponent(Component* constraint)
        {
            mConstraints.push_back(static_cast<PhysicsConstraint*>(constraint));
        }

        virtual void removeChildComponent(Component* constraint)
        {
            mConstraints.erase(std::remove(mConstraints.begin(), mConstraints.end(),
                                           static_cast<PhysicsConstraint*>(constraint)), mConstraints.end());
        }

        int getNumChildren() { return mConstraints.size(); }
        PhysicsConstraint* getChildAt(int i) { return mConstraints.at(i); }


        enum ConstraintType
        {
            fixedConstraint = 1,
            point2pointConstraint = 2,
            sliderConstraint = 3,
            hingeConstraint = 4,
            coneTwistConstraint = 5,
            genericConstraint = 6,
            universalConstraint = 7,
            jointMotor = 8
        };

        std::vector<PhysicsConstraint*> mConstraints;
    };

}
#endif //EXTENSIONS_PHYSICS_CONSTRAINT_H
