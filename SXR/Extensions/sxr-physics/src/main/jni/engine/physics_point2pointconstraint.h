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

#ifndef PHYSICS_POINT2POINT_CONSTRAINT_H_
#define PHYSICS_POINT2POINT_CONSTRAINT_H_

#include "physics_constraint.h"

namespace sxr {

class PhysicsRigidBody;

class PhysicsPoint2pointConstraint : public PhysicsConstraint {
public:

    virtual ~PhysicsPoint2pointConstraint() {}

    int getConstraintType() const { return PhysicsConstraint::point2pointConstraint; }
};

}

#endif /* PHYSICS_POINT2POINT_CONSTRAINT_H_ */
