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

package com.samsungxr.physics;

import com.samsungxr.SXRContext;

/**
 * Represents a constraint that forces two {@linkplain SXRRigidBody rigid bodies}
 * or ({@linkplain SXRPhysicsJoint} to keep same the
 * distance and same rotation in respect to each other.
 * The constraint should be attached to the second body or joint
 * (bodyB)
 */
public class SXRPhysicsJointMotor extends SXRConstraint
{
    /**
     * Constructs new joint motor.
     *
     * @param ctx   the context of the app
     */
    public SXRPhysicsJointMotor(SXRContext ctx)
    {
        this(ctx, Float.MAX_VALUE);
    }

    /**
     * Constructs new joint motor.
     *
     * @param ctx   the context of the app
     */
    public SXRPhysicsJointMotor(SXRContext ctx, float maxImpulse)
    {
        super(ctx, NativePhysicsJointMotor.create(maxImpulse));
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRPhysicsJointMotor(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }

    public void setVelocityTarget(int dof, float v)
    {
        NativePhysicsJointMotor.setVelocityTarget(getNative(), dof, v);
    }

    public void setVelocityTarget(float vx, float vy, float vz)
    {
        NativePhysicsJointMotor.setVelocityTarget3(getNative(), vx, vy, vz);
    }

    public void setPositionTarget(float px, float py, float pz)
    {
        NativePhysicsJointMotor.setPositionTarget3(getNative(), px, py, pz);
    }

    public void setPositionTarget(float px, float py, float pz, float pw)
    {
        NativePhysicsJointMotor.setPositionTarget4(getNative(), px, py, pz, pw);
    }


    public void setPositionTarget(int dof, float p)
    {
        NativePhysicsJointMotor.setPositionTarget(getNative(), dof, p);
    }
}

class NativePhysicsJointMotor
{
    static native long create(float maxImpulse);
    static native void setVelocityTarget(long motor, int dof, float v);
    static native void setVelocityTarget3(long motor, float vx, float vy, float vz);
    static native void setPositionTarget(long motor, int dof, float p);
    static native void setPositionTarget3(long motor, float px, float py, float pz);
    static native void setPositionTarget4(long motor, float px, float py, float pz, float pw);
}
