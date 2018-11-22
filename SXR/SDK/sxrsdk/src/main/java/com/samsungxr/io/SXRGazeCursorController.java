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

package com.samsungxr.io;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRScene;
import com.samsungxr.utility.Log;
import org.joml.Vector3f;

import java.util.concurrent.CountDownLatch;

final public class SXRGazeCursorController extends SXRCursorController
{
    private static final float DEPTH_SENSITIVITY = 0.1f;
    private float actionDownX;
    private float actionDownZ;
    private final KeyEvent BUTTON_GAZE_DOWN = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_1);
    private final KeyEvent BUTTON_GAZE_UP = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_1);
    private float mDisplayWidth = 0;
    private float mDisplayHeight = 0;
    private float mDisplayDepth;

    SXRGazeCursorController(SXRContext context,
                            SXRControllerType controllerType,
                            String name, int vendorId, int productId)
    {
        super(context, controllerType, name, vendorId, productId);
        mConnected = true;
    }

    /**
     * Use this to enable the user interaction by touch screen on mobile applications.
     *
     * @param depth Z depth of the touch screen (distance from camera).
     *              If zero, touch screen interaction is not enabled.
     */
    public void setTouchScreenDepth(float depth)
    {
        depth = Math.abs(depth);
        if (depth != 0)
        {
            final Activity activity = getSXRContext().getActivity();
            final DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            mDisplayWidth = metrics.widthPixels;
            mDisplayHeight = metrics.heightPixels;
            mDisplayDepth = depth;
            mPicker.setEnable(false);   // only pick based on touch
        }
        else
        {
            mPicker.setEnable(true);    // pick based on gaze direction
            mDisplayWidth = mDisplayHeight = mDisplayDepth = 0;
        }
    }

    /**
     *
     * @return True if the touch screen is enabled, otherwise returns false.
     */
    public boolean isTouchScreenEnabled() {
        return mDisplayDepth > 0;
    }

    @Override
    synchronized public boolean dispatchKeyEvent(KeyEvent event) {
        if (isEnabled())
        {
            setKeyEvent(event);
            return true;
        }
        return false;
    }

    @Override
    synchronized public boolean dispatchMotionEvent(MotionEvent event)
    {
        if (isEnabled())
        {
            handleMotionEvent(MotionEvent.obtain(event));
            return true;
        }
        return false;
    }

    private void handleMotionEvent(MotionEvent event)
    {
        final float eventX = event.getX();
        final float eventY = event.getY();
        final int action = event.getAction();
        float deltaX;
        int button = event.getButtonState();

        if (button == 0)
        {
            button = MotionEvent.BUTTON_PRIMARY;
        }
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
            actionDownX = eventX;
            actionDownZ = mCursorDepth;
            if ((mTouchButtons & button) != 0)
            {
                setActive(true);
            }
            // report ACTION_DOWN as a button
            setKeyEvent(BUTTON_GAZE_DOWN);
            break;

            case MotionEvent.ACTION_UP:
            setActive(false);
            setKeyEvent(BUTTON_GAZE_UP);
            break;

            case MotionEvent.ACTION_MOVE:
            deltaX = eventX - actionDownX;
            if (mCursorControl == CursorControl.CURSOR_DEPTH_FROM_CONTROLLER)
            {
                float eventZ = actionDownZ + deltaX * DEPTH_SENSITIVITY;
                if (eventZ <= getNearDepth())
                {
                    eventZ = getNearDepth();
                }
                if (eventZ >= getFarDepth())
                {
                    eventZ = getFarDepth();
                }
                setCursorDepth(eventZ);
            }
            break;
            default:
                event.recycle();
                return;
        }

        if (isTouchScreenEnabled()) {
            final SXRPerspectiveCamera cam
                    = getSXRContext().getMainScene().getMainCameraRig().getCenterCamera();
            final float aspect = cam.getAspectRatio();
            final double fov = Math.toRadians(cam.getFovY());
            final float h = (float) (mDisplayDepth * Math.tan(fov * 0.5f));
            final float w = aspect * h;

            final float x = (eventX / mDisplayWidth - 0.5f) * w * 2;
            final float y = (0.5f - eventY / mDisplayHeight) * h * 2;

            setPosition(x, y, -mDisplayDepth);
        }

        setMotionEvent(event);
        invalidate();
    }
}
