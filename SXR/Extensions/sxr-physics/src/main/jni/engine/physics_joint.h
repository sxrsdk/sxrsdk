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

	enum JointType
	{
		baseJoint = 0,
		fixedJoint = 1,
		sphericalJoint = 2,
		revoluteJoint = 3,
		prismaticJoint = 4,
		planarJoint = 5,
	};

	PhysicsJoint(float mass, int numBones) : PhysicsCollidable(PhysicsJoint::getComponentType()) { }
	PhysicsJoint(PhysicsJoint* parent, JointType type, int boneID, float mass) : PhysicsCollidable(PhysicsJoint::getComponentType()) { }

    virtual ~PhysicsJoint() {}

	static long long getComponentType() { return COMPONENT_TYPE_PHYSICS_JOINT; }

	virtual JointType getJointType() const = 0;

	virtual int getJointIndex() const = 0;

	virtual const glm::vec3& getPivot() const = 0;

	virtual void setPivot(const glm::vec3& pivot) = 0;

	virtual PhysicsJoint* getParent() const = 0;

	virtual const glm::vec3& getAxis() const = 0;

	virtual void setAxis(const glm::vec3& axis) = 0;

	virtual void applyCentralForce(float x, float y, float z) = 0;

	virtual void applyTorque(float x, float y, float z) = 0;

	virtual void applyTorque(float t) = 0;

	virtual Skeleton* getSkeleton() = 0;

};

}

#endif /* PHYSICS_JOINT_H_ */
