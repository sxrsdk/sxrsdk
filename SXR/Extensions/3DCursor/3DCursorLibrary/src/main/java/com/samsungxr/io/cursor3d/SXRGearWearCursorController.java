/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.io.cursor3d;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.KeyEvent;


import com.samsungxr.io.SXRControllerType;
import com.samsungxr.io.SXRDeviceConstants;
import com.samsungxr.io.gearwearlibrary.EventManager;
import com.samsungxr.io.gearwearlibrary.events.Back;
import com.samsungxr.io.gearwearlibrary.events.Click;
import com.samsungxr.io.gearwearlibrary.events.Connected;
import com.samsungxr.io.gearwearlibrary.events.Disconnected;
import com.samsungxr.io.gearwearlibrary.events.Rotary;
import com.samsungxr.io.gearwearlibrary.events.Rotary.Direction;
import com.samsungxr.io.gearwearlibrary.events.Swipe;
import com.samsungxr.io.gearwearlibrary.events.TouchEnd;
import com.samsungxr.io.gearwearlibrary.events.TouchMove;
import com.samsungxr.io.gearwearlibrary.events.TouchStart;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.utility.Log;

import org.joml.Vector3f;

import java.util.concurrent.TimeUnit;

// TODO: Consumer needs to send event when application exits (unless TouchEnd will occur)
// TODO: Need to detect connection issues, and stop moving if issue occurs
// TODO: See if new algorithm works well on Gear 2, since it has a 320x320 square display
public class SXRGearWearCursorController extends SXRCursorController
{
    private static final String TAG = SXRGearWearCursorController.class.getSimpleName();

    //Device information

    //Handler information
    private static final String HANDLER_THREAD_NAME = "BACKGROUND";
    private static final String WEAR_TOUCH_PAD_SERVICE_PACKAGE_NAME = "com.samsungxr.weartouchpad";
    private static final int MSG_PROCESS_START = 1;
    private static final int MSG_PROCESS_STOP = 2;
    private static final int MSG_PROCESS_ROTARY = 4;
    private static final int MSG_PROCESS_SWIPE = 5;
    private static final long PROCESS_EVENT_DELAY = 16;

    private static final float[] UP_VECTOR = {0.0f, 1.0f, 0.0f, 1.0f};
    private static final float[] RIGHT_VECTOR = {1.0f, 0.0f, 0.0f, 1.0f};

    private static final float ONE_RADIAN = 1.0f;
    private static final float DEPTH_STEP = (float) (1 / Math.toDegrees(ONE_RADIAN));
    private static final float Z_DEPTH_STEP = .06f;
    private static final float INITIAL_Z_DEPTH = -16.0f;
    private static final float MAX_Z_DEPTH = -15.0f;
    private static final float MIN_Z_DEPTH = -1.0f;

    private static final int MAX_DISTANCE = 180;
    private static final double MAX_SENSITIVITY = .3f;
    private static final double MIN_SENSITIVITY = .1f;
    private static final float TRACKPAD_SENSITIVITY = 0.2f;
    //TODO: The max width/height can be sent from the Gear
    private static final int MAX_WIDTH_GEAR_S2 = 360, MAX_HEIGHT_GEAR_S2 = 360;
    private static final int MAX_WIDTH_2_GEAR_2 = 320, MAX_HEIGHT_GEAR_2 = 320;
    private static final float CENTER_X = 180;
    private static final float CENTER_Y = 180;
    /**
     * Press event radius
     */
    private static final float MIDDLE_RADIUS = 45;
    private static final float INNER_RADIUS = 20;

    private EventReceiver eventReceiver;
    private SXRNode internalObject;
    private HandlerThread thread;
    private EventHandler handler;
    private final Vector3f positionData;
    private final Vector3f prevPositionData;
    private MovementMode movementMode;

    public enum MovementMode
    {
        JOYSTICK, TRACKPAD
    }

