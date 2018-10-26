package com.samsungxr;

import android.util.LongSparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A collider allows a scene object to be picked.
 * Instead of checking against all the vertices of the object's mesh,
 * a simpler collision geometry is provided to optimize picking.
 * 
 * There are different types of colliders depending on how accurate
 * you want picking to be. The SXRSphereCollider is the fastest but
 * least accurate - it picks against a sphere. You can use a SXRMeshCollider
 * to pick against a mesh which can be a simplified version of the
 * scene object's real mesh, such as the mesh bounding box.
 *
 * The SXRPicker class can cast a ray against the scene graph and
 * return a list of which collider were hit and where. The picker can
 * be attached to a scene graph node, such as the camera rig owner,
 * to automatically generate the ray from the scene object transform
 * and pick every frame. In this mode, the picker will generate
 * events indicating what was picked.
 *
 * @see SXRPicker
 * @see IPickEvents
 * @see SXRSphereCollider
 * @see SXRMeshCollider
 * @see SXRNode#attachComponent(SXRComponent)
 */
public class SXRCollider extends SXRComponent {
    private float mPickDistance = 0;
    private static final LongSparseArray<WeakReference<SXRCollider>> sColliders = new LongSparseArray<WeakReference<SXRCollider>>();
    private final static List<NativeCleanupHandler> sCleanup;
    private final static CleanupHandlerListManager sConcatenations;
    static {
        sCleanup = new ArrayList<NativeCleanupHandler>(1);
        sCleanup.add(new NativeCleanupHandler()
        {

            @Override
            public void nativeCleanup(long nativePointer) {
                synchronized (sColliders) {
                sColliders.remove(nativePointer);
            }
            }
        });
        sConcatenations = new CleanupHandlerListManager(sCleanup);
    }
    
    /**
     * Save a link between the native C++ pointer and the Java object.
     * @param nativePointer pointer to C++ Collider object
     * @see SXRCollider#lookup(long)
     */
    protected void registerNativePointer(long nativePointer) {
        synchronized (sColliders)
        {
            sColliders.put(nativePointer, new WeakReference<SXRCollider>(this));
        }
    }

    /**
     * Internal constructor for subclasses.
     * You cannot instantiate a SXRCollider, only a derivation of it.
     */
    protected SXRCollider(SXRContext context, long nativePointer)
    {
        super(context, nativePointer, sConcatenations.getUniqueConcatenation(sCleanup));
        mType = getComponentType();
        registerNativePointer(nativePointer);
    }
   
    static public long getComponentType()
    {
        return NativeCollider.getComponentType();
    }

    /**
     * Lookup a native pointer to a collider and return its Java object.
     * 
     * @param nativePointer native pointer to C++ Collider
     * @return Java SXRCollider object
     */
    static SXRCollider lookup(long nativePointer)
    {
        synchronized (sColliders)
        {
            WeakReference<SXRCollider> weakReference = sColliders.get(nativePointer);
            return weakReference == null ? null : weakReference.get();
        }
    }
    
    /**
     * Gets pick distance.
     * 
     * The pick distance defaults to zero which will
     * cause the item to be picked now matter how far
     * along the pick ray the hit is. If the distance
     * is greater than zero, the object will not be
     * picked if it is further from the ray origin
     * than the pick distance.
     * 
     * @return pick distance value
     */
    public float getPickDistance()
    {
        return mPickDistance;
    }

    /**
     * Sets the maximum distance from pick ray origin
     * before a hit is ignored.
     * 
     * The pick distance defaults to zero which will
     * cause the item to be picked now matter how far
     * along the pick ray the hit is. If the distance
     * is greater than zero, the object will not be
     * picked if it is further from the ray origin
     * than the pick distance.
     * 
     * @see #getPickDistance
     */
    public void setPickDistance(float dist)
    {
        mPickDistance = dist;
    }

    public void setOwnerObject(SXRNode owner)
    {
        SXRPicker.sFindObjectsLock.lock();
        try
        {
            super.setOwnerObject(owner);
        }
        finally
        {
            SXRPicker.sFindObjectsLock.unlock();
        }
    }

}

class NativeCollider
{
    static native long getComponentType();
}