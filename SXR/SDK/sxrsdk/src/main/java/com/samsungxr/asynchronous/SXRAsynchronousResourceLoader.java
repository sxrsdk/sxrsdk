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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.samsungxr.SXRCompressedImage;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAndroidResource.CancelableCallback;
import com.samsungxr.SXRAndroidResource.CompressedTextureCallback;
import com.samsungxr.SXRAndroidResource.TextureCallback;
import com.samsungxr.SXRAtlasInformation;
import com.samsungxr.SXRBitmapImage;
import com.samsungxr.SXRCompressedCubemapImage;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRCubemapImage;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRImage;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRTexture;
import com.samsungxr.utility.Log;
import com.samsungxr.utility.ResourceCache;
import com.samsungxr.utility.Threads;

import android.graphics.Bitmap;

/**
 * Internal API for asynchronous resource loading.
 * 
 * You will normally call into this class through
 * {@link SXRContext#loadCompressedTexture(SXRAndroidResource.CompressedTextureCallback, SXRAndroidResource)}
 * or
 * {@link SXRContext#loadBitmapTexture(SXRAndroidResource.BitmapTextureCallback, SXRAndroidResource)}
 * or
 * {@link SXRContext#loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource)}
 * .
 * 
 * @since 1.6.1
 */
public class SXRAsynchronousResourceLoader {

    /**
     * Get device parameters and so on.
     * 
     * This is an internal method, public only so it can be called across
     * package boundaries. Calling it from user code is both harmless and
     * pointless.
     */
    public static void setup(SXRContext gvrContext) {
        AsyncBitmapTexture.setup(gvrContext);
    }

    /**
     * Load a compressed texture asynchronously.
     * 
     * This is the implementation of
     * {@link SXRContext#loadCompressedTexture(SXRAndroidResource.CompressedTextureCallback, SXRAndroidResource)}
     * : it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Stream containing a compressed texture
     * @throws IllegalArgumentException
     *             If {@code gvrContext} or {@code callback} parameters are
     *             {@code null}
     */
    public static void loadCompressedTexture(final SXRContext gvrContext,
            ResourceCache<SXRImage> textureCache,
            final CompressedTextureCallback callback,
            final SXRAndroidResource resource) {
        loadCompressedTexture(gvrContext, textureCache, callback, resource,
                              SXRCompressedImage.DEFAULT_QUALITY);
    }

    /**
     * Load a compressed texture asynchronously.
     * 
     * This is the implementation of
     * {@link SXRContext#loadCompressedTexture(SXRAndroidResource.CompressedTextureCallback, SXRAndroidResource)}
     * : it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link SXRAndroidResource} parameter eliminates six overloads.
     * @param quality
     *            Speed/quality tradeoff: should be one of
     *            {@link SXRCompressedImage#SPEED},
     *            {@link SXRCompressedImage#BALANCED}, or
     *            {@link SXRCompressedImage#QUALITY}, but other values are
     *            'clamped' to one of the recognized values.
     * @throws IllegalArgumentException
     *             If {@code gvrContext} or {@code callback} parameters are
     *             {@code null}
     */
    public static void loadCompressedTexture(final SXRContext gvrContext,
            final ResourceCache<SXRImage> textureCache,
            final CompressedTextureCallback callback,
            final SXRAndroidResource resource, final int quality)
            throws IllegalArgumentException {
        validateCallbackParameters(gvrContext, callback, resource);

        final SXRImage cached = textureCache == null ? null : textureCache
                .get(resource);
        if (cached != null) {
            Log.v("ASSET", "Texture: %s loaded from cache", cached.getFileName());
            gvrContext.runOnGlThread(new Runnable() {

                @Override
                public void run() {
                    callback.loaded(cached, resource);
                }
            });
        }
        else
        {
            CompressedTextureCallback actualCallback = textureCache == null ? callback
                    : ResourceCache.wrapCallback(textureCache, callback);
            AsyncCompressedTexture.loadTexture(gvrContext,
                                           CancelableCallbackWrapper.wrap(SXRCompressedImage.class, actualCallback),
                                           resource, SXRContext.LOWEST_PRIORITY);
        }
    }

