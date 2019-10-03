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

import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * Represents a constraint for two {@linkplain SXRRigidBody bodies} in which the first one (the
 * owner) swings constrained to a right circular conic trajectory around a vortex while the other
 * body is simply fixed to this vortex (meaning that the vortex will move if the second body moves).
 */
public class SXRConeTwistConstraint extends SXRConstraint
{
    /**
     * Construct a new instance of a conic twist constraint.
     *
     * @param ctx          the context of the app
     * @param bodyB        the second rigid body (not the owner) in this constraint
     * @param pivotA       the vortex position (x, y and z coordinates) of the conic swing relative to
     *                     fixed body (the owner)
     * @param pivotB       the vortex position (x, y and z coordinates) of the conic swing relative to
     *                     moving body (the owner)
     * @param coneAxis     a vector with the cone axis
     */
    public SXRConeTwistConstraint(SXRContext ctx, SXRPhysicsCollidable bodyB,
                           final float pivotA[], final float pivotB[],
                           final float coneAxis[])
    {
        this(ctx, Native3DConeTwistConstraint.ctor(bodyB.getNative(),
                pivotA[0], pivotA[1], pivotA[2],
                pivotB[0], pivotB[1], pivotB[2],
                coneAxis[0], coneAxis[1], coneAxis[2]));

        mBodyB = bodyB;
    }

    /**
     * Construct a new instance of a conic twist constraint.
     *
     * @param ctx          the context of the app
     * @param bodyB        the second rigid body (not the owner) in this constraint
     * @param pivotA       the vortex position (x, y and z coordinates) of the conic swing relative to
     *                     fixed body (the owner)
     * @param pivotB       the vortex position (x, y and z coordinates) of the conic swing relative to
     *                     moving body (the owner)
     * @param coneAxis     a vector with the cone axis
     */
    public SXRConeTwistConstraint(SXRContext ctx, SXRPhysicsCollidable bodyB,
                                  final Vector3f pivotA,
                                  final Vector3f pivotB,
                                  final Vector3f coneAxis)
    {
        this(ctx, Native3DConeTwistConstraint.ctor(bodyB.getNative(),
                                                   pivotA.x, pivotA.y, pivotA.z,
                                                   pivotB.x, pivotB.y, pivotB.z,
                                                   coneAxis.x, coneAxis.y, coneAxis.z));

        mBodyB = bodyB;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRConeTwistConstraint(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }

    /**
     * Sets the swinging limit (cone aperture) for the swinging body.
     *
     * @param limit the angular swinging limit in radians
     */
    public void setSwingLimit(float limit)
    {
        Native3DConeTwistConstraint.setSwingLimit(getNative(), limit);
    }

    /**
     * Gets the swinging limit (cone aperture) for the swinging body.
     *
     * @return the angular swinging limit in radians
     */
    public float getSwingLimit() {
        return Native3DConeTwistConstraint.getSwingLimit(getNative());
    }

    /**
     * Sets the twisting limit for the swinging body.
     *
     * @param limit the angular twisting limit in radians
     */
    public void setTwistLimit(float limit)
    {
        Native3DConeTwistConstraint.setTwistLimit(getNative(), limit);
    }

    /**
     * Gets the twisting limit for the swinging body.
     *
     * @return the angular twisting limit in radians.
     */
    public float getTwistLimit() {
        return Native3DConeTwistConstraint.getTwistLimit(getNative());
    }
}

class Native3DConeTwistConstraint
{
    static native long ctor(long rigidBody,
                            float pivotAx, float pivotAy, float pivotAz,
                            float pivotBx, float pivotBy, float pivotBz,
                            float axisX, float axisY, float axisZ);

    static native void setSwingLimit(long jconstraint, float limit);

    static native float getSwingLimit(long jconstraint);

    static native void setTwistLimit(long jconstraint, float limit);

    static native float getTwistLimit(long jconstraint);
}
