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
 * Created by c.bozzetto on 06/06/2017.
 */

/**
 * Represents a constraint for two {@linkplain SXRRigidBody bodies} in which the first one (the
 * owner) swings constrained to a right circular conic trajectory around a vortex while the other
 * body is simply fixed to this vortex (meaning that the vortex will move if the second body moves).
 */
public class SXRConeTwistConstraint extends SXRConstraint {
    /**
     * Construct a new instance of a conic twist constraint.
     *
     * @param ctx          the context of the app
     * @param bodyB        the second rigid body (not the owner) in this constraint
     * @param vortex       the vortex position (x, y and z coordinates) of the conic swing relative to
     *                     first body (the owner)
     * @param bodyRotation a vector containing the elements of the 3x3 rotation matrix for the
     *                     swinging body
     * @param coneRotation a vector containing the elements of the 3x3 rotation matrix for the conic
     *                     trajectory
     */
    public SXRConeTwistConstraint(SXRContext ctx, SXRPhysicsWorldObject bodyB, final float vortex[],
                           final float bodyRotation[], final float coneRotation[])
    {
        this(ctx, Native3DConeTwistConstraint.ctor(bodyB.getNative(), vortex,
                bodyRotation, coneRotation));

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
    static native long ctor(long rigidBody, final float pivot[], final float bodyRotation[],
                            final float coneRotation[]);

    static native void setSwingLimit(long jconstraint, float limit);

    static native float getSwingLimit(long jconstraint);

    static native void setTwistLimit(long jconstraint, float limit);

    static native float getTwistLimit(long jconstraint);
}