    /**
     * Load a (compressed or bitmapped) texture asynchronously.
     * 
     * This is the implementation of
     * {@link SXRContext#loadTexture(com.samsungxr.SXRAndroidResource.TextureCallback, SXRAndroidResource, int, int)}
     * - it will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link SXRAndroidResource} parameter eliminates six overloads.
     * @param priority
     *            A value {@literal >=} {@link SXRContext#LOWEST_PRIORITY} and
     *            {@literal <=} {@link SXRContext#HIGHEST_PRIORITY}
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <}
     *             {@link SXRContext#LOWEST_PRIORITY} or {@literal >}
     *             {@link SXRContext#HIGHEST_PRIORITY}, or any of the other
     *             parameters are {@code null}.
     */
        public static void loadTexture(final SXRContext gvrContext,
            final ResourceCache<SXRImage> textureCache,
            final CancelableCallback<SXRImage> callback,
            final SXRAndroidResource resource,
            final int priority,
            final int quality) {
        Threads.spawn(new Runnable() {
            @Override
            public void run() {
                validateCallbackParameters(gvrContext, callback, resource);

                final SXRImage cached = textureCache == null ? null
                        : textureCache.get(resource);
                if (cached != null) {
                    Log.v("ASSET", "Texture: %s loaded from cache", cached.getFileName());
                    callback.loaded(cached, resource);
                } else {
                    // 'Sniff' out compressed textures on a thread from the
                    // thread-pool
                    final SXRCompressedTextureLoader loader = resource
                            .getCompressedLoader();
                    if (loader != null) {
                        CancelableCallback<SXRImage> actualCallback = textureCache == null
                                ? callback
                                : textureCache.wrapCallback(callback);

                        AsyncCompressedTexture.loadTexture(gvrContext,
                                CancelableCallbackWrapper.wrap(
                                        SXRCompressedImage.class,
                                        actualCallback),
                                resource, priority);
                    } else {
                        // We don't have a compressed texture: pass to
                        // AsyncBitmapTexture code
                        CancelableCallback<SXRImage> actualCallback = textureCache == null
                                ? callback
                                : textureCache.wrapCallback(callback);

                        AsyncBitmapTexture.loadTexture(gvrContext,
                                CancelableCallbackWrapper.wrap(
                                        SXRBitmapImage.class, actualCallback),
                                resource, priority);
                    }
                }
            }
        });
    }

    /**
     * Load a (compressed or bitmapped) texture asynchronously.
     * 
     * This is the implementation of
     * {@link SXRAssetLoader#loadTexture(SXRAndroidResource, int, int)} - it
     * will usually be more convenient (and more efficient) to call that
     * directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link SXRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code SXRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @param quality
     *            The compressed texture {@link SXRCompressedImage#mQuality
     *            quality} parameter: should be one of
     *            {@link SXRCompressedImage#SPEED},
     *            {@link SXRCompressedImage#BALANCED}, or
     *            {@link SXRCompressedImage#QUALITY}, but other values are
     *            'clamped' to one of the recognized values. Please note that
     *            this (currently) only applies to compressed textures; normal
     *            {@linkplain SXRBitmapImage bitmapped textures} don't take a
     *            quality parameter.
     * @return A {@link Future} that you can pass to methods like
     *         {@link SXRShaderData#setMainTexture(Future)}
     */
    public static SXRTexture loadFutureTexture(SXRContext gvrContext,
            ResourceCache<SXRImage> textureCache,
            SXRAndroidResource resource, int priority, int quality) {
        SXRImage cached = textureCache == null ? null : textureCache
                .get(resource);
        SXRTexture tex = new SXRTexture(gvrContext);
        if (cached != null)
        {
            Log.v("ASSET", "Future Texture: %s loaded from cache", cached.getFileName());
            tex.setImage(cached);
        }
        else
        {
            loadTexture(gvrContext, textureCache, tex, resource,
                    priority, quality);
        }
        return tex;
    }

