/*
/* Copyright 2017 Samsung Electronics Co., LTD
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

import android.graphics.PointF;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.samsungxr.IApplicationEvents;
import com.samsungxr.SXRApplication;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventManager;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRImportSettings;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.nodes.SXRLineNode;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * This class represents the Gear Controller.
 *
 * The input manager notifies the application when the controller has mConnected successfully.
 * Add a {@link com.samsungxr.io.SXRInputManager.ICursorControllerListener)} to the {@link SXREventReceiver}
 * of this controller to get notified when the controller is available to use.
 * To query the device specific information from the
 * Gear Controller make sure to type cast the returned {@link SXRCursorController} to
 * {@link SXRGearCursorController} like below:
 *
 * <code>
 * GearController controller = (GearController) SXRCursorController;
 * </code>
 *
 * You can add a listener for {@link IControllerEvent} to receive
 * notification whenever the controller information is updated.
 */
public final class SXRGearCursorController extends SXRCursorController
{
    public interface ControllerReader
    {
        boolean isConnected(int index);

        void onPause();

        void onResume();

        String getModelFileName();

        void getEvents(int controllerID, ArrayList<ControllerEvent> mControllerEvents);
    }

    public static class ControllerReaderStubs implements ControllerReader
    {
        @Override
        public boolean isConnected(int index) {
            return false;
        }
        @Override
        public void onPause() { }
        @Override
        public void onResume() { }
        @Override
        public String getModelFileName(){
            return "gear_vr_controller.obj";
        }

        @Override
        public void getEvents(int controllerID, ArrayList<ControllerEvent> mControllerEvents) {}
    }

    public enum CONTROLLER_KEYS
    {
        BUTTON_NONE(0),
        BUTTON_A(0x00000001),
        BUTTON_ENTER(0x00100000),
        BUTTON_BACK(0x00200000),
        BUTTON_UP(0x00010000),
        BUTTON_DOWN(0x00020000),
        BUTTON_LEFT(0x00040000),
        BUTTON_RIGHT(0x00080000),
        BUTTON_VOLUME_UP(0x00400000),
        BUTTON_VOLUME_DOWN(0x00800000),
        BUTTON_HOME(0x01000000);
        private int numVal;

        CONTROLLER_KEYS(int numVal)
        {
            this.numVal = numVal;
        }

        public int getNumVal()
        {
            return numVal;
        }

        static public CONTROLLER_KEYS[] fromValue(int value) {
            if (0 == value) {
                return null;
            }

            int count = Integer.bitCount(value);
            CONTROLLER_KEYS[] result = new CONTROLLER_KEYS[count];

            if (0 != (value & 0x00000001)) {
                result[--count] = BUTTON_A;
            }
            if (0 != (value & 0x00100000)) {
                result[--count] = BUTTON_ENTER;
            }
            if (0 != (value & 0x00200000)) {
                result[--count] = BUTTON_BACK;
            }
            if (0 != (value & 0x00010000)) {
                result[--count] = BUTTON_UP;
            }
            if (0 != (value & 0x00020000)) {
                result[--count] = BUTTON_DOWN;
            }
            if (0 != (value & 0x00040000)) {
                result[--count] = BUTTON_LEFT;
            }
            if (0 != (value & 0x00080000)) {
                result[--count] = BUTTON_RIGHT;
            }
            if (0 != (value & 0x00400000)) {
                result[--count] = BUTTON_VOLUME_UP;
            }
            if (0 != (value & 0x00800000)) {
                result[--count] = BUTTON_VOLUME_DOWN;
            }
            if (0 != (value & 0x01000000)) {
                result[--count] = BUTTON_HOME;
            }
            return result;
        }
    }

    /**
     * Defines the handedness of the gear controller.
     */
    public enum Handedness
    {
        LEFT, RIGHT
    }

    private SXRNode mControllerModel;
    private SXRNode mRayModel;
    private final SXRNode mPivotRoot;
    private SXRNode mControllerGroup;
    private ControllerReader mControllerReader;
    private boolean mShowControllerModel = false;
    private Matrix4f mTempPivotMtx = new Matrix4f();
    private Quaternionf mTempRotation = new Quaternionf();
    private final MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
    private final MotionEvent.PointerProperties[] pointerPropertiesArray;
    private final MotionEvent.PointerCoords[] pointerCoordsArray;
    private long prevEnterTime;
    private long prevATime;
    private boolean actionDown = false;
    private float touchDownX = 0.0f;
    private final int controllerID;
    private static final float DEPTH_SENSITIVITY = 0.01f;

