package com.samsungxr.asynchronous;

import com.samsungxr.GVRAndroidResource;
import com.samsungxr.GVRAndroidResource.CancelableCallback;
import com.samsungxr.GVRContext;
import com.samsungxr.GVRHybridObject;
import com.samsungxr.asynchronous.Throttler.AsyncLoaderFactory;

/**
 * This is the interface for a scheduler which executes tasks for loading GVRf resources.
 */
public interface Scheduler {
    /**
     * Schedule a load request with a callback.
     *
     * @param gvrContext
     *         The GVRf Context object.
     * @param outClass
     *         The class object of a resource to be loaded.
     * @param callback
     *         A callback object to be notified when loading is done or failed.
     * @param request
     *         A {@link GVRAndroidResource} object pointing to the resource to be loaded.
     * @param priority
     *         The priority of the task.
     */
    <OUTPUT extends GVRHybridObject, INTER>
    void registerCallback(GVRContext gvrContext,
            Class<OUTPUT> outClass,
            CancelableCallback<OUTPUT> callback,
            GVRAndroidResource request, int priority);
}