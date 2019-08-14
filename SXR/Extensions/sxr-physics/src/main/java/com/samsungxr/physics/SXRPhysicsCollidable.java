package com.samsungxr.physics;

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

import java.util.List;

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

}