    /**
     * Load a cube map texture asynchronously.
     * 
     * This is the implementation of
     * {@link SXRAssetLoader#loadCubemapTexture(SXRAndroidResource)} - it will
     * usually be more convenient (and more efficient) to call that directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param resource
     *            A steam containing a zip file which contains six bitmaps. The
     *            six bitmaps correspond to +x, -x, +y, -y, +z, and -z faces of
     *            the cube map texture respectively. The default names of the
     *            six images are "posx.png", "negx.png", "posy.png", "negx.png",
     *            "posz.png", and "negz.png", which can be changed by calling
     *            {@link SXRCubemapImage#setFaceNames(String[])}.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * @return A {@link Future} that you can pass to methods like
     *         {@link SXRShaderData#setMainTexture(Future)}
     */
    public static SXRTexture loadFutureCubemapTexture(
            SXRContext gvrContext, ResourceCache<SXRImage> textureCache,
            SXRAndroidResource resource, int priority,
            Map<String, Integer> faceIndexMap) {
        SXRTexture tex = new SXRTexture(gvrContext);
        SXRImage cached = textureCache.get(resource);
        if (cached != null)
        {
            Log.v("ASSET", "Future Texture: %s loaded from cache", cached.getFileName());
            tex.setImage(cached);
        }
        else
        {
            AsyncCubemapTexture.get().loadTexture(gvrContext,
                    CancelableCallbackWrapper.wrap(SXRCubemapImage.class, tex),
                    resource, priority, faceIndexMap);

        }
        return tex;
    }

    /**
     * Load a cubemap texture asynchronously.
     *
     * This is the implementation of
     * {@link SXRAssetLoader#loadCubemapTexture(SXRAndroidResource.TextureCallback, SXRAndroidResource, int)}
     * - it will usually be more convenient (and more efficient) to call that
     * directly.
     *
     * @param context
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link SXRAndroidResource} parameter eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * @return A {@link Future} that you can pass to methods like
     *         {@link SXRShaderData#setMainTexture(Future)}
     */
    public static void loadCubemapTexture(final SXRContext context,
                                         ResourceCache<SXRImage> textureCache,
                                         final TextureCallback callback,
                                         final SXRAndroidResource resource, int priority,
                                          Map<String, Integer> faceIndexMap)
            throws IllegalArgumentException
    {
        validatePriorityCallbackParameters(context, callback, resource,
                priority);
        final SXRImage cached = textureCache == null
                ? null
                : (SXRImage) textureCache.get(resource);
        if (cached != null)
        {
            Log.v("ASSET", "Texture: %s loaded from cache", cached.getFileName());
            context.runOnGlThread(new Runnable()
            {
                @Override
                public void run()
                {
                    callback.loaded(cached, resource);
                }
            });
        }
        else
        {
            TextureCallback actualCallback = (textureCache == null) ? callback
                    : ResourceCache.wrapCallback(textureCache, callback);
            AsyncCubemapTexture.loadTexture(context,
                    CancelableCallbackWrapper.wrap(SXRCubemapImage.class, actualCallback),
                    resource, priority, faceIndexMap);
        }
    }

