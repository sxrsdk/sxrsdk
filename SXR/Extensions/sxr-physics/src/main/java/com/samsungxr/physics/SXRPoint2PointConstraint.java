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
import com.samsungxr.SXRNode;

import org.joml.Vector3f;

/**
 * Created by c.bozzetto on 19/05/2017.
 */

/**
 * Represents a constraint that restricts translation of two {@linkplain SXRRigidBody rigid bodies}
 * or {@linkplain SXRPhysicsJoint joins} to keep fixed distance from a local pivot.
 */
public class SXRPoint2PointConstraint extends SXRConstraint
{
    /**
     * Constructs new instance of point-to-point constraint.
     *
     * @param ctx       the context of the app
     * @param bodyA     the first rigid body (not the owner) in this constraint
     * @param pivotA    the pivot point (x, y and z coordinates) related to body A
     * @param pivotB    the pivot point related to body B (the owner)
     */
    public SXRPoint2PointConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA,
                                    final float pivotA[], final float pivotB[])
    {
        this(ctx, Native3DPoint2PointConstraint.ctor(bodyA.getNative(),
                                                     pivotA[0], pivotA[1], pivotA[2],
                                                     pivotB[0], pivotB[1], pivotB[2]));
        mBodyA = bodyA;
    }

    /**
     * Constructs new instance of point-to-point constraint.
     *
     * @param ctx       the context of the app
     * @param bodyA     the first rigid body (not the owner) in this constraint
     * @param pivotA    the pivot point (x, y and z coordinates) related to body A
     * @param pivotB    the pivot point related to body B (the owner)
     */
    public SXRPoint2PointConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA,
                                    final Vector3f pivotA, final Vector3f pivotB)
    {
        this(ctx, Native3DPoint2PointConstraint.ctor(bodyA.getNative(),
                                                     pivotA.x, pivotA.y, pivotA.z,
                                                     pivotB.x, pivotB.y, pivotB.z));
        mBodyA = bodyA;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRPoint2PointConstraint(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }

}

class Native3DPoint2PointConstraint
{
    static native long ctor(long rbB, float pivotAx, float pivotAy, float pivotAz,
                            float pivotBx, float pivotBy, float pivotBz);
}