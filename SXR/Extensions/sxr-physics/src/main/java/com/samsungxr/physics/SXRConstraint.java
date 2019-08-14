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

import com.samsungxr.IComponentGroup;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base class to represent a constraint for the movement of two
 * {@linkplain SXRRigidBody rigid bodies} or {@linkplain SXRPhysicsJoint joints}
 * <p>.
 * After created anf fully configured a constraint must be attached to a
 * {@linkplain com.samsungxr.SXRNode node} containing a rigid body that will become
 * the owner of this constraint (body B).
 */
abstract class SXRConstraint extends SXRPhysicsWorldObject
    {
    static final int fixedConstraintId = 1;
    static final int point2pointConstraintId = 2;
    static final int sliderConstraintId = 3;
    static final int hingeConstraintId = 4;
    static final int coneTwistConstraintId = 5;
    static final int genericConstraintId = 6;
    static final int universalConstraintId = 7;
    static final int jointMotorId = 8;

    protected SXRPhysicsCollidable mBodyA = null;
    protected SXRPhysicsCollidable mBodyB = null;

    protected SXRConstraint(SXRContext gvrContext, long nativePointer)
    {
        super(gvrContext, nativePointer);
    }

    protected SXRConstraint(SXRContext gvrContext, long nativePointer, List<NativeCleanupHandler> cleanupHandlers)
    {
        super(gvrContext, nativePointer, cleanupHandlers);
    }

    @Override
    public void onAttach(SXRNode newOwner)
    {
        mBodyB = (SXRRigidBody) newOwner.getComponent(SXRRigidBody.getComponentType());
        if (mBodyB == null)
        {
            mBodyB = (SXRPhysicsJoint) newOwner.getComponent(SXRPhysicsJoint.getComponentType());
            if (mBodyB == null)
            {
                throw new UnsupportedOperationException("There is no rigid body or joint attached to owner object.");
            }
        }
        super.onAttach(newOwner);
    }

    @Override
    protected void addToWorld(SXRWorld world)
    {
        if (world != null)
        {
            world.addConstraint(this);
        }
    }

    @Override
    protected void removeFromWorld(SXRWorld world)
    {
        if (world != null)
        {
            world.removeConstraint(this);
        }
    }

    /**
     * Sets the breaking impulse for a constraint.
     *
     * @param impulse the breaking impulse value.
     */
    public void setBreakingImpulse(float impulse)
    {
        Native3DConstraint.setBreakingImpulse(getNative(), impulse);
    }

    /**
     * Gets the breaking impulse for a constraint.
     *
     * @return the breaking impulse value for the constraint.
     */
    public float getBreakingImpulse()
    {
        return Native3DConstraint.getBreakingImpulse(getNative());
    }

    static public long getComponentType()
    {
        return Native3DConstraint.getComponentType();
    }
}

class Native3DConstraint
{
    static native long getComponentType();

    static native int getConstraintType(long nativeConstraint);

    static native void setBreakingImpulse(long nativeConstraint, float impulse);

    static native float getBreakingImpulse(long nativeConstraint);

    static native void addChildComponent(long nativeConstraint, long nativeChild);

    static native void removeChildComponent(long nativeConstraint, long nativeChild);
}