    /**
     * Load a cubemap texture asynchronously.
     *
     * This is the implementation of
     * {@link SXRAssetLoader#loadCompressedCubemapTexture(SXRAndroidResource.TextureCallback, SXRAndroidResource, int)}
     * - it will usually be more convenient (and more efficient) to call that
     * directly.
     *
     * @param context
     *            The SXRF context
     * @param textureCache
     *            Texture cache - may be {@code null}
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a compressed texture. Taking a
     *            {@link SXRAndroidResource} parameter eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     */
    public static void loadCompressedCubemapTexture(final SXRContext context,
                                          ResourceCache<SXRImage> textureCache,
                                          final TextureCallback callback,
                                          final SXRAndroidResource resource, int priority,
                                          Map<String, Integer> faceIndexMap)
            throws IllegalArgumentException
    {
        validatePriorityCallbackParameters(context, callback, resource,
                                           priority);
        final SXRImage cached = textureCache == null
                ? null
                : (SXRImage) textureCache.get(resource);
        if (cached != null)
        {
            Log.v("ASSET", "Texture: %s loaded from cache", cached.getFileName());
            context.runOnGlThread(new Runnable()
            {
                @Override
                public void run()
                {
                    callback.loaded(cached, resource);
                }
            });
        }
        else
        {
            TextureCallback actualCallback = (textureCache == null) ? callback
                    : ResourceCache.wrapCallback(textureCache, callback);
            AsyncCompressedCubemapTexture.loadTexture(context, CancelableCallbackWrapper.wrap(SXRCompressedCubemapImage.class, actualCallback),
                                            resource, priority, faceIndexMap);
        }
    }

    /**
     * 
     * Load a GL mesh asynchronously.
     * 
     * This is the implementation of
     * {@link SXRAssetLoader#loadMesh(SXRAndroidResource.MeshCallback, SXRAndroidResource, int)}
     * - it will usually be more convenient to call that directly.
     * 
     * @param gvrContext
     *            The SXRF context
     * @param callback
     *            Asynchronous notifications
     * @param resource
     *            Basically, a stream containing a 3D model. Taking a
     *            {@link SXRAndroidResource} parameter eliminates six overloads.
     * @param priority
     *            A value {@literal >=} {@link SXRContext#LOWEST_PRIORITY} and
     *            {@literal <=} {@link SXRContext#HIGHEST_PRIORITY}
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <}
     *             {@link SXRContext#LOWEST_PRIORITY} or {@literal >}
     *             {@link SXRContext#HIGHEST_PRIORITY}, or any of the other
     *             parameters are {@code null}.
     * 
     * @since 1.6.2
     */
    // This method does not take a ResourceCache<SXRMeh> parameter because it
    // (indirectly) calls SXRContext.loadMesh() which 'knows about' the cache
    public static void loadMesh(SXRContext gvrContext,
            CancelableCallback<SXRMesh> callback, SXRAndroidResource resource,
            int priority) {
        validatePriorityCallbackParameters(gvrContext, callback, resource,
                priority);

        AsyncMesh.get().loadMesh(gvrContext, callback, resource, priority);
    }

