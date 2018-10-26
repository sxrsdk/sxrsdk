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

import android.view.MotionEvent;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;
import com.samsungxr.ITouchEvents;
import com.samsungxr.io.cursor3d.CursorAsset.Action;
import com.samsungxr.utility.Log;

/**
 * Class that represents a laser type cursor.
 */
class LaserCursor extends Cursor {
    private static final String TAG = "LaserCursor";

    /**
     * Create a laser type 3D cursor for SXRf.
     *
     * @param context The SXRf Context
     */
    LaserCursor(SXRContext context, CursorManager manager) {
        super(context, CursorType.LASER, manager);
        Log.d(TAG, Integer.toHexString(hashCode()) + " constructed");
        mTouchListener = new ITouchEvents()
        {
            public void onEnter(SXRNode obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.HOVER);
            }

            public void onExit(SXRNode obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.DEFAULT);
            }

            public void onInside(SXRNode obj, SXRPicker.SXRPickedObject hit) { }

            public void onTouchStart(SXRNode obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.CLICK);
            }

            public void onTouchEnd(SXRNode obj, SXRPicker.SXRPickedObject hit)
            {
                checkAndSetAsset(Action.DEFAULT);
            }

            public void onMotionOutside(SXRPicker picker, MotionEvent event) { }
        };
    }

    @Override
    void setCursorDepth(float depth) {
        depth = Math.abs(depth);
        if (depth > MAX_CURSOR_SCALE) {
            return;
        }
        super.setCursorDepth(depth);
    }

    @Override
    void setIoDevice(IoDevice ioDevice) {
        super.setIoDevice(ioDevice);
        ioDevice.setDisableRotation(true);
    }

}