    public SXRGearWearCursorController(SXRContext ctx)
    {
        super(ctx, SXRControllerType.WEARTOUCHPAD, "gearwear",
              SXRDeviceConstants.SAMSUNG_GEARWEAR_TOUCHPAD_VENDOR_ID,
              SXRDeviceConstants.SAMSUNG_GEARWEAR_TOUCHPAD_PRODUCT_ID);
        internalObject = new SXRNode(ctx);
        positionData = new Vector3f(0.0f, 0.0f, 0.0f);
        this.movementMode = movementMode;
        prevPositionData = new Vector3f(0.0f, 0.0f, 0.0f);
        setPosition(0.0f, 0.0f, -1.0f);
        EventManager.registerReceiver(ctx.getContext(), eventReceiver);
        EventManager.requestConnectionStatus(ctx.getContext());
    }

    @Override
    public void setPosition(float x, float y, float z)
    {
        super.setPosition(x, y, z);
        internalObject.getTransform().setPosition(x, y, z);
    }

    @Override
    public void setEnable(boolean enable)
    {
        if (!mEnabled && enable)
        {
            thread = new HandlerThread(HANDLER_THREAD_NAME);
            thread.start();
            handler = new EventHandler(thread.getLooper());
        }
        else if (mEnabled && !enable)
        {
            thread.quit();
        }
        super.setEnable(enable);
    }

    public void close()
    {
        EventManager.unregisterReceiver(mContext.getContext(), eventReceiver);
        if (mEnabled)
        {
            mEnabled = false;
            thread.quit();
        }
    }

    void setConnected(boolean connected)
    {
        boolean wasConnected = mConnected;

        mConnected = connected;
        if (!wasConnected && mConnected)
        {
            mContext.getInputManager().addCursorController(this);
        }
        else if (wasConnected && !mConnected)
        {
            mContext.getInputManager().removeCursorController(this);
            return;
        }
    }

    public MovementMode getMovementMode()
    {
        return movementMode;
    }

    public void setMovementMode(MovementMode movementMode)
    {
        this.movementMode = movementMode;
    }

    static boolean isServiceInstalled(SXRContext ctx)
    {
        PackageManager pm = ctx.getActivity().getPackageManager();
        try
        {
            pm.getPackageInfo(WEAR_TOUCH_PAD_SERVICE_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
        }
        return false;
    }

    private enum StateEvent
    {
        TOUCH_START, TOUCH_END, TIMER_EXPIRED, ROTARY, DISCONNECTED, CLICK, BACK_PRESSED
    }

    private class EventReceiver extends BroadcastReceiver
    {

        public static final int CLICK_DELAY_MS = 120;
        public static final int DOUBLE_CLICK_DELAY_MS = 250;
        SparseArray<State> stateArray = new SparseArray<State>(StateKey.values().length);
        State state;
        private Handler handler = new Handler(Looper.getMainLooper());
        private boolean pressed = false;

        Runnable timerRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                processEvent(StateEvent.TIMER_EXPIRED);
            }
        };

