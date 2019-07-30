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

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.samsungxr.SXRCamera;
import com.samsungxr.SXRCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSensor;
import com.samsungxr.SXRTransform;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.IPickEvents;
import com.samsungxr.ISensorEvents;
import com.samsungxr.ITouchEvents;
import com.samsungxr.SensorEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Define a class of type {@link SXRCursorController} to register a new cursor
 * controller with the {@link SXRInputManager}.
 * <p>
 * You can request for all the {@link SXRCursorController}s in the system by
 * querying the {@link SXRInputManager#getCursorControllers()} call.
 * Alternatively all notifications for {@link SXRCursorController}s attached or
 * detached from the framework can be obtained by registering a
 * {@link com.samsungxr.io.SXRInputManager.ICursorControllerListener} or
 * {@link com.samsungxr.io.SXRInputManager.ICursorControllerSelectListener
 * }with the {@link SXREventReceiver} of this input manager.
 * <p>
 * Make use of the {@link SXRCursorController#setCursor(SXRNode)} to
 * add a Cursor for the controller in 3D space. The {@link SXRInputManager} will
 * manipulate the {@link SXRNode} based on the input coming in to the
 * {@link SXRCursorController}.
 * @see SXRInputManager
 * @see com.samsungxr.io.SXRGearCursorController
 * @see com.samsungxr.io.SXRGazeCursorController
 */
public abstract class SXRCursorController implements IEventReceiver
{
    public enum CursorControl
    {
        NONE,
        CURSOR_CONSTANT_DEPTH,
        PROJECT_CURSOR_ON_SURFACE,
        ORIENT_CURSOR_WITH_SURFACE_NORMAL,
        CURSOR_DEPTH_FROM_CONTROLLER
    };


    /**
     * Implements thread-safe dragging of a scene hierarchy synchronized
     * with cursor movement
     */
    class Dragger implements Runnable
    {
        private SXRNode mDragMe;
        private SXRNode mDragParent;
        private boolean mDragging = false;
        private final Object mLock;

        public Dragger(Object lock) { mLock = lock; }

        public boolean start(SXRNode dragMe)
        {
            synchronized (mLock)
            {
                if (!mDragging)
                {
                    mDragMe = dragMe;
                    getSXRContext().runOnGlThreadPostRender(0, this);
                    return true;
                }
            }
            return false;
        }

        public boolean stop()
        {
            synchronized (mLock)
            {
                if (mDragging && (mDragMe != null))
                {
                    getSXRContext().runOnGlThreadPostRender(0, this);
                    return true;
                }
            }
            return false;

        }

        @Override
        public void run()
        {
            synchronized (mLock)
            {
                if (!mDragging)
                {
                    SXRTransform objTrans = mDragMe.getTransform();
                    Matrix4f cursorMtx = mDragRoot.getTransform().getModelMatrix4f();
                    Matrix4f objMatrix = objTrans.getModelMatrix4f();

                    mDragParent = mDragMe.getParent();
                    if (mDragParent != null)
                    {
                        mDragParent.removeChildObject(mDragMe);
                    }
                    cursorMtx.invert();
                    objTrans.setModelMatrix(cursorMtx.mul(objMatrix));
                    mDragRoot.addChildObject(mDragMe);
                    mDragging = true;
                }
                else
                {
                    SXRTransform objTrans = mDragMe.getTransform();
                    Matrix4f cursorMatrix = mDragRoot.getTransform().getModelMatrix4f();
                    mDragRoot.removeChildObject(mDragMe);
                    Matrix4f objMatrix = objTrans.getModelMatrix4f();

                    objTrans.setModelMatrix(cursorMatrix.mul(objMatrix));
                    if (mDragParent != null)
                    {
                        mDragParent.addChildObject(mDragMe);
                    }
                    else
                    {
                        scene.addNode(mDragMe);
                    }
                    mDragMe = null;
                    mDragParent = null;
                    mDragging = false;
                }
            }
        }
    };

    private static final String TAG = "SXRCursorController";
    private static int uniqueControllerId = 0;
    private final int controllerId;
    private final SXRControllerType controllerType;
    private boolean previousActive;
    private boolean active;
    protected float nearDepth = 0.50f;
    protected float farDepth = 50.0f;
    protected final Vector3f position, origin;
    protected List<KeyEvent> keyEvent;
    private List<KeyEvent> processedKeyEvent;
    protected List<MotionEvent> motionEvent;
    private List<MotionEvent> processedMotionEvent;
    private SXREventReceiver listeners;

    protected Object eventLock = new Object();
    protected SXRNode mCursor = null;
    protected boolean enable = true;
    protected Object mCursorLock = new Object();
    protected Dragger mDragger = new Dragger(mCursorLock);

    protected String name;
    protected int vendorId, productId;
    protected SXRScene scene = null;
    protected SXRPicker mPicker = null;
    protected CursorControl mCursorControl = CursorControl.PROJECT_CURSOR_ON_SURFACE;
    protected float mCursorDepth = 1.0f;
    protected SXRNode mCursorScale;
    protected SXRNode mDragRoot;
    protected SXRContext context;
    protected volatile boolean mConnected = false;
    protected int mTouchButtons = MotionEvent.BUTTON_SECONDARY | MotionEvent.BUTTON_PRIMARY;

    /**
     * Create an instance of {@link SXRCursorController} only using the
     * {@link SXRControllerType}.
     *
     * Note that this constructor creates a {@link SXRCursorController} with no
     * name (<code>null</code>) and vendor and product id set to 0.
     *
     * @param controllerType the type of this {@link SXRCursorController}.
     */
    public SXRCursorController(SXRContext context, SXRControllerType controllerType) {
        this(context, controllerType, null);
    }

    /**
     * Create an instance of {@link SXRCursorController} using a
     * {@link SXRControllerType} and a name.
     *
     * Note that this constructor creates a {@link SXRCursorController} with  vendor and product
     * id set to 0.
     *
     * @param controllerType the type of this {@link SXRCursorController}.
     * @param name           the name for this {@link SXRCursorController}
     */
    public SXRCursorController(SXRContext context, SXRControllerType controllerType, String name) {
        this(context, controllerType, name, 0, 0);
    }

    /**
     * Create an instance of {@link SXRCursorController}.
     *
     * @param controllerType the type of this {@link SXRCursorController}.
     * @param name           the name for this {@link SXRCursorController}
     * @param vendorId       the vendor id for this {@link SXRCursorController}
     * @param productId      the product id for this {@link SXRCursorController}
     */
    public SXRCursorController(SXRContext context, SXRControllerType controllerType, String name,
                               int vendorId, int productId) {
        this.context = context;
        this.controllerId = uniqueControllerId;
        this.controllerType = controllerType;
        this.name = name;
        this.vendorId = vendorId;
        this.productId = productId;
        uniqueControllerId++;
        position = new Vector3f(0, 0, -1);
        origin = new Vector3f(0, 0, 0);
        keyEvent = new ArrayList<KeyEvent>();
        processedKeyEvent = new ArrayList<KeyEvent>();
        motionEvent = new ArrayList<MotionEvent>();
        processedMotionEvent = new ArrayList<MotionEvent>();
        listeners = new SXREventReceiver(this);
        if (mPicker == null)
        {
            mPicker = new SXRPicker(this, false);
        }
        addPickEventListener(SXRSensor.getPickHandler());
        mCursorScale = new SXRNode(context);
        mCursorScale.setName("CursorController_CursorScale");
        mDragRoot = new SXRNode(context);
        mDragRoot.setName("CursorController_DragRoot");
        mDragRoot.addChildObject(mCursorScale);
    }

    /**
     * By default both primary and secondary buttons will cause a touch event.
     * This function allows you to set which buttons are "active" and
     * will produce ITouchEvents.
     *
     * @param button Android button states for touch (e.g. MotionEvent.BUTTON_PRIMARY)
     *                    * @see ITouchEvents
     * @see #getTouchButtons()
     */
    public void setTouchButtons(int button) { mTouchButtons = button; }

    /*
     * Gets the button states which will cause touch events.
     * @see ITouchEvents
     * @see #setTouchButtons(int)
     */
    public int getTouchButtons() { return mTouchButtons; }

    /**
     * Dispatch a key event for this controller.
     * @param event event to dispatch
     * @return true if event handled, false if event should be routed to the application
     */
    synchronized public boolean dispatchKeyEvent(KeyEvent event)
    {
        synchronized (eventLock) {
            this.keyEvent.add(event);
        }
        return true;
    }

    /**
     * Dispatch a motion event for this controller.
     * @param event event to dispatch
     * @return true if event handled, false if event should be routed to the application
     */
    synchronized public boolean dispatchMotionEvent(MotionEvent event)
    {
        synchronized (eventLock) {
            this.motionEvent.add(event);
        }
        return true;
    }

    /**
     * Return an id that represent this {@link SXRCursorController}
     *
     * @return an integer id that identifies this controller.
     */
    public int getId() {
        return controllerId;
    }

    /**
     * Get the {@link SXRContext} which owns this controller.
     * @return {@link SXRContext} for the controller
     */
    public SXRContext getSXRContext() { return context; }


    /**
     * Get the {@link SXREventReceiver} which dispatches {@link IControllerEvent}s
     * for this controller.
     * @link {@link SXREventReceiver} for the controller
     * @see SXREventReceiver#addListener(IEvents)
     */
    public SXREventReceiver getEventReceiver() { return listeners; }

    /**
     * Set a {@link SXRNode} to be controlled by the
     * {@link SXRCursorController}.
     *
     * @param object the {@link SXRNode} representing the cursor
     */
    public void setCursor(SXRNode object)
    {
        synchronized (mCursorLock)
        {
            if (object != null)
            {
                if ((mCursor != null) && (mCursor != object))
                {
                    detachCursor();
                }
                attachCursor(object);
                mCursor = object;
                moveCursor();
                object.setEnable(true);
            }
            else if (mCursor != null)
            {
                detachCursor();
            }
            mCursor = object;
        }
    }

    /**
     * Set the default distance of the cursor from the camera.
     * <p>
     * If the cursor is not projected on a surface, it is
     * displayed at this depth.
     * </p>
     * @param depth default cursor depth
     * @see #getCursorDepth()'
     * @see #setCursorControl(CursorControl)
     */
    public void setCursorDepth(float depth)
    {
        mCursorDepth = Math.abs(depth);
    }

    /**
     * Get the defrault distance of the cursor from the camera.
     * <p>
     * If the cursor is not projected on a surface, it is
     * displayed at this depth.
     * </p>
     * @return default cursor depth
     * @see #setCursorDepth(float)
     * @see #setCursorControl(CursorControl)
     */
    public float getCursorDepth()
    {
        return mCursorDepth;
    }

    /**
     * Return the {@link SXRNode} representing the current cursor.
     *
     * @return the current cursor or null if none
     * @see #setCursor(SXRNode)
     */
    public SXRNode getCursor()
    {
        synchronized (mCursorLock)
        {
            return mCursor;
        }
    }

    /**
     * The method will force a process cycle that may result in
     * {@link ISensorEvents} being generated if there is a significant event
     * that affects a {@link SXRSensor} or {@link IPickEvents} if the
     * cursor pick ray intersects a collider.
     * <p>
     * In most cases when a new position
     * or key event is received, the {@link SXRCursorController} internally
     * invalidates its own data. However there may be situations where the
     * controller data remains the same while the scene graph is changed. This
     * {@link #invalidate()} call can help force the {@link SXRCursorController}
     * to run a new process loop on its existing information against the changed
     * scene graph to generate possible {@link ISensorEvents} for
     * {@link SXRSensor}s.
     */
    public void invalidate() {
        // check if the controller is enabled
        if (isEnabled()) {
            update();
        }
    }

    /**
     * Return the {@link SXRControllerType} associated with the
     * {@link SXRCursorController}.
     * @return the {@link SXRControllerType} for the {@link SXRCursorController}
     */
    public SXRControllerType getControllerType() {
        return controllerType;
    }

    /**
     * Get the picker associated with this controller
     * @return SXRPicker used to pick for this controller
     */
    public SXRPicker getPicker() { return mPicker; }


    /**
     * Establishes how the cursor controller will control the cursor.
     * The cursor control options are specified with
     * {@link SXRCursorController.CursorControl}:
     * <table>
     * <tr><td>CURSOR_CONSTANT_DEPTH</td>
     *     <td>cursor is always kept at constant
     *         distance from origin of pick ray
     *     </td>
     * </tr>
     * <tr<td>PROJECT_CURSOR_ON_SURFACE</td>
     *    <td>cursor depth is changed dynamically to move the
     *        cursor to the hit point on the collider that was hit.
     *    </td>
     * </tr>
     * <tr><td>ORIENT_CURSOR_ON_SURFACE</td>
     *    <td>cursor depth is changed dynamically to move the
     *        cursor to the hit point on the collider that was hit.
     *        In addition the cursor is oriented along the surface normal
     *        in the case of a mesh collider with barycentric coordinate
     *        picking enabled {@link SXRMeshCollider#SXRMeshCollider(SXRContext, SXRMesh, boolean}
     *    </td>
     * </tr>
     * <tr><td>CURSOR_DEPTH_FROM_CONTROLLER</td>
     * <td>cursor depth can be changed by the user with the touchpad on the controller</td>
     * </table>
     * @param control cursor control options
     */
    public void setCursorControl(CursorControl control)
    {
        mCursorControl = control;
    }

    /**
     * Get the current cursor control options which indicate how
     * the distance of the cursor from the camera is controlled.
     * @return cursor control options
     * @see #setCursorControl(CursorControl)
     */
    public CursorControl getCursorControl() { return mCursorControl; }



    /**
     * Allows a single {@link SXRNode} to be dragged by the controller.
     * <p>
     * The object is added as a child of the controller until dragging is stopped.
     * Because the cursor is moved independently in a separate thread,
     * this function is required to parent the dragged object in a thread-safe way,
     * </p>
     * Only one object may be dragged at a time. If an object is already
     * being dragged, this function returns false and does nothing.
     * <p>
     * To stop dragging with the cursor, call {@link #stopDrag()}.
     * </p>
     * @param dragMe node to drag with controller
     * @return true if object is dragging, false on error
     * @see #stopDrag()
     */
    public boolean startDrag(SXRNode dragMe)
    {
        return mDragger.start(dragMe);
    }

    /**
     * Stops dragging the designated object with the controller.
     * <p>
     * Because the cursor is moved independently in a separate thread,
     * this function is required to disconnect the dragged object in a thread-safe way,
     * </p>
     * The {@link #startDrag(SXRNode)} function starts dragging an object
     * with the controller.
     * If an object is not being dragged, this function returns false and does nothing.
     * @return true if dragging is stopped, false if nothing dragged
     * @see #startDrag(SXRNode)
     */
    public boolean stopDrag()
    {
        return mDragger.stop();
    }

    /**
     * Get the all the key events processed by the {@link SXRCursorController}
     * if there are any. This call returns all the motion events reports since
     * the last callback was made.
     *
     * Note that not all {@link SXRCursorController} report {@link KeyEvent}s),
     * this function could also return an empty list for
     * {@link SXRCursorController}s that do not generate {@link KeyEvent}s.
     *
     * To get every {@link KeyEvent} reported by the {@link SXRCursorController}
     * use the {@link IControllerEvent} or the {@link ISensorEvents}
     * listener to query for the {@link KeyEvent} whenever a a callback is made.
     *
     * The {@link KeyEvent} would be valid for the lifetime of that callback and
     * would be reset to null on completion.
     *
     * @return the list of {@link KeyEvent}s processed by the
     * {@link SXRCursorController}.
     */
    public List<KeyEvent> getKeyEvents() {
        synchronized (eventLock) {
            return processedKeyEvent;
        }
    }

    /**
     * Get the latest key event processed by the {@link SXRCursorController} if
     * there is one (not all {@link SXRCursorController} report {@link KeyEvent}
     * s). Note that this value will be null if the latest event processed by
     * the {@link SXRCursorController} did not contain a {@link KeyEvent}.
     *
     * Note that this function also returns a null. To get every
     * {@link KeyEvent} reported by the {@link SXRCursorController} use the
     * {@link IControllerEvent} or the {@link ISensorEvents} listener to
     * query for the {@link KeyEvent} whenever a a callback is made.
     *
     * The {@link KeyEvent} would be valid for the lifetime of that callback and
     * would be reset to null on completion.
     *
     * @return the {@link KeyEvent} or null if there isn't one.
     */
    public KeyEvent getKeyEvent() {
        synchronized (eventLock) {
            if (processedKeyEvent.isEmpty()) {
                return null;
            } else {
                return processedKeyEvent.get(processedKeyEvent.size() - 1);
            }
        }
    }


    /**
     * Get the all the {@link MotionEvent}s processed by the
     * {@link SXRCursorController} if there are any. This call returns all the
     * motion events reports since the last callback was made.
     *
     * Note that not all {@link SXRCursorController}s report {@link MotionEvent}s,
     * this function also returns an empty list. To get every
     * {@link MotionEvent} reported by the {@link SXRCursorController} use the
     * {@link IControllerEvent} or the {@link ISensorEvents} listener to
     * query for the {@link MotionEvent}s whenever a a callback is made.
     *
     * The {@link MotionEvent}s reported would be valid for the lifetime of that
     * callback and would be recycled and reset on completion. Make use to the
     * {@link MotionEvent#obtain(MotionEvent)} to clone a copy of the
     * {@link MotionEvent}.
     *
     * @return a list of {@link MotionEvent}s processed by the
     * {@link SXRCursorController} .
     */
    public List<MotionEvent> getMotionEvents() {
        synchronized (eventLock) {
            return processedMotionEvent;
        }
    }

    /**
     * Get the latest {@link MotionEvent} processed by the
     * {@link SXRCursorController} if there is one (not all
     * {@link SXRCursorController}s report {@link MotionEvent}s)
     *
     * Note that this function also returns a null. To get every
     * {@link MotionEvent} reported by the {@link SXRCursorController} use the
     * {@link IControllerEvent} or the {@link ISensorEvents} listener to
     * query for the {@link MotionEvent} whenever a a callback is made.
     *
     * The {@link MotionEvent} would be valid for the lifetime of that callback
     * and would be recycled and reset to null on completion. Make use to the
     * {@link MotionEvent#obtain(MotionEvent)} to clone a copy of the
     * {@link MotionEvent}.
     *
     * @return the latest {@link MotionEvent} processed by the
     * {@link SXRCursorController} or null.
     */
    public MotionEvent getMotionEvent() {
        synchronized (eventLock) {
            if (processedMotionEvent.isEmpty()) {
                return null;
            } else {
                return MotionEvent.obtain(processedMotionEvent
                        .get(processedMotionEvent.size() - 1));
            }
        }
    }


    /**
     * Register a {@link IControllerEvent} to receive a callback whenever
     * the {@link SXRCursorController} has been updated.
     *
     * Use the {@link SXRCursorController} methods to query for information
     * about the {@link SXRCursorController}.
     */
    public interface IControllerEvent extends IEvents
    {
        void onEvent(SXRCursorController controller, boolean isActive);
    }

    /**
     * Add a {@link IControllerEvent} to receive updates from this
     * {@link SXRCursorController}.
     *
     * @param listener the {@link com.samsungxr.io.SXRInputManager.ICursorControllerListener} to be added.
     */
    public void addControllerEventListener(IControllerEvent listener) {
        listeners.addListener(listener);
    }

    /**
     * Remove the previously added {@link IControllerEvent}.
     *
     * @param listener {@link IControllerEvent} that was previously added .
     */
    public void removeControllerEventListener(IControllerEvent listener) {
        listeners.removeListener(listener);
    }

    /**
     * Add a {@link IPickEvents} or {@link ITouchEvents} listener to receive updates from this
     * {@link SXRCursorController}. A pick event is emitted whenever
     * the pick ray from the controller intersects a {@link SXRCollider}.
     * A touch event is emitted when the active button is pressed while
     * the pick ray is inside the collider.
     *
     * @param listener the {@link IPickEvents} or {@link ITouchEvents} listener to be added.
     */
    public void addPickEventListener(IEvents listener)
    {
        if (IPickEvents.class.isAssignableFrom(listener.getClass()) ||
                ITouchEvents.class.isAssignableFrom(listener.getClass()))
        {
            mPicker.getEventReceiver().addListener(listener);
        }
        else
        {
            throw new IllegalArgumentException("Pick event listener must be derive from IPickEvents or ITouchEvents");
        }
    }

    /**
     * Remove the previously added pick or touch listener.
     *
     * @param listener {@link IPickEvents} or {@link ITouchEvents} listener that was previously added.
     */
    public void removePickEventListener(IEvents listener)
    {
        mPicker.getEventReceiver().removeListener(listener);
    }

    /**
     * Use this method to enable or disable the {@link SXRCursorController}.
     *
     * By default the {@link SXRCursorController} is enabled. If disabled, the
     * controller would not report new positions for the cursor and would not
     * generate {@link SensorEvent}s to {@link SXRSensor}s.
     *
     * @param flag <code>true</code> to enable the {@link SXRCursorController},
     *               <code>false</code> to disable.
     */
    public void setEnable(boolean flag) {
        mPicker.setEnable(flag);
        mDragRoot.setEnable(flag);
        if (this.enable == flag)
        {
            // nothing to be done here, return
            return;
        }
        this.enable = flag;
        if (!flag)
        {
            // reset
            position.set(0, 0, -1);
            if (previousActive) {
                active = false;
            }

            synchronized (eventLock) {
                keyEvent.clear();
                motionEvent.clear();
            }
            update();
            context.getInputManager().removeCursorController(this);
        }
    }

    /**
     * Check if the {@link SXRCursorController} is enabled or disabled.
     *
     * By default the {@link SXRCursorController} is enabled.
     *
     * @return <code>true</code> if the {@link SXRCursorController} is enabled,
     * <code>false</code> otherwise.
     */
    public boolean isEnabled() {
        return enable;
    }

    /**
     * Check if the {@link SXRCursorController} is connected and providing
     * input data.
     * @return true if controller is connected, else false
     */
    public boolean isConnected() { return mConnected; }


    /**
     * Sets the x, y, z location from where the pick begins
     * Should match the location of the camera or the hand controller.
     * @param x X position of the camera
     * @param y Y position of the camera
     * @param z Z position of the camera
     */
    public void setOrigin(float x, float y, float z)
    {
        origin.set(x,y,z);
    }

    /**
     * Returns the origin of the pick ray for this controller.
     * @return X,Y,Z origin of picking ray
     */
    public Vector3f getOrigin()
    {
        return origin;
    }

    /**
     * This call sets the direction of the picking ray.
     * <p>
     * Use this call to set the picking direction
     * based on mouse or screen coordinates.
     *
     * @param x x direction of the pick ray.
     * @param y y direction of the pick ray.
     * @param z z direction of the pick ray.
     */
    public void setPosition(float x, float y, float z)
    {
        if (isEnabled())
        {
            position.set(x, y, z);
            position.normalize();
            mPicker.setPickRay(origin.x, origin.y, origin.z, position.x, position.y, position.z);
        }
    }

    /**
     * Get the direction of the picking ray for the
     * {@link SXRPicker} associated with this controller.
     * This ray is a unit vector.
     * @param pos vector updated with the picking ray direction.
     * @return input vector
     */
    public Vector3f getPosition(Vector3f pos)
    {
        pos.set(position);
        return pos;
    }

    /**
     * Set the near depth value for the controller. This is the closest the
     * {@link SXRCursorController} can get in relation to the {@link SXRCamera}.
     *
     * By default this value is set to zero.
     *
     * @param nearDepth
     */
    public void setNearDepth(float nearDepth) {
        this.nearDepth = nearDepth;
    }

    /**
     * Set the far depth value for the controller. This is the farthest the
     * {@link SXRCursorController} can get in relation to the {@link SXRCamera}.
     *
     * By default this value is set to negative {@link Float#MAX_VALUE}.
     *
     * @param farDepth
     */
    public void setFarDepth(float farDepth) {
        this.farDepth = farDepth;
    }

    /**
     * Get the near depth value for the controller.
     *
     * @return value representing the near depth. By default the value returned
     * is zero.
     */
    public float getNearDepth() {
        return nearDepth;
    }

    /**
     * Get the far depth value for the controller.
     *
     * @return value representing the far depth. By default the value returned
     * is negative {@link Float#MAX_VALUE}.
     */
    public float getFarDepth() {
        return farDepth;
    }

    /**
     * Get the product id associated with the {@link SXRCursorController}
     *
     * @return an integer representing the product id if there is one, else
     * return zero.
     */
    public int getProductId() {
        return productId;
    }

    /**
     * Get the vendor id associated with the {@link SXRCursorController}
     *
     * @return an integer representing the vendor id if there is one, else
     * return zero.
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * Get the name associated with the {@link SXRCursorController}.
     *
     * @return a string representing the {@link SXRCursorController} is there is
     * one, else return <code>null</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Change the scene associated with this controller (and
     * its associated picker). The picker is not enabled
     * and will not automatically pick. The controller
     * explicity calls SXRPicker.processPick each tima a
     * controller event is received.
     * @param scene The scene from which to pick colliders
     */
    public void setScene(SXRScene scene)
    {
        mPicker.setScene(scene);
        if (scene != null)
        {
            synchronized (mCursorLock)
            {
                if (mDragRoot.getParent() != null)
                {
                    mDragRoot.getParent().removeChildObject(mDragRoot);
                }
                scene.getMainCameraRig().addChildObject(mDragRoot);
            }
        }
        this.scene = scene;
    }

    /**
     * Use this method to set the active state of the{@link SXRCursorController}.
     * It indicates whether or not the "active" button is pressed.
     * <p>
     * It is up to the developer to map the designated button to the active flag.
     * Eg. A Gamepad could attach {@link KeyEvent#KEYCODE_BUTTON_A} to active.
     * @param active    Setting active to true causes the {@link SensorEvent} generated
     *                  from collisions with the cursor to have {@link SensorEvent#isActive()} as
     *                  <code>true</code>. Clearing it will emit events with <code>false</code>.
     *                  The active flag is also propagated to the picker, setting the value of
     *                  {@link SXRPicker.SXRPickedObject#touched}.
     */
    protected void setActive(boolean active)
    {
        if (isEnabled())
        {
            this.active = active;
        }
    }

    protected void attachCursor(SXRNode cursor)
    {
        SXRNode parent = cursor.getParent();
        if (parent != null)
        {
            parent.removeChildObject(cursor);
        }
        mCursorScale.addChildObject(cursor);
    }

    protected void detachCursor()
    {
        mCursorScale.removeChildObject(mCursor);
    }

    protected void updateCursor(SXRPicker.SXRPickedObject collision)
    {
        synchronized (mCursorLock)
        {
            SXRTransform cursorTrans = mDragRoot.getTransform();

            if (mCursorControl == CursorControl.NONE)
            {
                return;
            }
            if ((mCursorControl == CursorControl.CURSOR_CONSTANT_DEPTH) ||
                (mCursorControl == CursorControl.CURSOR_DEPTH_FROM_CONTROLLER))
            {
                cursorTrans.setPosition(position.x * mCursorDepth,
                        position.y * mCursorDepth,
                        position.z * mCursorDepth);
                return;
            }
            SXRNode parent = collision.hitObject.getParent();
            float dist = collision.hitDistance;
            float scale = dist / mCursorDepth;

            if (mCursorControl == CursorControl.ORIENT_CURSOR_WITH_SURFACE_NORMAL)
            {
                orientCursor(collision);
            }
            mCursorScale.getTransform().setScale(scale, scale, scale);
            while (parent != null)
            {
                if (parent == mDragRoot)
                {
                    return;
                }
                parent = parent.getParent();
            }
            float xcursor = position.x * dist;   // vector to hit position
            float ycursor = position.y * dist;
            float zcursor = position.z * dist;

            cursorTrans.getTransform().setPosition(xcursor, ycursor, zcursor);
        }
    }

    protected void moveCursor()
    {
        if (mCursorControl != CursorControl.NONE)
        {
            synchronized (mCursorLock)
            {
                SXRTransform trans = mDragRoot.getTransform();
                trans.setRotation(1, 0, 0, 0);
                trans.setPosition(position.x * mCursorDepth, position.y * mCursorDepth, position.z * mCursorDepth);
                mCursorScale.getTransform().setScale(1, 1, 1);
            }
        }
    }

    protected boolean orientCursor(SXRPicker.SXRPickedObject collision)
    {
        SXRNode parent = mCursorScale.getParent();
        float[] baryCoords = collision.getBarycentricCoords();
        boolean coordinatesCalculated = (baryCoords != null) && !Arrays.equals(baryCoords, new float[] {-1f, -1f, -1f});

        if ((parent != null) && coordinatesCalculated)
        {
            float[] normal = collision.getNormalCoords();
            Vector3f lookat = new Vector3f(normal[0], normal[1], normal[2]);
            Vector3f Xaxis = new Vector3f();
            Vector3f Yaxis = new Vector3f();
            Vector3f up = new Vector3f(0, 1, 0);

            up.cross(lookat.x, lookat.y, lookat.z, Xaxis);
            Xaxis = Xaxis.normalize();
            lookat.cross(Xaxis.x, Xaxis.y, Xaxis.z, Yaxis);
            Yaxis = Yaxis.normalize();
            Matrix3f orientMtx = new Matrix3f(Xaxis.x, Xaxis.y, Xaxis.z,
                    Yaxis.x, Yaxis.y, Yaxis.z,
                    lookat.x, lookat.y, lookat.z);
            Quaternionf orient = new Quaternionf();
            orient.setFromNormalized(orientMtx);
            Quaternionf cursorWtL = new Quaternionf();
            Quaternionf hitLtW = new Quaternionf();

            cursorWtL.setFromUnnormalized(parent.getTransform().getModelMatrix4f());
            hitLtW.setFromUnnormalized(collision.hitObject.getTransform().getModelMatrix4f());
            cursorWtL.invert();
            orient.mul(hitLtW);
            orient.mul(cursorWtL);
            orient.normalize();
            SXRTransform cursorTrans = mCursorScale.getTransform();
            cursorTrans.setRotation(orient.w, orient.x, orient.y, orient.z);
            return true;
        }
        return false;
    }

    /**
     * Set a key event. Note that this call can be used in lieu of
     * {@link SXRCursorController#setActive(boolean)}.
     *
     * The {@link SXRCursorController} processes a ACTION_DOWN
     * as active <code>true</code> and ACTION_UP as active
     * <code>false</code>.
     *
     * In addition the key event passed is used as a reference for applications
     * that wish to use the contents from the class.
     *
     * {@link #setActive(boolean)} can still be used for applications that do
     * not want to expose key events.
     *
     * @param keyEvent
     */
    protected void setKeyEvent(KeyEvent keyEvent) {
        synchronized (eventLock) {
            this.keyEvent.add(keyEvent);
        }
    }


    /**
     * Set the latest motion event processed by the {@link SXRCursorController}.
     *
     * Make sure not to recycle the passed {@link MotionEvent}. The
     * {@link SXRCursorController} will recycle the {@link MotionEvent} after
     * completion.
     *
     * @param motionEvent the {@link MotionEvent} processed by the
     *                    {@link SXRCursorController}.
     */
    protected void setMotionEvent(MotionEvent motionEvent) {
        synchronized (eventLock) {
            this.motionEvent.add(motionEvent);
        }
    }

    protected final class ControllerPick implements Runnable
    {
        public MotionEvent mEvent;
        public SXRPicker mPicker;
        public boolean mActive;
        public boolean mDoPick;

        public ControllerPick(SXRPicker picker, MotionEvent event, boolean active)
        {
            mPicker = picker;
            mEvent = event;
            mActive = active;
            mDoPick = (mEvent != null) || !mPicker.isEnabled();
        }

        public void run()
        {
            SXRPicker.SXRPickedObject[] picked = null;

            if (mDoPick)
            {
                mPicker.processPick(mActive, mEvent);
            }
            if (mEvent != null)
            {
                mEvent.recycle();
                mEvent = null;
            }
            picked = mPicker.getPicked();
            if ((picked != null) && (picked.length > 0))
            {
                SXRPicker.SXRPickedObject hit = picked[0];
                if (hit != null)
                {
                    updateCursor(hit);
                    return;
                }
            }
            moveCursor();
        }
    }

    /**
     * Update the state of the picker. If it has an owner, the picker
     * will use that object to derive its position and orientation.
     * The "active" state of this controller is used to indicate touch.
     * The cursor position is updated after picking.
     */
    protected void updatePicker(MotionEvent event, boolean isActive)
    {
        final MotionEvent newEvent = (event != null) ? event : null;
        final ControllerPick controllerPick = new ControllerPick(mPicker, newEvent, isActive);
        context.runOnGlThread(controllerPick);
    }

    /**
     * Process the input data.
     */
    private void update()
    {
        boolean hasEvents = false;
        // set the newly received key and motion events.
        synchronized (eventLock)
        {
            hasEvents = (keyEvent.size() > 0) || (motionEvent.size() > 0);
            processedKeyEvent.addAll(keyEvent);
            keyEvent.clear();
            processedMotionEvent.addAll(motionEvent);
            motionEvent.clear();
        }
        previousActive = active;
        if ((scene != null) && (mPicker != null))
        {
            updatePicker(getMotionEvent(), active);
        }
        context.getEventManager().sendEvent(this, IControllerEvent.class, "onEvent", this, active);

        // reset the set key and motion events.
        synchronized (eventLock)
        {
            processedKeyEvent.clear();
            processedMotionEvent.clear();
        }
    }

}
