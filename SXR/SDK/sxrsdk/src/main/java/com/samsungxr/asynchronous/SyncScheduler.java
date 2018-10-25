package com.samsungxr.asynchronous;

import java.io.IOException;
import java.util.Map;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAndroidResource.CancelableCallback;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.asynchronous.Throttler.AsyncLoader;
import com.samsungxr.asynchronous.Throttler.AsyncLoaderFactory;
import com.samsungxr.utility.Log;

/**
 * This is an implementation of the resource loading scheduler, which
 * blocks until each resource is loaded. It is mainly used as an example
 * of implementing a scheduler and for debugging purposes.
 */
public class SyncScheduler implements Scheduler {
    private static final String TAG = Log.tag(SyncScheduler.class);

    /*
     * Singleton
     */

    private static SyncScheduler mInstance;

    public static SyncScheduler get() {
        if (mInstance != null) {
            return mInstance;
        }

        synchronized (SyncScheduler.class) {
            mInstance = new SyncScheduler();
        }

        return mInstance;
    }

    private SyncScheduler() {
    }

    /*
     * Scheduler
     */

    @Override
    public <OUTPUT extends SXRHybridObject, INTER> void registerCallback(SXRContext gvrContext,
            Class<OUTPUT> outClass,
            CancelableCallback<OUTPUT> callback, SXRAndroidResource request,
            int priority) {
        @SuppressWarnings("unchecked")
        AsyncLoaderFactory<OUTPUT, INTER> factory = (AsyncLoaderFactory<OUTPUT, INTER>) getFactories().get(outClass);
        if (factory == null) {
            callback.failed(new IOException("Cannot find loader factory"), request);
            return;
        }

        AsyncLoader<OUTPUT, INTER> loader =
                factory.threadProc(gvrContext, request, callback, priority);

        // Run the loader synchronously
        loader.run();
    }

    private Map<Class<? extends SXRHybridObject>, AsyncLoaderFactory<? extends SXRHybridObject, ?>> getFactories() {
        return AsyncManager.get().getFactories();
    }
}
