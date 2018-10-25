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

import android.util.Log;
import android.view.MotionEvent;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.ITouchEvents;
import com.samsungxr.io.cursor3d.CursorAsset.Action;

import java.util.HashSet;
import java.util.Set;

class ObjectCursor extends Cursor {
    private static final String TAG = ObjectCursor.class.getSimpleName();
    private static final float POINT_CURSOR_NEAR_DEPTH = 3.0f;
    private int mColliderID = -1;

    ObjectCursor(SXRContext context, CursorManager cursorManager) {
        super(context, CursorType.OBJECT, cursorManager);

        Log.d(TAG, Integer.toHexString(hashCode()) + " constructed");
        mTouchListener = new ITouchEvents()
        {
            public void onEnter(SXRSceneObject obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.INTERSECT);
            }

            public void onExit(SXRSceneObject obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.DEFAULT);
            }

            public void onInside(SXRSceneObject obj, SXRPicker.SXRPickedObject hit) { }

            public void onTouchStart(SXRSceneObject obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.CLICK);
            }

            public void onTouchEnd(SXRSceneObject obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.DEFAULT);
            }

            public void onMotionOutside(SXRPicker picker, MotionEvent event) { }
        };
    }

    public int getColliderID() { return mColliderID; }

    public void setColliderID(int id) { mColliderID = id; }


    @Override
    void setCursorDepth(float depth) {
        if (depth > MAX_CURSOR_SCALE) {
            return;
        }

        // place the cursor at half the depth scale
        super.setCursorDepth(depth / 2);

        IoDevice device = getIoDevice();
        if (device != null) {
            device.setNearDepth(POINT_CURSOR_NEAR_DEPTH);
        }
    }

    @Override
    void setIoDevice(IoDevice ioDevice) {
        super.setIoDevice(ioDevice);
        ioDevice.setNearDepth(POINT_CURSOR_NEAR_DEPTH);
    }

    @Override
    void setupIoDevice(IoDevice ioDevice) {
        super.setupIoDevice(ioDevice);
        ioDevice.setDisableRotation(false);
    }
}