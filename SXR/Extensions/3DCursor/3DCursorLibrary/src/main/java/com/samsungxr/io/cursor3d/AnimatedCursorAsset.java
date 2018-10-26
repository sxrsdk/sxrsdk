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

package com.samsungxr.io.cursor3d;

import android.util.SparseArray;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.ZipLoader;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimationEngine;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.utility.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Use this {@link CursorAsset} for cases where the {@link Cursor} needs to be animated
 * using a series of textures that define a time series of the animation frames.
 *
 * The class takes the name of the folder in the assets directory that contains all the texture
 * files that help animate the {@link Cursor}.
 *
 * This class in itself only defines texture animations. It is assumed that the object that uses
 * this {@link CursorAsset} already has a {@link SXRMesh} and a {@link SXRMaterial} set.
 */
class AnimatedCursorAsset extends MeshCursorAsset {
    private static final String TAG = AnimatedCursorAsset.class.getSimpleName();
    private List<SXRTexture> loaderTextures;
    private final static float LOADING_IMAGE_FRAME_ANIMATION_DURATION = 1f;
    private float animationDuration = LOADING_IMAGE_FRAME_ANIMATION_DURATION;
    private final static int LOOP_REPEAT = -1;
    private SparseArray<SXRImageFrameAnimation> animations;
    private final SXRAnimationEngine animationEngine;

    private String zipFileName;

    AnimatedCursorAsset(SXRContext context, CursorType type, Action action, String zipFileName,
                        String mesh) {
        super(context, type, action, mesh, null);
        this.zipFileName = zipFileName;
        animations = new SparseArray<SXRImageFrameAnimation>();
        animationEngine = context.getAnimationEngine();
    }

    AnimatedCursorAsset(SXRContext context, CursorType type, Action action, String zipFileName) {
        this(context, type, action, zipFileName, null);
    }

    @Override
    void set(Cursor cursor) {
        super.set(cursor);

        int key = cursor.getId();
        SXRImageFrameAnimation animation = animations.get(key);
        if (animation == null) {
            SXRNode assetNode = sceneObjectArray.get(key);
            if (assetNode == null) {
                Log.e(TAG, "Render data not found, should not happen");
                return;
            }
            SXRRenderData renderData = assetNode.getRenderData();
            SXRMaterial loadingMaterial = renderData.getMaterial();
            loadingMaterial.setMainTexture(loaderTextures.get(0));
            animation = new SXRImageFrameAnimation(loadingMaterial,
                    animationDuration, loaderTextures);
            //Usual animations have a repeat behavior
            animation.setRepeatMode(SXRRepeatMode.REPEATED);
            animation.setRepeatCount(LOOP_REPEAT);
            animations.append(key, animation);
        }

        animationEngine.start(animation).setOnFinish(null);
    }

    @Override
    void reset(Cursor cursor) {
        int key = cursor.getId();
        SXRImageFrameAnimation animation = animations.get(key);
        if (animation == null) {
            //nothing to do
            Log.d(TAG, "Animation is finished return, should not happen ");
            super.reset(cursor);
            return;
        }

        if (animation.isFinished() == false) {
            if (animation.getRepeatCount() == LOOP_REPEAT) {
                animation.setRepeatCount(0);
                animation.setRepeatMode(SXRRepeatMode.ONCE);
            }
            animationEngine.stop(animation);
        }
        animations.remove(key);

        super.reset(cursor);
    }

    @Override
    void load(Cursor cursor) {
        super.load(cursor);
        if (loaderTextures != null) {
            return;
        }
        try {
            loaderTextures = ZipLoader.load(context, zipFileName, new ZipLoader
                    .ZipEntryProcessor<SXRTexture>() {

                @Override
                public SXRTexture getItem(SXRContext context, SXRAndroidResource resource) {
                    return context.getAssetLoader().loadTexture(resource);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error loading textures", e);
        }
    }

    @Override
    void unload(Cursor cursor) {
        super.unload(cursor);
        animations.remove(cursor.getId());

        // check if there are cursors still using the textures
        if (sceneObjectArray.size() == 0) {
            loaderTextures.clear();
            loaderTextures = null;
        }
    }

    void setAnimationDuration(float duration) {
        animationDuration = duration;
    }

    float getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Implements texture update animation.
     */
    private static class SXRImageFrameAnimation extends SXRAnimation {
        private final List<SXRTexture> animationTextures;
        private int lastFileIndex = -1;

        /**
         * @param material             {@link SXRMaterial} to animate
         * @param duration             The animation duration, in seconds.
         * @param texturesForAnimation arrayList of SXRTexture used during animation
         */

        private SXRImageFrameAnimation(SXRMaterial material, float duration,
                                       final List<SXRTexture> texturesForAnimation) {
            super(material, duration);
            animationTextures = texturesForAnimation;
        }

        @Override
        protected void animate(SXRHybridObject target, float ratio) {
            final int size = animationTextures.size();
            final int fileIndex = (int) (ratio * size);

            if (lastFileIndex == fileIndex || fileIndex == size) {
                return;
            }

            lastFileIndex = fileIndex;

            SXRMaterial material = (SXRMaterial) target;
            material.setMainTexture(animationTextures.get(fileIndex));
        }
    }
}