    private int prevButtonEnter = KeyEvent.ACTION_UP;
    private int prevButtonA = KeyEvent.ACTION_UP;
    private int prevButtonBack = KeyEvent.ACTION_UP;
    private int prevButtonVolumeUp = KeyEvent.ACTION_UP;
    private int prevButtonVolumeDown = KeyEvent.ACTION_UP;
    private int prevButtonHome = KeyEvent.ACTION_UP;
    private ControllerEvent currentControllerEvent;

    public SXRGearCursorController(SXRContext context, int id)
    {
        super(context, SXRControllerType.CONTROLLER);
        controllerID = id;
        mPivotRoot = new SXRNode(context);
        mPivotRoot.setName("GearCursorController_Pivot");
        mControllerGroup = new SXRNode(context);
        mControllerGroup.setName("GearCursorController_ControllerGroup");
        mPivotRoot.addChildObject(mControllerGroup);
        mControllerGroup.addChildObject(mDragRoot);
        mControllerGroup.attachComponent(mPicker);
        position.set(0.0f, 0.0f, -1.0f);
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerPropertiesArray = new MotionEvent.PointerProperties[]{properties};
        pointerCoordsArray = new MotionEvent.PointerCoords[]{pointerCoords};
        mPropagateEvents = new SendEvents(context);
    }

    public void attachReader(ControllerReader reader)
    {
        mControllerReader = reader;
    }

    /**
     * Get the ID of this controller.
     * It is a 0-based integer (either 0 or 1)
     * that is established when the SXRGearCursorController
     * instance is created.
     * @return controller ID
     */
    public int getControllerID() { return controllerID; }

    /**
     * Show or hide the controller model and picking ray.
     * <p>
     * The nodes remain in the scene but they are not rendered.
     *
     * @param flag true to show the model and ray, false to hide it.
     */
    public void showControllerModel(boolean flag)
    {
        boolean show = flag && isEnabled();
        mShowControllerModel = flag;
        if (mControllerModel != null)
        {
            mControllerModel.setEnable(show);
            mControllerGroup.setEnable(show);
        }
        else if (show)
        {
            createControllerModel();
        }
    }

    /**
     * Get the model currently being used to depict the controller
     * in the scene.
     * @return controller model
     * @see #setControllerModel(SXRNode)
     * @see #showControllerModel(boolean)
     */
    public SXRNode getControllerModel() { return mControllerModel; }

    /**
     * Replaces the model used to depict the controller in the scene.
     *
     * @param controllerModel root of hierarchy to use for controller model
     * @see #getControllerModel()
     * @see #showControllerModel(boolean)
     */
    public void setControllerModel(SXRNode controllerModel)
    {
        if (mControllerModel != null)
        {
            mControllerGroup.removeChildObject(mControllerModel);
        }
        mControllerModel = controllerModel;
        mControllerGroup.addChildObject(mControllerModel);
        mControllerModel.setEnable(mShowControllerModel);
    }

    /**
     * Set the depth of the cursor.
     * This is the length of the ray from the origin
     * to the cursor.
     * @param depth default cursor depth
     */
    @Override
    public void setCursorDepth(float depth)
    {
        super.setCursorDepth(depth);
        if (mRayModel != null)
        {
            mRayModel.getTransform().setScaleZ(mCursorDepth);
        }
    }


    protected void updateCursor(SXRPicker.SXRPickedObject collision)
    {
        super.updateCursor(collision);
        if (mRayModel != null)
        {
            if ((mCursorControl == CursorControl.PROJECT_CURSOR_ON_SURFACE) ||
                (mCursorControl == CursorControl.ORIENT_CURSOR_WITH_SURFACE_NORMAL))
            {
                mRayModel.getTransform().setScaleZ(collision.hitDistance);
            }
            else
            {
                mRayModel.getTransform().setScaleZ(mCursorDepth);
            }
        }
    }

    protected void moveCursor()
    {
        super.moveCursor();
        if (mRayModel != null)
        {
            mRayModel.getTransform().setScaleZ(mCursorDepth);
        }
    }

    /**
     * Set the position of the pick ray.
     * This function is used internally to update the
     * pick ray with the new controller position.
     * @param x the x value of the position.
     * @param y the y value of the position.
     * @param z the z value of the position.
     */
    @Override
    public void setPosition(float x, float y, float z)
    {
        super.setPosition(x, y, z);
        invalidate();
    }