        public EventReceiver()
        {
            stateArray.put(StateKey.INIT.ordinal(), new InitState());
            stateArray.put(StateKey.WAITING_CLICK_1.ordinal(), new WaitingClick1State());
            stateArray.put(StateKey.CLICK_1.ordinal(), new Click1State());
            stateArray.put(StateKey.WAITING_CLICK_2.ordinal(), new WaitingClick2State());
            stateArray.put(StateKey.CLICK_2.ordinal(), new Click2State());
            stateArray.put(StateKey.MOVE.ordinal(), new MoveState());
            stateArray.put(StateKey.CLICK_2_HOLD.ordinal(), new Click2HoldState());
            setState(StateKey.INIT);
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Parcelable data = intent.getParcelableExtra(EventManager.EXTRA_EVENT);
            Log.v(TAG, "onReceive: data: %s", data);

            if (data instanceof Connected)
            {
                Log.d(TAG, "onReceive Connected");
                if (!mEnabled)
                {
                    Log.d(TAG, "onReceive enabled");
                    setConnected(true);
                }
                return;
            }
            else if (data instanceof Disconnected)
            {
                Log.d(TAG, "onReceive Disconnected");
                if (mEnabled)
                {
                    Log.d(TAG, "onReceive disabled");
                    setConnected(false);
                    processEvent(StateEvent.DISCONNECTED);
                }
                return;
            }

            if (!mEnabled)
            {
                Log.d(TAG, "Received data when disabled, do nothing.");
                return;
            }

            if (data instanceof TouchStart)
            {
                Log.v(TAG, "TouchStart");
                TouchStart touchStart = (TouchStart) data;
                positionData.set(touchStart.x, touchStart.y, 0.0f);
                processEvent(StateEvent.TOUCH_START);
            }
            else if (data instanceof TouchMove)
            {
                Log.v(TAG, "TouchMove");
                TouchMove touchMove = (TouchMove) data;
                positionData.set(touchMove.x, touchMove.y, 0.0f);
            }
            else if (data instanceof Rotary)
            {
                Log.v(TAG, "TouchRotary");
                Direction direction = ((Rotary) data).direction;
                processRotary(direction);
                processEvent(StateEvent.ROTARY);
            }
            else if (data instanceof TouchEnd)
            {
                Log.v(TAG, "TouchEnd");
                processEvent(StateEvent.TOUCH_END);
            }
            else if (data instanceof Click)
            {
                Log.v(TAG, "Click");
                processEvent(StateEvent.CLICK);
            }
            else if (data instanceof Back)
            {
                Log.v(TAG, "Back button pressed");
                processEvent(StateEvent.BACK_PRESSED);
            }
            else if (data instanceof Swipe)
            {
                Swipe.Direction direction = ((Swipe) data).direction;
                Log.v(TAG, "Swipe: direction=" + direction);
                consumeSwipe(direction);
            }
        }

        public void processEvent(StateEvent event)
        {
            Log.v(TAG, "Processing " + event + " on state:" + state.getClass().getSimpleName());
            state.processEvent(event);
        }

        public void setState(StateKey key)
        {
            state = stateArray.get(key.ordinal());
            state.doAction();
        }

        public void startTimer(int delay) {
            handler.postDelayed(timerRunnable, delay);
        }

        public void cancelTimer() {
            handler.removeCallbacks(timerRunnable);
        }

