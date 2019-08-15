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

#ifndef PHYSICS_JOINTMOTOR_H_
#define PHYSICS_JOINTMOTOR_H_

#include "physics_constraint.h"

namespace sxr {

class PhysicsJointMotor :  public PhysicsConstraint
{
 public:

	PhysicsJointMotor() : PhysicsConstraint() { }
	virtual ~PhysicsJointMotor() {}

    virtual int getConstraintType() const { return PhysicsConstraint::jointMotor; }
    virtual void setVelocityTarget(int dof, float v) = 0;
	virtual void setVelocityTarget(float x, float y, float z) = 0;
	virtual void setPositionTarget(int dof, float p) = 0;
	virtual void setPositionTarget(float px, float py, float pz) = 0;
	virtual void setPositionTarget(float px, float py, float pz, float pw) = 0;
};

}

#endif /* PHYSICS_JOINTMOTOR_H_ */
