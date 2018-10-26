/*
 * Copyright 2016 Samsung Electronics Co., LTD
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

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.SXRAnimationEngine;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXRRepeatMode;
import com.samsungxr.utility.Log;

import java.io.IOException;

/**
 * Use this behavior to uniquely set an object to the {@link Cursor}.
 */
class ObjectCursorAsset extends CursorAsset {
    private static final String TAG = ObjectCursorAsset.class.getSimpleName();
    private final String assetName;
    private SparseArray<SXRNode> objects;
    private SXRAnimationEngine animationEngine;
    private int LOOP_REPEAT_COUNT = -1;

    ObjectCursorAsset(SXRContext context, CursorType type, Action action, String assetName) {
        super(context, type, action);
        this.assetName = assetName;
        objects = new SparseArray<SXRNode>();
        animationEngine = context.getAnimationEngine();
    }

    @Override
    void set(Cursor cursor) {
        super.set(cursor);
        SXRNode modelNode = objects.get(cursor.getId());

        if (modelNode == null) {
            Log.e(TAG, "Model not found, should not happen");
            return;
        }
        modelNode.setEnable(true);

        SXRAnimator animator = (SXRAnimator) modelNode.getComponent(SXRAnimator.getComponentType());
        if (animator != null)
        {
            animator.setRepeatMode(SXRRepeatMode.REPEATED);
            animator.setRepeatCount(LOOP_REPEAT_COUNT);
            animator.start();
        }
    }

    private SXRNode loadModelNode() {
        SXRNode modelNode = null;
        try {
            modelNode = context.getAssetLoader().loadModel(assetName);
        } catch (IOException e) {
            //should not happen
            Log.e(TAG, "Could not load model", e);
        }
        return modelNode;
    }

    @Override
    void reset(Cursor cursor) {
        super.reset(cursor);

        SXRNode modelNode = objects.get(cursor.getId());

        modelNode.setEnable(false);
        SXRAnimator animator = (SXRAnimator) modelNode.getComponent(SXRAnimator.getComponentType());
        if (animator != null)
        {
            animator.setRepeatMode(SXRRepeatMode.ONCE);
            animator.setRepeatCount(0);
            animator.start();
        }
    }

    @Override
    void load(Cursor cursor) {
        Integer key = cursor.getId();
        SXRNode modelNode = objects.get(key);

        if (modelNode == null) {
            modelNode = loadModelNode();
            modelNode.setName( getAction().toString() + key.toString());
            objects.put(key, modelNode);
        }
        cursor.addChildObject(modelNode);
        modelNode.setEnable(false);
    }

    @Override
    void unload(Cursor cursor) {
        SXRNode assetNode = objects.get(cursor.getId());
        cursor.removeChildObject(assetNode);
        objects.remove(cursor.getId());
    }
}
