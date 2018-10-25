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

package com.samsungxr.asynchronous;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAndroidResource.CancelableCallback;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMesh;
import com.samsungxr.asynchronous.Throttler.AsyncLoader;
import com.samsungxr.asynchronous.Throttler.AsyncLoaderFactory;
import com.samsungxr.asynchronous.Throttler.GlConverter;
import com.samsungxr.utility.Log;

import java.io.IOException;

/**
 * Async resource loading: meshes.
 * 
 * @since 1.6.2
 */
class AsyncMesh {

    @SuppressWarnings("unused")
    private static final String TAG = Log.tag(AsyncMesh.class);

    /*
     * The API
     */

    void loadMesh(SXRContext gvrContext,
            CancelableCallback<SXRMesh> callback, SXRAndroidResource resource,
            int priority) {
        AsyncManager.get().getScheduler().registerCallback(gvrContext, MESH_CLASS, callback, resource,
                priority);
    }

    /*
     * Singleton
     */
    

    private static final Class<SXRMesh> MESH_CLASS = SXRMesh.class;

    private static AsyncMesh sInstance = new AsyncMesh();

    /**
     * Gets the {@link AsyncMesh} singleton for loading bitmap textures.
     * @return The {@link AsyncMesh} singleton.
     */
    public static AsyncMesh get() {
        return sInstance;
    }

    private AsyncMesh() {
        AsyncManager.get().registerDatatype(MESH_CLASS,
                new AsyncLoaderFactory<SXRMesh, SXRMesh>() {
                    @Override
                    AsyncLoader<SXRMesh, SXRMesh> threadProc(
                            SXRContext gvrContext,
                            SXRAndroidResource request,
                            CancelableCallback<SXRMesh> callback,
                            int priority) {
                        return new AsyncLoadMesh(gvrContext, request, callback, priority);
                    }
                });
    }

    /*
     * The implementation
     */

    private static class AsyncLoadMesh extends AsyncLoader<SXRMesh, SXRMesh> {
        static final GlConverter<SXRMesh, SXRMesh> sConverter = new GlConverter<SXRMesh, SXRMesh>() {

            @Override
            public SXRMesh convert(SXRContext gvrContext, SXRMesh mesh) {
                return mesh;
            }
        };

        AsyncLoadMesh(SXRContext gvrContext, SXRAndroidResource request,
                CancelableCallback<SXRMesh> callback, int priority) {
            super(gvrContext, sConverter, request, callback);
        }

        @Override
        protected SXRMesh loadResource() throws InterruptedException, IOException {
            return gvrContext.getAssetLoader().loadMesh(resource);
        }
    }
}
