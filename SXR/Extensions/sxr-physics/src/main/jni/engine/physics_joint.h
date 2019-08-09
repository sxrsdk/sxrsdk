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

/***************************************************************************
 * Represents a physics 2D or 3D rigid body
 ***************************************************************************/

#ifndef PHYSICS_JOINT_H_
#define PHYSICS_JOINT_H_

#include "objects/node.h"
#include "objects/components/component.h"
#include "objects/components/transform.h"
#include "physics_collidable.h"

namespace sxr {
class PhysicsWorld;

class PhysicsJoint :  public PhysicsCollidable
{
 public:

	PhysicsJoint(float mass, int numBones) : PhysicsCollidable(PhysicsJoint::getComponentType()) {}
	PhysicsJoint(PhysicsJoint* parent, int boneID, float mass) : PhysicsCollidable(PhysicsJoint::getComponentType()) {}

    virtual ~PhysicsJoint() {}

	static long long getComponentType() {return COMPONENT_TYPE_PHYSICS_JOINT; }

	virtual int getBoneID() = 0;

	virtual void applyTorque(float x, float y, float z) = 0;

	virtual void applyTorque(float t) = 0;

};

}

#endif /* PHYSICS_JOINT_H_ */
