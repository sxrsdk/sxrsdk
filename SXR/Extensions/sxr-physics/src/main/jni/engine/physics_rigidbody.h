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

#ifndef PHYSICS_RIGIDBODY_H_
#define PHYSICS_RIGIDBODY_H_

#include "objects/node.h"
#include "objects/components/component.h"
#include "objects/components/transform.h"
#include "physics_collidable.h"

namespace sxr {
class PhysicsWorld;

class PhysicsRigidBody : public PhysicsCollidable
{
 public:
	enum SimulationType
	{
		DYNAMIC = 0,
		KINEMATIC = 1,
		STATIC = 2
	};

	PhysicsRigidBody() : PhysicsCollidable(PhysicsRigidBody::getComponentType()) {}

	virtual ~PhysicsRigidBody() {}

	static long long getComponentType() { return COMPONENT_TYPE_PHYSICS_RIGID_BODY; }

	virtual void setSimulationType(SimulationType t) = 0;
	virtual SimulationType getSimulationType() const = 0;
    virtual void setCenterOfMass(Transform* t) = 0;
	virtual void getRotation(float &w, float &x, float &y, float &z) = 0;
    virtual void getTranslation(float &x, float &y, float &z) = 0;
    virtual void applyCentralForce(float x, float y, float z) = 0;
	virtual void applyForce(float force_x, float force_y, float force_z,
			float rel_pos_x, float rel_pos_y, float rel_pos_z) = 0;
	virtual void applyCentralImpulse(float x, float y, float z) = 0;
    virtual void applyImpulse(float impulse_x, float impulse_y, float impulse_z,
                            float rel_pos_x, float rel_pos_y, float rel_pos_z) = 0;
    virtual void applyTorque(float x, float y, float z) = 0;
    virtual void applyTorqueImpulse(float x, float y, float z) = 0;

	virtual void setGravity(float x, float y, float z)  = 0;
	virtual void setDamping(float linear, float angular)  = 0;
	virtual void setLinearVelocity(float x, float y, float z)  = 0;
	virtual void setAngularVelocity(float x, float y, float z)  = 0;
	virtual void setAngularFactor(float x, float y, float z)  = 0;
	virtual void setLinearFactor(float x, float y, float z)  = 0;
	virtual void setRestitution(float n)  = 0;

	virtual void setSleepingThresholds(float linear, float angular)  = 0;
	virtual void setCcdMotionThreshold(float n)  = 0;
	virtual void setCcdSweptSphereRadius(float n)  = 0;
	virtual void setContactProcessingThreshold(float n)  = 0;

	virtual void setIgnoreCollisionCheck( PhysicsRigidBody* collisionObj, bool ignore)  = 0;

	virtual void getGravity(float *v3) const = 0;
	virtual void getLinearVelocity(float *v3) const = 0;
	virtual void getAngularVelocity(float *v3) const = 0;
	virtual void getAngularFactor(float *v3) const = 0;
	virtual void getLinearFactor(float *v3) const = 0;
	virtual void getDamping(float &angular, float &linear) const = 0;

	virtual float getRestitution() const = 0;
	virtual float getCcdMotionThreshold() const = 0;
	virtual float getCcdSweptSphereRadius() const = 0;
	virtual float getContactProcessingThreshold() const = 0;

    virtual void reset(bool rebuildCollider = true) = 0;
};

}

#endif /* PHYSICS_RIGIDBODY_H_ */
