package com.samsungxr.asynchronous;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAndroidResource.CancelableCallback;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.asynchronous.Throttler.AsyncLoaderFactory;

/**
 * This is the interface for a scheduler which executes tasks for loading SXRf resources.
 */
public interface Scheduler {
    /**
     * Schedule a load request with a callback.
     *
     * @param gvrContext
     *         The SXRf Context object.
     * @param outClass
     *         The class object of a resource to be loaded.
     * @param callback
     *         A callback object to be notified when loading is done or failed.
     * @param request
     *         A {@link SXRAndroidResource} object pointing to the resource to be loaded.
     * @param priority
     *         The priority of the task.
     */
    <OUTPUT extends SXRHybridObject, INTER>
    void registerCallback(SXRContext gvrContext,
            Class<OUTPUT> outClass,
            CancelableCallback<OUTPUT> callback,
            SXRAndroidResource request, int priority);
}