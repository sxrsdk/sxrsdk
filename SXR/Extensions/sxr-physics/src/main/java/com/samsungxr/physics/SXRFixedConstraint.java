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
public class SXRFixedConstraint extends SXRConstraint
{
    /**
     * Constructs new instance of fixed constraint.
     *
     * @param ctx   the context of the app
     * @param bodyA the first rigid body or joint (not the owner) in this constraint
     */
    public SXRFixedConstraint(SXRContext ctx, SXRPhysicsCollidable bodyA)
    {
        this(ctx, Native3DFixedConstraint.ctor(bodyA.getNative()));

        mBodyA = bodyA;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRFixedConstraint(SXRContext gvrContext, long nativeConstraint)
    {
        super(gvrContext, nativeConstraint);
    }
}

class Native3DFixedConstraint
{
    static native long ctor( long rbB);
}