    @Override
    public void setEnable(boolean flag)
    {
        super.setEnable(flag);
        mControllerGroup.setEnable(flag);
    }

    private void createControllerModel()
    {
        if (mRayModel == null)
        {
            mRayModel = new SXRLineNode(context, 1, new Vector4f(1, 0, 0, 1),
                                               new Vector4f(1, 0, 0, 0));
            final SXRRenderData renderData = mRayModel.getRenderData();
            final SXRMaterial rayMaterial = renderData.getMaterial();

            mRayModel.setName("GearCursorController_Ray");
            rayMaterial.setLineWidth(4.0f);
            renderData.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY + 10);
            renderData.setDepthTest(false);
            renderData.setAlphaBlend(true);
            mControllerGroup.addChildObject(mRayModel);
            mRayModel.getTransform().setScaleZ(mCursorDepth);
        }
        try
        {
            EnumSet<SXRImportSettings> settings = SXRImportSettings.getRecommendedSettingsWith(
                    EnumSet.of(SXRImportSettings.NO_LIGHTING));

            mControllerModel =
                    context.getAssetLoader().loadModel(mControllerReader.getModelFileName(), settings, true,
                                                       null);
        }
        catch (IOException ex)
        {
            Log.e(TAG, "cannot load controller model gear_vr_controller.obj");
            return;
        }
        mControllerGroup.addChildObject(mControllerModel);
        mControllerGroup.setEnable(true);
    }

    @Override
    public void setScene(SXRScene scene)
    {
        synchronized (mPivotRoot) {
            SXRNode parent = mPivotRoot.getParent();

            mPicker.setScene(scene);
            this.scene = scene;
            if (parent != null) {
                parent.removeChildObject(mPivotRoot);
            }
            if (scene != null) {
                scene.addNode(mPivotRoot);
            }
        }
        showControllerModel(mShowControllerModel);
    }

    private final ArrayList<ControllerEvent> mControllerEvents = new ArrayList<>();
    public void pollController()
    {
        boolean wasConnected = mConnected;

        mConnected = (mControllerReader != null) && mControllerReader.isConnected(controllerID);
        if (!wasConnected && mConnected)
        {
            context.getInputManager().addCursorController(this);
        }
        else if (wasConnected && !mConnected)
        {
            context.getInputManager().removeCursorController(this);
            return;
        }
        if (isEnabled())
        {
            mControllerEvents.clear();
            try {
                mControllerReader.getEvents(controllerID, mControllerEvents);
            } catch (final RuntimeException exc) {
                Log.e(TAG, "getEvents threw: " + exc.toString());
                exc.printStackTrace();
            }

            for (final ControllerEvent event: mControllerEvents) {
                handleControllerEvent(event);
            }
        }
    }

    public synchronized boolean dispatchKeyEvent(KeyEvent e)
    {
        return false;
    }

    public synchronized boolean dispatchMotionEvent(MotionEvent e)
    {
        return false;
    }

    /**
     * Return the current position of the Gear Controller.
     *
     * @return a {@link Vector3f} representing the position of the controller. This function
     * returns <code>null</code> if the controller is unavailable or the data is stale.
     */
    public Vector3f getPosition()
    {
        if ((currentControllerEvent == null) ||
            currentControllerEvent.isRecycled())
        {
            return null;
        }
        return currentControllerEvent.position;
    }

    /**
     * Return the current rotation of the Gear Controller.
     *
     * @return a {@link Quaternionf} representing the rotation of the controller. This function
     * returns <code>null</code> if the controller is unavailable or the data is stale.
     */
    public Quaternionf getRotation()
    {
        if ((currentControllerEvent == null) ||
            currentControllerEvent.isRecycled())
        {
            return null;
        }
        return currentControllerEvent.rotation;
    }

    /**
     * Return the current touch coordinates of the Gear Controller touchpad.
     *
     * @return a {@link PointF} representing the touch coordinates on the controller. If the
     * user is not using the touchpad (0.0f, 0.0f) is returned. This function
     * returns <code>null</code> if the controller is unavailable or the data is stale.
     */
    public PointF getTouch()
    {
        if ((currentControllerEvent == null) ||
            currentControllerEvent.isRecycled())
        {
            return null;
        }
        return currentControllerEvent.pointF;
    }

    /**
     * Return the current handedness of the Gear Controller.
     *
     * @return returns whether the user is using the controller left or right handed. This function
     * returns <code>null</code> if the controller is unavailable or the data is stale.
     */
    @SuppressWarnings("unused")
    public Handedness getHandedness()
    {
        if ((currentControllerEvent == null) || currentControllerEvent.isRecycled())
        {
            return null;
        }
        return currentControllerEvent.handedness == 0.0f ?
                Handedness.LEFT : Handedness.RIGHT;
    }

    private final SendEvents mPropagateEvents;

    @Override
    protected void updatePicker(MotionEvent event, boolean isActive)
    {
        MotionEvent newEvent = (event != null) ? MotionEvent.obtain(event) : null;
        final ControllerPick controllerPick = new ControllerPick(mPicker, newEvent,isActive);
        controllerPick.run();
    }

    private void handleControllerEvent(final ControllerEvent event)
    {
        context.getEventManager().sendEvent(context.getApplication(), IApplicationEvents.class,
                                            "onControllerEvent",
                                            CONTROLLER_KEYS.fromValue(event.key), event.position, event.rotation, event.pointF,
                                            event.touched, event.angularAcceleration,event.angularVelocity);

        this.currentControllerEvent = event;
        int key = event.key;
        Quaternionf q = event.rotation;
        Vector3f pos = event.position;
        Matrix4f camMtx = context.getMainScene().getMainCameraRig().getTransform().getModelMatrix4f();
        float x = camMtx.m30();
        float y = camMtx.m31();
        float z = camMtx.m32();

        q.normalize();
        camMtx.getNormalizedRotation(mTempRotation);
        mTempRotation.transform(pos);           // rotate controller position by camera orientation
        x += pos.x;
        y += pos.y;
        z += pos.z;
        mTempRotation.mul(q);
        mTempPivotMtx.rotation(mTempRotation);  // translate pivot by combined event and camera translation
        mTempPivotMtx.setTranslation(x, y, z);
        synchronized (mPivotRoot) {
            mPivotRoot.getTransform().setModelMatrix(mTempPivotMtx);
        }
        setOrigin(x, y, z);

        int handleResult = handleEnterButton(key, event.pointF, event.touched);
        prevButtonEnter = handleResult == -1 ? prevButtonEnter : handleResult;

        handleResult = handleAButton(key);
        prevButtonA = handleResult == -1 ? prevButtonA : handleResult;

        handleResult = handleButton(key, CONTROLLER_KEYS.BUTTON_BACK,
                                    prevButtonBack, KeyEvent.KEYCODE_BACK);
        prevButtonBack = handleResult == -1 ? prevButtonBack : handleResult;

        handleResult = handleButton(key, CONTROLLER_KEYS.BUTTON_VOLUME_UP,
                                    prevButtonVolumeUp, KeyEvent.KEYCODE_VOLUME_UP);
        prevButtonVolumeUp = handleResult == -1 ? prevButtonVolumeUp : handleResult;

        handleResult = handleButton(key, CONTROLLER_KEYS.BUTTON_VOLUME_DOWN,
                                    prevButtonVolumeDown, KeyEvent.KEYCODE_VOLUME_DOWN);
        prevButtonVolumeDown = handleResult == -1 ? prevButtonVolumeDown : handleResult;

        handleResult = handleButton(key, CONTROLLER_KEYS.BUTTON_HOME,
                                    prevButtonHome, KeyEvent.KEYCODE_HOME);
        prevButtonHome = handleResult == -1 ? prevButtonHome : handleResult;
        event.recycle();
        if (keyEvent.size() > 0 || motionEvent.size() > 0)
        {
            mPropagateEvents.init(keyEvent, motionEvent);
            getSXRContext().getActivity().runOnUiThread(mPropagateEvents);
        }
        invalidate();
    }

    private int handleEnterButton(int key, PointF pointF, boolean touched)
    {
        long time = SystemClock.uptimeMillis();
        int handled = handleButton(key, CONTROLLER_KEYS.BUTTON_ENTER, prevButtonEnter,
                                   KeyEvent.KEYCODE_ENTER);
        if ((handled == KeyEvent.ACTION_UP) || (actionDown && !touched))
        {
            pointerCoords.x = pointF.x;
            pointerCoords.y = pointF.y;
            MotionEvent motionEvent = MotionEvent.obtain(prevEnterTime, time,
                                                         MotionEvent.ACTION_UP, 1,
                                                         pointerPropertiesArray, pointerCoordsArray,
                                                         0, MotionEvent.BUTTON_PRIMARY, 1f, 1f, 0,
                                                         0, InputDevice.SOURCE_TOUCHPAD, 0);
            setMotionEvent(motionEvent);
            setActive(false);
        }
        else if ((handled == KeyEvent.ACTION_DOWN) || (touched && !actionDown))
        {
            pointerCoords.x = pointF.x;
            pointerCoords.y = pointF.y;
            MotionEvent motionEvent = MotionEvent.obtain(time, time,
                                                         MotionEvent.ACTION_DOWN, 1,
                                                         pointerPropertiesArray,
                                                         pointerCoordsArray,
                                                         0, MotionEvent.BUTTON_PRIMARY, 1f, 1f,
                                                         0, 0, InputDevice.SOURCE_TOUCHPAD, 0);
            setMotionEvent(motionEvent);
            if ((mTouchButtons & MotionEvent.BUTTON_PRIMARY) != 0)
            {
                setActive(true);
            }
            prevEnterTime = time;
        }
        else if (actionDown && touched)
        {
            pointerCoords.x = pointF.x;
            pointerCoords.y = pointF.y;
            MotionEvent motionEvent = MotionEvent.obtain(prevEnterTime, time,
                                                         MotionEvent.ACTION_MOVE, 1,
                                                         pointerPropertiesArray, pointerCoordsArray,
                                                         0, MotionEvent.BUTTON_PRIMARY, 1f, 1f, 0,
                                                         0, InputDevice.SOURCE_TOUCHPAD, 0);
            setMotionEvent(motionEvent);
        }
        /*
         * If the controller is allowed to change the cursor depth,
         * update it from the X delta on the controller touchpad.
         * The near and far depth values are NEGATIVE,
         * the controller depth is POSITIVE, hence the strange math.
         */
        if (touched && (mCursorControl == CursorControl.CURSOR_DEPTH_FROM_CONTROLLER))
        {
            float cursorDepth = getCursorDepth();
            float dx = pointF.x;

            if (!actionDown)
            {
                touchDownX = dx;
            }
            else
            {
                dx -= touchDownX;
                cursorDepth += dx * DEPTH_SENSITIVITY;
                if ((cursorDepth >= getNearDepth()) && (cursorDepth <= getFarDepth()))
                {
                    setCursorDepth(cursorDepth);
                }
            }
        }
        actionDown = touched;
        return handled;
    }

    private int handleAButton(int key)
    {
        long time = SystemClock.uptimeMillis();
        int handled = handleButton(key, CONTROLLER_KEYS.BUTTON_A, prevButtonA, KeyEvent.KEYCODE_A);
        if (handled == KeyEvent.ACTION_UP)
        {
            setActive(false);
            pointerCoords.x = 0;
            pointerCoords.y = 0;
            MotionEvent motionEvent = MotionEvent.obtain(prevATime, time,
                                                         MotionEvent.ACTION_UP, 1,
                                                         pointerPropertiesArray, pointerCoordsArray,
                                                         0, MotionEvent.BUTTON_SECONDARY, 1f, 1f, 0,
                                                         0, InputDevice.SOURCE_TOUCHPAD, 0);
            setMotionEvent(motionEvent);
            Log.d(TAG, "handleAButton action=%d button=%d x=%f y=%f",
                  motionEvent.getAction(), motionEvent.getButtonState(), motionEvent.getX(),
                  motionEvent.getY());
        }
        else if (handled == KeyEvent.ACTION_DOWN)
        {
            pointerCoords.x = 0;
            pointerCoords.y = 0;
            MotionEvent motionEvent = MotionEvent.obtain(time, time,
                                                         MotionEvent.ACTION_DOWN, 1,
                                                         pointerPropertiesArray, pointerCoordsArray,
                                                         0, MotionEvent.BUTTON_SECONDARY, 1f, 1f, 0,
                                                         0, InputDevice.SOURCE_TOUCHPAD, 0);
            setMotionEvent(motionEvent);
            prevATime = time;
            if ((mTouchButtons & MotionEvent.BUTTON_SECONDARY) != 0)
            {
                setActive(true);
            }
            Log.d(TAG, "handleAButton action=%d button=%d x=%f y=%f",
                  motionEvent.getAction(), motionEvent.getButtonState(), motionEvent.getX(),
                  motionEvent.getY());
        }
        return handled;
    }

    private int handleButton(int key, CONTROLLER_KEYS button, int prevButton, int keyCode)
    {
        if ((key & button.getNumVal()) != 0)
        {
            Log.d(TAG, "keyPress button=%d code=%d", button.getNumVal(), keyCode);
            if (prevButton != KeyEvent.ACTION_DOWN)
            {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                setKeyEvent(keyEvent);
                return KeyEvent.ACTION_DOWN;
            }
        }
        else
        {
            if (prevButton != KeyEvent.ACTION_UP)
            {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                setKeyEvent(keyEvent);
                return KeyEvent.ACTION_UP;
            }
        }
        return -1;
    }

    public static final class ControllerEvent
    {
        @Override
        public String toString() {
            return "ControllerEvent{" +
                    "next=" + next +
                    ", rotation=" + rotation +
                    ", position=" + position +
                    ", angularVelocity=" + angularVelocity +
                    ", angularAcceleration=" + angularAcceleration +
                    ", pointF=" + pointF +
                    ", key=" + key +
                    ", handedness=" + handedness +
                    ", recycled=" + recycled +
                    ", touched=" + touched +
                    '}';
        }

        private static final int MAX_RECYCLED = 5;
        private static final Object recyclerLock = new Object();
        private static int recyclerUsed;
        private static ControllerEvent recyclerTop;
        private ControllerEvent next;
        public Quaternionf rotation = new Quaternionf();
        public Vector3f position = new Vector3f();
        public Vector3f angularVelocity = new Vector3f();
        public Vector3f angularAcceleration = new Vector3f();
        public PointF pointF = new PointF();
        public int key;
        public float handedness;
        private boolean recycled = false;
        public boolean touched = false;

        public static ControllerEvent obtain()
        {
            final ControllerEvent event;
            synchronized (recyclerLock)
            {
                event = recyclerTop;
                if (event == null)
                {
                    return new ControllerEvent();
                } else {
                    event.handedness = SXRGearCursorController.Handedness.RIGHT.ordinal();
                    event.pointF.set(0, 0);
                    event.key = 0;
                    event.touched = false;
                    event.angularAcceleration.set(0, 0, 0);
                    event.angularVelocity.set(0, 0, 0);
                }
                event.recycled = false;
                recyclerTop = event.next;
                recyclerUsed -= 1;
            }
            event.next = null;
            return event;
        }

        final void recycle()
        {
            synchronized (recyclerLock)
            {
                if (recyclerUsed < MAX_RECYCLED)
                {
                    recyclerUsed++;
                    next = recyclerTop;
                    recyclerTop = this;
                    recycled = true;
                }
            }
        }

        boolean isRecycled()
        {
            return recycled;
        }
    }

    public static final class SendEvents implements Runnable
    {
        private final ConcurrentLinkedQueue<KeyEvent> mKeyEvents = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<MotionEvent> mMotionEvents =
                new ConcurrentLinkedQueue<>();
        private final SXRContext mContext;

        SendEvents(final SXRContext context)
        {
            mContext = context;
        }

        public void init(List<KeyEvent> keyEvents, List<MotionEvent> motionEvents)
        {
            mKeyEvents.addAll(keyEvents);
            mMotionEvents.addAll(motionEvents);
        }

        public void run() {
            final SXRApplication application = mContext.getApplication();
            for (final Iterator<KeyEvent> it = mKeyEvents.iterator(); it.hasNext(); ) {
                final KeyEvent e = it.next();
                mContext.getEventManager().sendEventWithMask(
                        SXREventManager.SEND_MASK_ALL & ~SXREventManager.SEND_MASK_OBJECT,
                        application,
                        IApplicationEvents.class,
                        "dispatchKeyEvent", e);
                it.remove();
            }

            for (Iterator<MotionEvent> it = mMotionEvents.iterator(); it.hasNext(); ) {
                final MotionEvent e = it.next();
                final MotionEvent dupe = MotionEvent.obtain(e);
                it.remove();

                //@todo move the io package back to gearvrf
                mContext.getEventManager().sendEventWithMask(
                        SXREventManager.SEND_MASK_ALL & ~SXREventManager.SEND_MASK_OBJECT,
                        application,
                        IApplicationEvents.class,
                        "dispatchTouchEvent", dupe);

                dupe.recycle();
            }
        }
    }

    private static final String TAG = "SXRGearCursorController";
}
