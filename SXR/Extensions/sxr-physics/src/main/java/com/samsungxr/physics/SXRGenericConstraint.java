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
import com.samsungxr.SXRTransform;

import org.joml.Vector3f;

/**
 * Created by c.bozzetto on 09/06/2017.
 */

/**
 * Represents a generic constraint for two {@linkplain SXRRigidBody rigid bodies} or
 * {@linkplain SXRPhysicsJoint joints} that are linked by a point related to the first one.
 * The second one is the owner of the constraint). Though it can be moved the
 * first body can be referred as "fixed" since it will keep same distance and rotation from this
 * joint point while the second is the "moving" because one can explicitly set restriction for each
 * translation and rotation axis.
 */
public class SXRGenericConstraint extends SXRConstraint
{
    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     * @param pivotA     the pivot point (x, y and z coordinates) in this constraint
     *                   relative to "fixed" body
     * @param pivotB     the pivot point (x, y and z coordinates) in this constraint
     *                   relative to owner
     */
    public SXRGenericConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA, final float pivotA[], final float pivotB[])
    {
        this(ctx, Native3DGenericConstraint.ctor(bodyA.getNative(),
                                                 pivotA[0], pivotA[1], pivotA[2],
                                                 pivotB[0], pivotB[1], pivotB[2]));
        mBodyA = bodyA;
    }

    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     * @param pivotA     the pivot point (x, y and z coordinates) in this constraint
     *                   relative to "fixed" body
     * @param pivotB     the pivot point (x, y and z coordinates) in this constraint
     *                   relative to owner
     */
    public SXRGenericConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA, final Vector3f pivotA, final Vector3f pivotB)
    {
        this(ctx, Native3DGenericConstraint.ctor(bodyA.getNative(),
                                                 pivotA.x, pivotA.y, pivotA.z,
                                                 pivotB.x, pivotB.y, pivotB.z));
        mBodyA = bodyA;
    }

    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     * @param pivotB     the pivot point (x, y and z coordinates) in this constraint
     *                   relative to owner
     */
    public SXRGenericConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA, final Vector3f pivotB)
    {
        this(ctx, Native3DGenericConstraint.ctor(bodyA.getNative(),
                                                 0, 0, 0,
                                                 pivotB.x, pivotB.y, pivotB.z));
        mBodyA = bodyA;
    }

    /**
     * Construct a new instance of a generic constraint.
     *
     * @param ctx        the context of the app
     * @param bodyA      the "fixed" body (not the owner) in this constraint
     */
    public SXRGenericConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA)
    {
        this(ctx, Native3DGenericConstraint.ctor(bodyA.getNative(),
                                                 0, 0, 0,
                                                 0, 0, 0));
        mBodyA = bodyA;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRGenericConstraint(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }

    /**
     * Sets the lower limits for the "moving" body translation relative to joint point.
     *
     * @param limitX the X axis lower translation limit
     * @param limitY the Y axis lower translation limit
     * @param limitZ the Z axis lower translation limit
     */
    public void setLinearLowerLimits(float limitX, float limitY, float limitZ)
    {
        Native3DGenericConstraint.setLinearLowerLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the lower limits for the "moving" body translation relative to joint point.
     *
     * @return an array containing the lower translation limits for each (X, Y and Z) axis.
     */
    public float[] getLinearLowerLimits()
    {
        return Native3DGenericConstraint.getLinearLowerLimits(getNative());
    }

    /**
     * Sets the upper limits for the "moving" body translation relative to joint point.
     *
     * @param limitX the X upper lower translation limit
     * @param limitY the Y upper lower translation limit
     * @param limitZ the Z upper lower translation limit
     */
    public void setLinearUpperLimits(float limitX, float limitY, float limitZ)
    {
        Native3DGenericConstraint.setLinearUpperLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the upper limits for the "moving" body translation relative to joint point.
     *
     * @return an array containing the upper translation limits for each (X, Y and Z) axis.
     */
    public float[] getLinearUpperLimits()
    {
        return Native3DGenericConstraint.getLinearUpperLimits(getNative());
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
        if ((limitX < -Math.PI) ||
            (limitY < -Math.PI / 2) ||
            (limitZ < -Math.PI))
        {
            throw new IllegalArgumentException("Angular limits out of range");
        }
        Native3DGenericConstraint.setAngularLowerLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the lower limits for the "moving" body rotation relative to joint point.
     *
     * @return an array containing the lower rotation limits for each (X, Y and Z) axis.
     */
    public float[] getAngularLowerLimits()
    {
        return Native3DGenericConstraint.getAngularLowerLimits(getNative());
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
        if ((limitX > Math.PI) ||
            (limitY > Math.PI / 2) ||
            (limitZ > Math.PI))
        {
            throw new IllegalArgumentException("Angular limits out of range");
        }        Native3DGenericConstraint.setAngularUpperLimits(getNative(), limitX, limitY, limitZ);
    }

    /**
     * Gets the upper limits for the "moving" body rotation relative to joint point.
     *
     * @return an array containing the upper rotation limits for each (X, Y and Z) axis.
     */
    public float[] getAngularUpperLimits()
    {
        return Native3DGenericConstraint.getAngularUpperLimits(getNative());
    }
}

class Native3DGenericConstraint
{
    static native long ctor(long rigidBodyA,
                            float pivotAx, float pivotAy, float pivotAz,
                            float pivotBx, float pivotBy, float pivotBz);

    static native void setLinearLowerLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getLinearLowerLimits(long jconstr);

    static native void setLinearUpperLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getLinearUpperLimits(long jconstr);

    static native void setAngularLowerLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getAngularLowerLimits(long jconstr);

    static native void setAngularUpperLimits(long jconstr, float limX, float limY, float limZ);

    static native float[] getAngularUpperLimits(long jconstr);

}