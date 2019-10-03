package com.samsungxr.physics;

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

import java.util.List;

/**
 * Represents a physics component that participate in collisions.
 * <p>
 * This includes rigid bodies and joints but not constraints.
 * @see SXRRigidBody
 * @see SXRPhysicsJoint
 */
abstract class SXRPhysicsCollidable extends SXRPhysicsWorldObject
{

    protected SXRPhysicsCollidable(SXRContext gvrContext, long nativePointer)
    {
        super(gvrContext, nativePointer);
    }

    protected SXRPhysicsCollidable(SXRContext gvrContext, long nativePointer, List<NativeCleanupHandler> cleanupHandlers)
    {
        super(gvrContext, nativePointer, cleanupHandlers);
    }

    abstract public int getCollisionGroup();
}