    public static class FutureResource<T extends SXRHybridObject> implements
            Future<T> {

        private static final String TAG = Log.tag(FutureResource.class);

        /** Do all our synchronization on data private to this instance */
        private final Object[] lock = new Object[0];

        private T result = null;
        private Throwable error = null;
        private boolean pending = true;
        private boolean canceled = false;

        private SXRAndroidResource resource;

        private final CancelableCallback<T> callback = new CancelableCallback<T>() {

            @Override
            public void loaded(T data, SXRAndroidResource androidResource) {
                synchronized (lock) {
                    result = data;
                    pending = false;
                    lock.notifyAll();
                }
            }

            @Override
            public void failed(Throwable t, SXRAndroidResource androidResource) {
                Log.d(TAG, "failed(%s), %s", androidResource, t);
                synchronized (lock) {
                    error = t;
                    pending = false;
                    lock.notifyAll();
                }
            }

            @Override
            public boolean stillWanted(SXRAndroidResource androidResource) {
                return canceled == false;
            }
        };

        public FutureResource(SXRAndroidResource resource) {
            this.resource = resource;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            canceled = true;
            return pending;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return get(0);
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            return get(unit.toMillis(timeout));
        }

        private T get(long millis)
                throws InterruptedException, ExecutionException {
            if (!pending) {
                return result;
            }

            synchronized (lock) {
                if (pending) {                    
                    lock.wait(millis);
                }
            }

            if (canceled) {
                throw new CancellationException();
            }
            if (error != null) {
                throw new ExecutionException(error);
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean isDone() {
            return pending == false;
        }

        public SXRAndroidResource getResource() {
            return resource;
        }
    }

    /*
     * This is a wrapper to convert {@code CancelableCallback<S>} to {@code CancelableCallback<T>}
     * where T extends S.
     */
    static class CancelableCallbackWrapper<S extends SXRHybridObject, T extends S>
    implements CancelableCallback<T> {
        private CancelableCallback<S> wrapped_;

        private CancelableCallbackWrapper(CancelableCallback<S> wrapped) {
            wrapped_ = wrapped;
        }

        @Override
        public void loaded(T resource, SXRAndroidResource androidResource) {
            wrapped_.loaded(resource, androidResource);
        }

        @Override
        public void failed(Throwable t, SXRAndroidResource androidResource) {
            wrapped_.failed(t, androidResource);
        }

        @Override
        public boolean stillWanted(SXRAndroidResource androidResource) {
            return wrapped_.stillWanted(androidResource);
        }

        public static <S extends SXRHybridObject, T extends S> CancelableCallbackWrapper<S, T> wrap(
                Class<T> targetClass,
                CancelableCallback<S> wrapped) {
            return new CancelableCallbackWrapper<S, T>(wrapped);
        }
    }

    private static <T extends SXRHybridObject> void validateCallbackParameters(
            SXRContext gvrContext, SXRAndroidResource.Callback<T> callback,
            SXRAndroidResource resource) {
        if (gvrContext == null) {
            throw new IllegalArgumentException("gvrContext == null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback == null");
        }
        if (resource == null) {
            throw new IllegalArgumentException("resource == null");
        }
    }

    private static <T extends SXRHybridObject> void validatePriorityCallbackParameters(
            SXRContext gvrContext, SXRAndroidResource.Callback<T> callback,
            SXRAndroidResource resource, int priority) {
        validateCallbackParameters(gvrContext, callback, resource);
        if (priority < SXRContext.LOWEST_PRIORITY
                || priority > SXRContext.HIGHEST_PRIORITY) {
            throw new IllegalArgumentException(
                    "Priority < SXRContext.LOWEST_PRIORITY or > SXRContext.HIGHEST_PRIORITY");
        }
    }

    /**
     * An internal method, public only so that SXRContext can make cross-package
     * calls.
     * 
     * A synchronous (blocking) wrapper around
     * {@link android.graphics.BitmapFactory#decodeStream(InputStream)
     * BitmapFactory.decodeStream} that uses an
     * {@link android.graphics.BitmapFactory.Options} <code>inTempStorage</code>
     * decode buffer. On low memory, returns half (quarter, eighth, ...) size
     * images.
     * <p>
     * If {@code stream} is a {@link FileInputStream} and is at offset 0 (zero),
     * uses
     * {@link android.graphics.BitmapFactory#decodeFileDescriptor(FileDescriptor)
     * BitmapFactory.decodeFileDescriptor()} instead of
     * {@link android.graphics.BitmapFactory#decodeStream(InputStream)
     * BitmapFactory.decodeStream()}.
     * 
     * @param stream
     *            Bitmap stream
     * @param closeStream
     *            If {@code true}, closes {@code stream}
     * @return Bitmap, or null if cannot be decoded into a bitmap
     */
    public static Bitmap decodeStream(InputStream stream, boolean closeStream) {
        return AsyncBitmapTexture.decodeStream(stream,
                AsyncBitmapTexture.glMaxTextureSize,
                AsyncBitmapTexture.glMaxTextureSize, true, null, closeStream);
    }

    /**
     * Load a atlas map information asynchronously.
     *
     * @param ins
     *            JSON text stream
     */
    public static List<SXRAtlasInformation> loadAtlasInformation(InputStream ins) {
        return AsyncAtlasInfo.loadAtlasInformation(ins);
    }
}
