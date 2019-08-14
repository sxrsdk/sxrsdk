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

import org.joml.Vector3f;


/**
 * Represents a universal constraint for two {@linkplain SXRRigidBody rigid bodies} or
 * {@linkplain SXRPhysicsJoint joints} that are linked by a point related to the first one.
 * The second one is the owner of the constraint). Though it can be moved the
 * first body can be referred as "fixed" since it will keep same distance and rotation from this
 * joint point while the second is the "moving" because one can explicitly set restriction for each
 * translation and rotation axis.
 */
public class SXRUniversalConstraint extends SXRConstraint
{
    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     */
    public SXRUniversalConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA)
    {
        this(ctx, NativeUniversalConstraint.ctor(bodyA.getNative(),
                                                 0, 0, 0,
                                                 0, 0, 1,
                                                 0, 1, 0));
        mBodyA = bodyA;
    }

    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     */
    public SXRUniversalConstraint(SXRContext ctx,
                                  SXRPhysicsCollidable bodyA,
                                  final float anchor[],
                                  final float Zaxis[],
                                  final float Yaxis[])
    {
        this(ctx, NativeUniversalConstraint.ctor(bodyA.getNative(),
                                                 anchor[0], anchor[1], anchor[2],
                                                 Zaxis[0], Zaxis[1], Zaxis[2],
                                                 Yaxis[0], Yaxis[1], Yaxis[1]));
        mBodyA = bodyA;
    }

    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     */
    public SXRUniversalConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA,
                                  final Vector3f pivotA,
                                  final Vector3f Zaxis,
                                  final Vector3f Yaxis)
    {
        this(ctx, NativeUniversalConstraint.ctor(bodyA.getNative(),
                                                 pivotA.x, pivotA.y, pivotA.z,
                                                 Zaxis.x, Zaxis.y, Zaxis.z,
                                                 Yaxis.x, Yaxis.y, Yaxis.z));
        mBodyA = bodyA;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRUniversalConstraint(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }

    /**
     * Sets the lower limits for the "moving" body rotation relative to joint point.
     *
     * @param limitX the X axis lower rotation limit (in radians)
     * @param limitY the Y axis lower rotation limit (in radians)
     * @param limitZ the Z axis lower rotation limit (in radians)
     */
    public void setAngularLowerLimits(float limitX, float limitY, float limitZ)
    {
        NativeUniversalConstraint.setAngularLowerLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the lower limits for the "moving" body rotation relative to joint point.
     *
     * @return an array containing the lower rotation limits for each (X, Y and Z) axis.
     */
    public float[] getAngularLowerLimits()
    {
        return NativeUniversalConstraint.getAngularLowerLimits(getNative());
    }

    /**
     * Sets the upper limits for the "moving" body rotation relative to joint point.
     *
     * @param limitX the X axis upper rotation limit (in radians)
     * @param limitY the Y axis upper rotation limit (in radians)
     * @param limitZ the Z axis upper rotation limit (in radians)
     */
    public void setAngularUpperLimits(float limitX, float limitY, float limitZ)
    {
        NativeUniversalConstraint.setAngularUpperLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the upper limits for the "moving" body rotation relative to joint point.
     *
     * @return an array containing the upper rotation limits for each (X, Y and Z) axis.
     */
    public float[] getAngularUpperLimits()
    {
        return NativeUniversalConstraint.getAngularUpperLimits(getNative());
    }
}

class NativeUniversalConstraint
{
    static native long ctor(long rigidBodyB, float anchorX, float anchorY, float anchorZ,
                            float axis1X, float axis1Y, float axis1Z,
                            float axis2X, float axis2Y, float axis2Z);

    static native void setAngularLowerLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getAngularLowerLimits(long jconstr);

    static native void setAngularUpperLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getAngularUpperLimits(long jconstr);

}