        public void toggleDoubleClickState()
        {
            if (pressed)
            {
                SXRGearWearCursorController.this.handler.sendPressStop();
            }
            else
            {
                SXRGearWearCursorController.this.handler.sendPressStart();
            }
            pressed = !pressed;
        }
    }

    private void consumeSwipe(Swipe.Direction direction)
    {
        switch (direction)
        {
            case LEFT:
                dispatchKeyEvent(CustomKeyEvent.ACTION_SWIPE, CustomKeyEvent.KEYCODE_SWIPE_LEFT);
                handler.sendProcessSwipe();
                break;

            case RIGHT:
                dispatchKeyEvent(CustomKeyEvent.ACTION_SWIPE, CustomKeyEvent.KEYCODE_SWIPE_RIGHT);
                handler.sendProcessSwipe();
                break;

            default:
                break;
        }
    }

    private enum StateKey
    {
        INIT, WAITING_CLICK_1, CLICK_1, WAITING_CLICK_2, CLICK_2, MOVE, CLICK_2_HOLD
    }

    private abstract class State
    {
        void doAction() { }

        abstract void processEvent(StateEvent event);
    }

    private class InitState extends State
    {
        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case TOUCH_START:
                    eventReceiver.setState(StateKey.WAITING_CLICK_1);
                    break;

                case CLICK:
                    eventReceiver.setState(StateKey.CLICK_1);
                    break;

                case BACK_PRESSED:
                    eventReceiver.toggleDoubleClickState();
                    break;

                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
                    break;
            }
        }
    }

    private class WaitingClick1State extends State
    {
        void doAction() {
            eventReceiver.startTimer(EventReceiver.CLICK_DELAY_MS);
        }

        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case TOUCH_END:
                    eventReceiver.setState(StateKey.CLICK_1);
                    break;
                case TIMER_EXPIRED:
                    eventReceiver.setState(StateKey.MOVE);
                    break;
                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
                    break;
            }
        }
    }

    private class Click1State extends State
    {
        void doAction() {
            eventReceiver.cancelTimer();
            eventReceiver.startTimer(EventReceiver.DOUBLE_CLICK_DELAY_MS);
        }

        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case TOUCH_START:
                    eventReceiver.setState(StateKey.WAITING_CLICK_2);
                    break;
                case TIMER_EXPIRED:
                    handler.sendPressStart();
                    handler.sendPressStop();
                    eventReceiver.setState(StateKey.INIT);
                    break;
                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass()
                            .getSimpleName());
                    break;
            }
        }
    }

    private class WaitingClick2State extends State
    {
        void doAction()
        {
            eventReceiver.cancelTimer();
            eventReceiver.startTimer(EventReceiver.CLICK_DELAY_MS);
        }

        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case TOUCH_END:
                    eventReceiver.setState(StateKey.CLICK_2);
                    break;
                case TIMER_EXPIRED:
                    eventReceiver.setState(StateKey.CLICK_2_HOLD);
                    break;
                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
                    break;
            }
        }
    }

    private class Click2State extends State
    {

        void doAction() {
            eventReceiver.toggleDoubleClickState();
        }

        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case CLICK:
                    eventReceiver.setState(StateKey.INIT);
                    break;

                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
                    break;
            }
        }
    }

    private class MoveState extends State
    {
        void doAction() {
            handler.sendStartProcessing();
        }

        void processEvent(StateEvent event)
        {
            switch (event)
            {
                case TOUCH_END:
                case ROTARY:
                case DISCONNECTED:
                    handler.sendStopProcessing();
                    eventReceiver.setState(StateKey.INIT);
                    break;
                case BACK_PRESSED:
                    eventReceiver.setState(StateKey.CLICK_2_HOLD);
                    break;
                default:
                    Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
                    break;
            }
        }
    }

    private class Click2HoldState extends State
    {
        void doAction()
        {
            eventReceiver.toggleDoubleClickState();
            eventReceiver.setState(StateKey.MOVE);
        }

        void processEvent(StateEvent event)
        {
            Log.v(TAG, "Ignoring event:" + event + " for " + this.getClass().getSimpleName());
        }
    }

    public void dispatchKeyEvent(int action, int code) {
        setKeyEvent(new KeyEvent(action, code));
    }

    private void processControllerEventDifferential()
    {
        float x, y, z;
        synchronized (positionData)
        {
            x = positionData.x;
            y = positionData.y;
            z = positionData.z;
            positionData.z = 0;
        }

        SXRScene scene = mContext.getMainScene();
        if (scene != null)
        {
            float[] viewMatrix = scene.getMainCameraRig().getHeadTransform().getModelMatrix();
            float[] xAxis = new float[4];
            float[] yAxis = new float[4];

            Matrix.multiplyMV(xAxis, 0, viewMatrix, 0, UP_VECTOR, 0);
            Matrix.multiplyMV(yAxis, 0, viewMatrix, 0, RIGHT_VECTOR, 0);

            if (x != 0 || y != 0)
            {
                float xAngle, yAngle;
                if (movementMode == MovementMode.JOYSTICK)
                {
                    float sensitivity = getSensitivity(x, y);
                    x = x / CENTER_X - 1;
                    y = -(y / CENTER_Y - 1);
                    Log.v(TAG, "processing: x-diff: %f, y-diff: %f", x, y);
                    float angle = (float) Math.atan2(y, x);
                    xAngle = -1 * ((float) Math.cos(angle)) * sensitivity;
                    yAngle = ((float) Math.sin(angle)) * sensitivity;
                }
                else
                {
                    if ((prevPositionData.x == 0) &&
                        (prevPositionData.y == 0) &&
                        (prevPositionData.z == 0))
                    {
                        prevPositionData.set(x, y, z);
                        return;
                    }
                    xAngle = (prevPositionData.x - x) * TRACKPAD_SENSITIVITY;
                    yAngle = (prevPositionData.y - y) * TRACKPAD_SENSITIVITY;
                    prevPositionData.set(x, y, z);
                    Log.v(TAG, "processing: x: %f, y: %f, xAngle: %f, yAngle: %f", x, y, xAngle, yAngle);
                }
                internalObject.getTransform().setRotation(1.0f, 0.0f, 0.0f, 0.0f);
                internalObject.getTransform().rotateByAxisWithPivot(
                        xAngle, xAxis[0], xAxis[1],
                        xAxis[2], 0.0f, 0.0f, 0.0f);
                internalObject.getTransform().rotateByAxisWithPivot(
                        yAngle, yAxis[0], yAxis[1],
                        yAxis[2], 0.0f, 0.0f, 0.0f);
            }

            if (z != 0.0f)
            {
                Vector3f controllerPosition = new Vector3f(
                    internalObject.getTransform().getPositionX(),
                    internalObject.getTransform().getPositionY(),
                    internalObject.getTransform().getPositionZ());
                float step = (z < 0) ? Z_DEPTH_STEP : -Z_DEPTH_STEP;
                Vector3f point = new Vector3f(controllerPosition);
                point.mul(1 + step);

                if (checkBounds(point))
                {
                    Log.v(TAG, "Check bounds passed, setting position");
                    internalObject.getTransform().setPosition(point.x, point.y, point.z);
                }
            }
            super.setPosition(internalObject.getTransform().getPositionX(),
                              internalObject.getTransform().getPositionY(),
                              internalObject.getTransform().getPositionZ());
        }
    }

    private boolean checkBounds(Vector3f point)
    {
        float lhs = point.length();
        return (lhs <= getMaxRadius() && lhs >= getMinRadius());
    }

    private float getMinRadius()
    {
        float nearDepth = getNearDepth();
        return nearDepth * nearDepth;
    }

    private float getMaxRadius()
    {
        float farDepth = getFarDepth();
        return farDepth * farDepth;
    }

    /**
     * Calculate sensitivity based on distance from center
     * <p/>
     * The farther from the center, the more sensitive the cursor should be (linearly)
     */
    private float getSensitivity(float x, float y)
    {
        double furthestDistance = MAX_DISTANCE;
        double distance = Math.sqrt(Math.pow(x - CENTER_X, 2) + Math.pow(y - CENTER_Y, 2));
        double sens = MAX_SENSITIVITY * distance / furthestDistance;
        if (sens > MAX_SENSITIVITY)
        {
            sens = MAX_SENSITIVITY;
        }
        return (float) sens;
    }

    private void processRotary(Direction direction)
    {
        float z;
        switch (direction)
        {
            case CW: z = -1.0f;break;
            case CCW: z = 1.0f;break;
            default: Log.w(TAG, "Unhandled direction: %s", direction);return;
        }
        positionData.set(0.0f, 0.0f, z);
        handler.sendProcessRotary();
    }

    /**
     * Check if position is in middle circle (press region)
     *
     * @param x x position
     * @param y y position
     * @return true if in middle circle, false otherwise
     */
    private boolean isInMiddleCircle(float x, float y)
    {
        return isInCircle(x, y, CENTER_X, CENTER_Y, MIDDLE_RADIUS);
    }

    /**
     * Check if position is in inner circle
     *
     * @param x x position
     * @param y y position
     * @return true if in inner circle, false otherwise
     */
    private boolean isInInnerCircle(float x, float y)
    {
        return isInCircle(x, y, CENTER_X, CENTER_Y, INNER_RADIUS);
    }

    /**
     * Check if a position is within a circle
     *
     * @param x       x position
     * @param y       y position
     * @param centerX center x of circle
     * @param centerY center y of circle
     * @return true if within circle, false otherwise
     */
    private boolean isInCircle(float x, float y, float centerX, float centerY, float radius)
    {
        return Math.abs(x - centerX) < radius && Math.abs(y - centerY) < radius;
    }

    /**
     * Handler to process events. Used to move the cursor around and handle click events.
     * <p/>
     * <b>Current algorithm</b>
     * <br>
     * Device is has an outer circle, middle circle, and inner circle<br>
     * If initial touch is in outer circle, movement immediately occurs. Additional touches will
     * also cause movement.<br>
     * If initial touch is in the <b>middle circle</b>, a press occurs. Additional touches will only
     * cause movement once they are outside the <b>inner circle</b><br>
     */
    private class EventHandler extends Handler
    {

        private static final int SLEEP_FOR_CLICK_MS = 20;

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_PROCESS_START:
                    synchronized (positionData)
                    {
                        processControllerEventDifferential();
                    }
                    sendDelayedProcessing();
                    break;

                case MSG_PROCESS_STOP:
                    processStop();
                    prevPositionData.set(0.0f, 0.0f, 0.0f);
                    break;

                case MSG_PROCESS_ROTARY:
                    /**
                     * No TouchEnd event is received if user was pressing down on screen when
                     * rotary event arrives. Should simulate TouchEnd event if processing is
                     * currently occurring
                     */
                    if (hasMessages(MSG_PROCESS_START))
                    {
                        Log.v(TAG, "Rotary event, simulating TouchEnd event");
                        processStop();
                    }
                    synchronized (positionData)
                    {
                        processControllerEventDifferential();
                    }
                    break;

                default:
                    Log.w(TAG, "Unhandled msg.what=%d", msg.what);
                    break;
            }
        }

        void processStop() {
            removeMessages(MSG_PROCESS_START);
        }

        private void sendStartProcessing() {
            sendEmptyMessage(MSG_PROCESS_START);
        }

        private void sendDelayedProcessing()
        {
            sendEmptyMessageDelayed(MSG_PROCESS_START, PROCESS_EVENT_DELAY);
        }

        private void sendStopProcessing() {
            sendEmptyMessage(MSG_PROCESS_STOP);
        }

        private void sendProcessRotary() {
            sendEmptyMessage(MSG_PROCESS_ROTARY);
        }

        private void sendProcessSwipe() {
            sendEmptyMessage(MSG_PROCESS_SWIPE);
        }

        private void sendPressStart()
        {
            Log.d(TAG, "send press start");
            post(pressStart);
        }

        private void sendPressStop()
        {
            Log.d(TAG, "send press stop");
            post(pressEnd);
        }

        private Runnable pressStart = new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Key event: Action down");
                dispatchKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_1);
                synchronized (positionData)
                {
                    positionData.set(0.0f, 0.0f, 0.0f);
                    processControllerEventDifferential();
                }
                try
                {
                    //TODO: Workaround for ACTION_DOWN > ACTION_UP not working
                    TimeUnit.MILLISECONDS.sleep(SLEEP_FOR_CLICK_MS);
                }
                catch (InterruptedException e)
                {
                    Log.w(TAG, "", e);
                }
            }
        };

        private Runnable pressEnd = new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Key event: Action up");
                dispatchKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_1);
                synchronized (positionData)
                {
                    positionData.set(0.0f, 0.0f, 0.0f);
                    processControllerEventDifferential();
                }
            }
        };
    }
}
