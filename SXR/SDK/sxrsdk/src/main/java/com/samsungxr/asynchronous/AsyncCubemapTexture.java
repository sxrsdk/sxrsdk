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

import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRAndroidResource.CancelableCallback;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRCubemapImage;
import com.samsungxr.asynchronous.Throttler.AsyncLoader;
import com.samsungxr.asynchronous.Throttler.AsyncLoaderFactory;
import com.samsungxr.asynchronous.Throttler.GlConverter;
import com.samsungxr.utility.FileNameUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Async resource loading: cube map textures.
 * 
 * Since ZipInputStream does not support mark() and reset(), we directly use
 * BitmapFactory .decodeStream() in loadResource().
 * 
 * @since 1.6.9
 */
class AsyncCubemapTexture {
    private static final String TAG = AsyncCubemapTexture.class.getSimpleName();
    /*
     * The API
     */

    static void loadTexture(SXRContext gvrContext,
                            CancelableCallback<SXRCubemapImage> callback,
            SXRAndroidResource resource, int priority, Map<String, Integer> map) {
        faceIndexMap = map;
        AsyncManager.get().getScheduler().registerCallback(gvrContext, TEXTURE_CLASS, callback,
                resource, priority);
    }

    private static Map<String, Integer> faceIndexMap;
    
    private static final Class<SXRCubemapImage> TEXTURE_CLASS = SXRCubemapImage.class;
    
    /*
     * Singleton
     */
    private static AsyncCubemapTexture sInstance = new AsyncCubemapTexture();

    /**
     * Gets the {@link AsyncCubemapTexture} singleton for loading bitmap textures.
     * @return The {@link AsyncCubemapTexture} singleton.
     */
    public static AsyncCubemapTexture get() {
        return sInstance;
    }

    private AsyncCubemapTexture() {
        AsyncManager.get().registerDatatype(TEXTURE_CLASS,
                new AsyncLoaderFactory<SXRCubemapImage, Bitmap[]>() {
                    @Override
                    AsyncLoader<SXRCubemapImage, Bitmap[]> threadProc(
                            SXRContext gvrContext,
                            SXRAndroidResource request,
                            CancelableCallback<SXRCubemapImage> cancelableCallback,
                            int priority) {
                        return new AsyncLoadCubemapTextureResource(gvrContext,
                                request, cancelableCallback, priority);
                    }
                });
    }

    /*
     * Asynchronous loader for uncompressed cubemap
     */

    private static class AsyncLoadCubemapTextureResource extends
            AsyncLoader<SXRCubemapImage, Bitmap[]> {

        private static final GlConverter<SXRCubemapImage, Bitmap[]> sConverter = new GlConverter<SXRCubemapImage, Bitmap[]>() {

            @Override
            public SXRCubemapImage convert(SXRContext gvrContext,
                                           Bitmap[] bitmapArray) {
                return new SXRCubemapImage(gvrContext, bitmapArray);
            }
        };

        protected AsyncLoadCubemapTextureResource(SXRContext gvrContext,
                                                  SXRAndroidResource request,
                                                  CancelableCallback<SXRCubemapImage> callback, int priority) {
            super(gvrContext, sConverter, request, callback);
        }

        @Override
        protected Bitmap[] loadResource() throws IOException {
            Bitmap[] bitmapArray = new Bitmap[6];
            ZipInputStream zipInputStream = null;
            try {
                ZipEntry zipEntry = null;
                zipInputStream = new ZipInputStream(resource.getStream());
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String imageName = zipEntry.getName();
                    String imageBaseName = FileNameUtils.getBaseName(imageName);
                    Integer imageIndex = faceIndexMap.get(imageBaseName);
                    if (imageIndex == null) {
                        continue;
                    }
                    bitmapArray[imageIndex] = BitmapFactory
                            .decodeStream(zipInputStream);
                }
               for(int i=0; i < bitmapArray.length; i++) {
                   if(bitmapArray[i] == null) {
                       for(Map.Entry<String,Integer> entry:faceIndexMap.entrySet()) {
                           if(entry.getValue() == i) {
                               throw new IllegalArgumentException("Could not find texture " +
                                       "corresponding to " + entry.getKey() + " in the cubemap " +
                                       "texture");
                           }
                       }
                   }
               }
            }
            finally {
                zipInputStream.close();
                resource.closeStream();
            }
            return bitmapArray;
        }
    }
}
