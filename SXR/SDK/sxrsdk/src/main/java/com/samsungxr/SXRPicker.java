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

package com.samsungxr;

import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.samsungxr.io.SXRCursorController;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Finds the scene objects that are hit by a ray or sphere.
 *
 * The picker can function in two modes. One way is to simply call its
 * static functions to make a single scan through the scene to determine
 * what is hit by the picking ray.
 * <p/>
 * The other way is to add the picker as a component to a scene object.
 * Usually, the scene object is the camera. In this case,
 * the picking ray is generated from the camera viewpoint
 * each frame. It's origin is the camera position and it's direction is
 * the camera forward look vector (what the user is looking at).
 * <p/>
 * For a {@linkplain SXRNode scene object} to be pickable, it must have a
 * {@link SXRCollider} component attached to it that is enabled.
 * The picker "casts" a ray into the screen graph, and returns an array
 * containing all the collisions as instances of SXRPickedObject.
 * The picked object contains the collider instance hit, the distance from the
 * camera and the hit position.
 * <p/>
 * The picker maintains the list of currently picked objects
 * (which can be obtained with getPicked()) and continually updates it each frame.
 * <p/>
 * In this mode, when the ray from the scene object hits a pickable object,
 * the picker generates one or more pick events (IPickEvents interface)
 * which are sent the event receiver of the scene. These events can be
 * observed by listeners.
 * <ul>
 * <li>onEnter(SXRNode)  called when the pick ray enters a scene object.</li>
 * <li>onExit(SXRNode)   called when the pick ray exits a scene object.</li>
 * <li>onInside(SXRNode) called while the pick ray penetrates a scene object.</li>
 * <li>onPick(SXRPicker)        called every frame if something is picked.</li>
 * <li>onNoPick(SXRPicker)      called every frame if nothing is picked.</li>
 * </ul
 * Each cursor controller has an internal picker that generates pick events
 * from that cursor. When a cursor is used, the pick events contain touch
 * information. You can register as a listener for pick events from a specific cursor.
 * @see IPickEvents
 * @see SXRNode#attachCollider(SXRCollider)
 * @see SXRCollider
 * @see SXRCollider#setEnable(boolean)
 * @see SXRPickedObject
 * @see SXRCursorController#addPickEventListener(IEvents)
 */
public class SXRPicker extends SXRBehavior implements IEventReceiver {
    private static final String TAG = Log.tag(SXRPicker.class);
    static private long TYPE_PICKMANAGER = newComponentType(SXRPicker.class);
    private final Vector3f mRayOrigin = new Vector3f(0, 0, 0);
    private final Vector3f mRayDirection = new Vector3f(0, 0, -1);
    private final float[] mPickRay = new float[6];
    protected volatile boolean mTouched = false;
    protected volatile MotionEvent mMotionEvent = null;
    protected SXRScene mScene = null;
    protected SXRCursorController mController = null;
    protected SXRPickedObject[] mPicked = null;
    protected boolean mPickClosest = true;
    protected SXREventReceiver mListeners = null;
    protected Lock mPickEventLock = new ReentrantLock();
    protected boolean mPickListChanged = false;
    protected EnumSet<EventOptions> mEventOptions = EnumSet.of(
            EventOptions.SEND_PICK_EVENTS,
            EventOptions.SEND_TO_SCENE,
            EventOptions.SEND_TO_HIT_OBJECT,
            EventOptions.SEND_TO_LISTENERS);

    /**
     * One or more of these options may be combined to control
     * what events the picker sends and to which objects.
     * <table>
     *     <tr><td>SEND_PICK_EVENTS</td>
     *     Send events using the {@link IPickEvents} interface.
     *     </tr>
     *     <tr><td>SEND_TOUCH_EVENTS</td>
     *     Send events using the {@link ITouchEvents} interface.
     *     </tr>
     *     <tr><td>SEND_TO_LISTENERS</td>
     *     Send events to the pick event listeners attached to the {@link SXREventReceiver}
     *     of this picker.
     *     </tr>
     *     <tr><td>SEND_TO_HIT_OBJECT</td>
     *     Send touch events to the event listeners attached to the object that was hit.
     *     Pick events are not sent to hit objects.
     *     </tr>
     *     <tr><td>SEND_TO_SCENE</td>
     *     Send pick and touch events to the event listeners attached to the scene associated
     *     with the picker.
     *     </tr>
     * </table>
     * @see #setEventOptions(EnumSet)
     */
    public enum EventOptions
    {
        SEND_PICK_EVENTS,
        SEND_TOUCH_EVENTS,
        SEND_TO_HIT_OBJECT,
        SEND_TO_LISTENERS,
        SEND_TO_SCENE
    };


    /**
     * Construct a picker which picks from the camera of a given scene.
     * Instantiating the picker will cause it to scan the scene
     * every frame and generate pick events based on the result.
     * This constructor enables the SEND_PICK_EVENTS, SEND_TO_LISTENERS
     * and SEND_TO_SCENE event options.
     * @param context context that owns the scene
     * @param scene scene containing the scene objects to pick from
     * @see #getScene()
     * @see #setScene(SXRScene)
     * @see #setEventOptions(EnumSet)
     */
    public SXRPicker(SXRContext context, SXRScene scene)
    {
        this(scene, true);
    }

    /**
     * Construct a picker which picks from a given scene
     * using a ray emanating from the specified scene object.
     * The picker will be attached to the scene object and
     * will scan the scene every frame and generate pick events.
     * <p>
     * This constructor is useful when you want to pick from the
     * viewpoint of a scene object. It enables the SEND_PICK_EVENTS,
     * SEND_TO_LISTENERS and SEND_TO_SCENE event options.
     *
     * @param owner scene object to own the picker
     * @param scene scene containing the scene objects to pick from
     * @see #getScene()
     * @see #setScene(SXRScene)
     * @see #setEventOptions(EnumSet)
     */
    public SXRPicker(SXRNode owner, SXRScene scene)
    {
        this(scene, false);
        owner.attachComponent(this);
    }

    /**
     * Construct a picker which picks from a given cursor controller.
     * <p>
     * Instantiating the picker will cause it to generate pick and touch
     * events from the position and direction specified by the controller.
     * This constructor enables the SEND_PICK_EVENTS, SEND_TOUCH_EVENTS
     * and SEND_TO_LISTENERS event options.
     * @param controller {@link SXRCursorController} which will generate the pick ray.
     * @param enable true to start in the enabled state (listening for events)
     * @see #getController()
     * @see #setEventOptions(EnumSet)
     */
    public SXRPicker(SXRCursorController controller, boolean enable)
    {
        super(controller.getSXRContext());
        mScene = null;
        mType = getComponentType();
        mListeners = new SXREventReceiver(this);
        setPickRay(0, 0, 0, 0, 0, -1);
        mController = controller;
        mEventOptions = EnumSet.of(
                EventOptions.SEND_PICK_EVENTS,
                EventOptions.SEND_TOUCH_EVENTS,
                EventOptions.SEND_TO_HIT_OBJECT,
                EventOptions.SEND_TO_LISTENERS);
        if (!enable)
        {
            setEnable(enable);
        }
        else
        {
            startListening();
        }
    }

    /**
     * Construct a picker which picks from a given scene.
     *<p>
     * This constructor enables the SEND_PICK_EVENTS
     * and SEND_TO_LISTENERS event options.
     * @param scene scene containing the scene objects to pick from
     * @param enable true to start in the enabled state (listening for events)
     * @see #setEventOptions(EnumSet)
     */
    public SXRPicker(SXRScene scene, boolean enable)
    {
        super(scene.getSXRContext());
        mScene = scene;
        mType = getComponentType();
        mListeners = new SXREventReceiver(this);
        setPickRay(0, 0, 0, 0, 0, -1);
        if (!enable)
        {
            setEnable(enable);
        }
        else
        {
            startListening();
        }
    }

    static public long getComponentType() { return TYPE_PICKMANAGER; }


    /**
     * Sets the event options which control what events
     * the picker sends and to which objects.
     * The picker implements {@link IEventReceiver} which keeps a list
     * of listeners for {@link IPickEvents} and {@link ITouchEvents}.
     * You can add a listener with {@link SXREventReceiver#addListener(IEvents)}.
     * <p>
     * The default event options are <i>SEND_PICK_EVENTS, SEND_TOUCH_EVENTS, SEND_TO_LISTENERS</i>
     * which will send events to both {@link IPickEvents} and {@link ITouchEvents} listeners
     * but will not send them to the hit objects.
     * </p>
     * One or more of these options may be combined to change the event behavior:
     * <table>
     *     <tr><td>SEND_PICK_EVENTS</td>
     *     Send events using the {@link IPickEvents} interface.
     *     </tr>
     *     <tr><td>SEND_TOUCH_EVENTS</td>
     *     Send events using the {@link ITouchEvents} interface.
     *     </tr>
     *     <tr><td>SEND_TO_LISTENERS</td>
     *     Send events to the pick event listeners attached to the {@link SXREventReceiver}
     *     of this picker.
     *     </tr>
     *     <tr><td>SEND_TO_HIT_OBJECT</td>
     *     Send events to the event listeners attached to the object that was hit.
     *     </tr>
     *     <tr><td>SEND_TO_SCENE</td>
     *     Send events to the event listeners attached to the scene associated
     *     with this picker.
     *     </tr>
     * </table>
     * @see #getEventOptions()
     */
    public void setEventOptions(EnumSet<EventOptions> options)
    {
        mEventOptions = options;
    }

    /**
     * Get the event options which control what events are
     * sent and to which objects.
     * @return picker event options
     * @see #setEventOptions(EnumSet)
     */
    public EnumSet<EventOptions> getEventOptions()
    {
        return mEventOptions;
    }

    /**
     * Get the scene containing the objects to pick from.
     * @return {@link SXRScene} to pick against
     * @see #setScene(SXRScene)
     */
    public SXRScene getScene() { return mScene; }

    /**
     * Set the scene to pick against.
     * <p>
     * The picker will only pick scene objects that are in
     * this scene. You can change the scene at any time but
     * this may give confusing event streams if done
     * while the application is in the middle of picking.
     * @param scene new scene to pick against, may not be null.
     * @see #getScene()
     */
    public void setScene(SXRScene scene)
    {
        mScene = scene;
    }

    /**
     * Determine whether the pick list has changed since last frame.
     * The value returned by this function is only valid
     * within pick event listeners.
     * @returns true if pick list changed, else false
     */
    public boolean hasPickListChanged() { return mPickListChanged; }

    /**
     * Get the current ray to use for picking.
     * <p/>
     * If the picker is attached to a scene object,
     * this ray is derived from the scene object's transform.
     * The origin of the ray is the translation component
     * of the total model matrix and the ray direction
     * is the forward look vector.
     * <p/>
     * If not attached to a scene object, the origin of the
     * ray is the position of the viewer and its direction
     * is where the viewer is looking.
     * <p>
     * You can get the pick ray in world coordinates instead of
     * with respect to the camera by calling {@link #getWorldPickRay(Vector3f, Vector3f)}.
     * @return pick ray in local or camera coordinates
     */
    public final float[] getPickRay()
    {
        synchronized (this)
        {
            mPickRay[0] = mRayOrigin.x;
            mPickRay[1] = mRayOrigin.y;
            mPickRay[2] = mRayOrigin.z;
            mPickRay[3] = mRayDirection.x;
            mPickRay[4] = mRayDirection.y;
            mPickRay[5] = mRayDirection.z;
        }
        return mPickRay;
    }

    /**
     * Gets the pick ray in world coordinates.
     * <p>
     * World coordinates are defined as the coordinate system at the
     * root of the scene graph before any camera viewing transformation
     * is applied.
     * <p>
     * You can get the pick ray relative to the scene object that
     * owns the picker (or the camera if no owner) by calling
     * {@link #getPickRay()}
     * @param origin    world coordinate origin of the pick ray
     * @param direction world coordinate direction of the pick ray
     * @see #getPickRay()
     * @see #setPickRay(float, float, float, float, float, float)
     */
    public final void getWorldPickRay(Vector3f origin, Vector3f direction)
    {
        SXRNode owner = getOwnerObject();

        if (owner == null)              // should never come here, picker always
        {                               // owned by SXRGearCursorController pivot
            owner = mScene.getMainCameraRig().getHeadTransformObject();
        }
        Matrix4f mtx = owner.getTransform().getModelMatrix4f();
        origin.set(mRayOrigin);
        direction.set(mRayDirection);
        origin.mulPosition(mtx);        // get ray in world coordinates
        direction.mulDirection(mtx);
        direction.normalize();
    }

    /**
     * Gets the current pick list.
     * <p/>
     * Each collision with an object is described as a
     * SXRPickedObject which contains the scene object
     * and collider hit, the distance from the camera
     * and the hit position in the coordinate system
     * of the collision geometry. The objects in the pick
     * list are sorted based on increasing distance
     * from the origin of the pick ray.
     * @return SXRPickedObject array with objects picked or null if nothing picked.
     * @see #doPick()
     * @see IPickEvents
     * @see #setPickRay(float, float, float, float, float, float)
     */
    public final SXRPickedObject[] getPicked()
    {
        return mPicked;
    }

    /**
     * Sets the origin and direction of the pick ray.
     *
     * @param ox    X coordinate of origin.
     * @param oy    Y coordinate of origin.
     * @param oz    Z coordinate of origin.
     * @param dx    X coordinate of ray direction.
     * @param dy    Y coordinate of ray direction.
     * @param dz    Z coordinate of ray direction.
     *
     * The coordinate system of the ray depends on the whether the
     * picker is attached to a scene object or not. When attached
     * to a scene object, the ray is in the coordinate system of
     * that object where (0, 0, 0) is the center of the scene object
     * and (0, 0, 1) is it's positive Z axis. If not attached to an
     * object, the ray is in the coordinate system of the scene's
     * main camera with (0, 0, 0) at the viewer and (0, 0, -1)
     * where the viewer is looking.
     * @see #doPick()
     * @see #getPickRay()
     * @see #getWorldPickRay(Vector3f, Vector3f)
     */
    public void setPickRay(float ox, float oy, float oz, float dx, float dy, float dz)
    {
        synchronized (this)
        {
            mRayOrigin.x = ox;
            mRayOrigin.y = oy;
            mRayOrigin.z = oz;
            mRayDirection.x = dx;
            mRayDirection.y = dy;
            mRayDirection.z = dz;
        }
    }

    /**
     * Get the cursor controller that drives this picker.
     * <p>
     * Every cursor controller has it's own picker that
     * generates pick and touch events for that controller.
     * You can also instantiate a picker independently of
     * any controller.
     * @return controller driving this picker or null if none.
     * @see SXRCursorController#getPicker()
     * @see SXRPicker(SXRCursorController, boolean)
     */
    public SXRCursorController getController() { return mController; }

    /**
     * Get the event receiver for this picker.
     * <p>
     * You can add listeners to this object for either
     * pick or touch events. To add a listener, call
     * {@code @getEventReceiver().addListener(IEvents)} with
     * the {@link IPickEvents} or {@ITouchEvents} interface.
     * @return {@link SXREventReceiver} that dispatches pick events
     * @see IPickEvents
     * @see ITouchEvents
     * @see #setEventOptions(EnumSet)
     */
    public final SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Query if picker is picking closest object or all objects
     * intersected by the pick ray.
     * @return true if picking closest object,
     *          false if picking all intersected objects.
     */
    public boolean getPickClosest() { return mPickClosest; }

    /**
     * Enable or disable picking of the closest object.
     * By default, the picker will return a list containing
     * the object closest to the origin of the pick ray.
     * You can disable this option and the picker will return
     * a list of objects sorted by distance from the origin.
     * @param flag true to pick closest object,
     *            false to pick all objects intersecting the pick ray.
     * @see #getPickClosest()
     * @see #getPicked()
     */
    public void setPickClosest(boolean flag)
    {
        mPickClosest = flag;
    }

    /**
     * Called every frame if the picker is enabled
     * to generate pick events.
     * @param frameTime starting time of the current frame
     */
    public void onDrawFrame(float frameTime)
    {
        if (isEnabled() && (mScene != null) && mPickEventLock.tryLock())
        {
            // Don't call if we are in the middle of processing another pick
            try
            {
                doPick();
            }
            finally
            {
                mPickEventLock.unlock();
            }
        }
    }

    /**
     * Scans the scene graph to collect picked items
     * and generates appropriate pick events.
     * This function is called automatically by
     * the picker every frame.
     * @see IPickEvents
     * @see ITouchEvents
     * @see #pickObjects(SXRScene, float, float, float, float, float, float)
     */
    protected void doPick()
    {
        SXRNode owner = getOwnerObject();
        SXRTransform trans = (owner != null) ? owner.getTransform() : null;
        SXRPickedObject[] picked;

        if (mPickClosest)
        {
            SXRPickedObject closest = pickClosest(mScene, trans,
                mRayOrigin.x, mRayOrigin.y, mRayOrigin.z,
                mRayDirection.x, mRayDirection.y, mRayDirection.z);
            if (closest != null)
            {
                picked = new SXRPickedObject[] { closest };
            }
            else
            {
                picked = new SXRPickedObject[0];
            }
        }
        else
        {
            picked = pickObjects(mScene, trans,
                    mRayOrigin.x, mRayOrigin.y, mRayOrigin.z,
                    mRayDirection.x, mRayDirection.y, mRayDirection.z);
        }
        generatePickEvents(picked);
        mMotionEvent = null;
    }

    /**
     * Scans the scene graph to collect picked items
     * and generates appropriate pick and touch events.
     * This function is called by the cursor controller
     * internally but can also be used to funnel a
     * stream of Android motion events into the picker.
     * @see #pickObjects(SXRScene, float, float, float, float, float, float)
     * @param touched    true if the "touched" button is pressed.
     *                   Which button indicates touch is controller dependent.
     * @param event      Android MotionEvent which caused the pick
     * @see IPickEvents
     * @see ITouchEvents
     */
    public void processPick(boolean touched, MotionEvent event)
    {
        mPickEventLock.lock();
        mTouched = touched;
        mMotionEvent = event;
        doPick();
        mPickEventLock.unlock();
    }

    protected void generatePickEvents(SXRPickedObject[] picked)
    {
    /*
     * Send "onExit" events for colliders that were picked but
     * are not picked anymore.
     */
        if (mPicked != null)
        {
            for (SXRPickedObject collision : mPicked)
            {
                if (collision == null)
                {
                    continue;
                }
                SXRCollider collider = collision.hitCollider;
                SXRPickedObject temp = findCollider(picked, collider);
                if (temp == null)
                {
                    collision.touched = mTouched;
                    collision.motionEvent = mMotionEvent;
                    mPickListChanged = true;
                    propagateOnExit(collider.getOwnerObject(), collision);
                }
            }
        }
        // get the count of non null picked objects
        int pickedCount = 0;

    /*
     * Send "onEnter" events for colliders that were picked for the first time.
     * Send "onTouchStart" events for colliders that were touched for the first time.
     * Send "onTouchEnd" events for colliders that are no longer touched.
     * Send "onInside" events for colliders that were already picked.
     */
        for (SXRPickedObject collision : picked)
        {
            if (collision == null)
            {
                continue;
            }
            pickedCount++;
            SXRCollider collider = collision.hitCollider;
            SXRPickedObject prevHit = findCollider(mPicked, collider);

            collision.picker = this;
            collision.touched = mTouched;
            collision.motionEvent = mMotionEvent;
            if (prevHit == null)
            {
                mPickListChanged = true;
                propagateOnEnter(collision);
                if (mTouched)
                {
                    propagateOnTouch(collision);
                }
            }
            else
            {
                propagateOnInside(collision);
                if (prevHit.touched && !mTouched)

                {
                    mPickListChanged = true;
                    propagateOnNoTouch(collision);
                }
                else if (!prevHit.touched && mTouched)
                {
                    mPickListChanged = true;
                    propagateOnTouch(collision);
                }
            }
        }

        if (pickedCount > 0)
        {
            mPicked = picked;
            propagateOnPick(this);
        }
        else
        {
            mPicked = null;
            propagateOnNoPick(this);
            if (mMotionEvent != null)
            {
                propagateOnMotionOutside(mMotionEvent);
            }
        }
        mPickListChanged = false;
    }

    //@todo anything that sets nativePointer to 0 needs this otherwise SXRHybridObject's hashCode
    //method breaks; this should go to SXRBehavior but I rather make this change gradually;  i am
    //very much against mutable nativePointers and using the magic value of 0 btw.
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Propagate onNoPick events to listeners
     * @param picker SXRPicker which generated the event
     */
    protected void propagateOnNoPick(SXRPicker picker)
    {
        if (mEventOptions.contains(EventOptions.SEND_PICK_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                getSXRContext().getEventManager().sendEvent(this, IPickEvents.class, "onNoPick", picker);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                getSXRContext().getEventManager().sendEvent(mScene, IPickEvents.class, "onNoPick", picker);
            }
        }
    }

    /**
     * Propagate onPick events to listeners
     * @param picker SXRPicker which generated the event
     */
    protected void propagateOnPick(SXRPicker picker)
    {
        if (mEventOptions.contains(EventOptions.SEND_PICK_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                getSXRContext().getEventManager().sendEvent(this, IPickEvents.class, "onPick", picker);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                getSXRContext().getEventManager().sendEvent(mScene, IPickEvents.class, "onPick", picker);
            }
        }
    }

    /**
     * Propagate onMotionOutside events to listeners
     * @param MotionEvent Android MotionEvent when nothing is picked
     */
    protected void propagateOnMotionOutside(MotionEvent event)
    {
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                getSXRContext().getEventManager().sendEvent(this, ITouchEvents.class, "onMotionOutside", this, event);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                getSXRContext().getEventManager().sendEvent(mScene, ITouchEvents.class, "onMotionOutside", this, event);
            }
        }
    }

    /**
     * Propagate onEnter events to listeners
     * @param hit collision object
     */
    protected void propagateOnEnter(SXRPickedObject hit)
    {
        SXRNode hitObject = hit.getHitObject();
        SXREventManager eventManager = getSXRContext().getEventManager();
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, ITouchEvents.class, "onEnter", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, ITouchEvents.class, "onEnter", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, ITouchEvents.class, "onEnter", hitObject, hit);
            }
        }
        if (mEventOptions.contains(EventOptions.SEND_PICK_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, IPickEvents.class, "onEnter", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, IPickEvents.class, "onEnter", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, IPickEvents.class, "onEnter", hitObject, hit);
            }
        }
    }

    /**
     * Propagate onTouchStart events to listeners
     * @param hit collision object
     */
    protected void propagateOnTouch(SXRPickedObject hit)
    {
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            SXREventManager eventManager = getSXRContext().getEventManager();
            SXRNode hitObject = hit.getHitObject();
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, ITouchEvents.class, "onTouchStart", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, ITouchEvents.class, "onTouchStart", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, ITouchEvents.class, "onTouchStart", hitObject, hit);
            }
        }
    }

    /**
     * Propagate onTouchEnd events to listeners
     * @param hit collision object
     */
    protected void propagateOnNoTouch(SXRPickedObject hit)
    {
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            SXREventManager eventManager = getSXRContext().getEventManager();
            SXRNode hitObject = hit.getHitObject();
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, ITouchEvents.class, "onTouchEnd", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, ITouchEvents.class, "onTouchEnd", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, ITouchEvents.class, "onTouchEnd", hitObject, hit);
            }
        }
    }

    /**
     * Propagate onInside events to listeners
     * @param hit collision object
     */
    protected void propagateOnInside(SXRPickedObject hit)
    {
        SXRNode hitObject = hit.getHitObject();
        SXREventManager eventManager = getSXRContext().getEventManager();
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, ITouchEvents.class, "onInside", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, ITouchEvents.class, "onInside", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, ITouchEvents.class, "onInside", hitObject, hit);
            }
        }
        if (mEventOptions.contains(EventOptions.SEND_PICK_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, IPickEvents.class, "onInside", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, IPickEvents.class, "onInside", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, IPickEvents.class, "onInside", hitObject, hit);
            }
        }
    }

    /**
     * Propagate onExit events to listeners
     * @param hitObject scene object
     */
    protected void propagateOnExit(SXRNode hitObject, SXRPickedObject hit)
    {
        SXREventManager eventManager = getSXRContext().getEventManager();
        if (mEventOptions.contains(EventOptions.SEND_TOUCH_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, ITouchEvents.class, "onExit", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, ITouchEvents.class, "onExit", hitObject, hit);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, ITouchEvents.class, "onExit", hitObject, hit);
            }
        }
        if (mEventOptions.contains(EventOptions.SEND_PICK_EVENTS))
        {
            if (mEventOptions.contains(EventOptions.SEND_TO_LISTENERS))
            {
                eventManager.sendEvent(this, IPickEvents.class, "onExit", hitObject);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_HIT_OBJECT))
            {
                eventManager.sendEvent(hitObject, IPickEvents.class, "onExit", hitObject);
            }
            if (mEventOptions.contains(EventOptions.SEND_TO_SCENE) && (mScene != null))
            {
                eventManager.sendEvent(mScene, IPickEvents.class, "onExit", hitObject);
            }
        }
    }

    /**
     * Find the collision against a specific collider in a list of collisions.
     * @param pickList collision list
     * @param findme   collider to find
     * @return collision with the specified collider, null if not found
     */
    protected SXRPickedObject findCollider(SXRPickedObject[] pickList, SXRCollider findme)
    {
        if (pickList == null)
        {
            return null;
        }
        for (SXRPickedObject hit : pickList)
        {
            if ((hit != null) && (hit.hitCollider == findme))
            {
                return hit;
            }
        }
        return null;
    }

    /**
     * Tests the {@link SXRNode} against the ray information passed to the function.
     *
     * @param sceneObject
     *            The {@link SXRNode} to be tested.
     *
     * @param ox
     *            The x coordinate of the ray origin (in world coords).
     *
     * @param oy
     *            The y coordinate of the ray origin (in world coords).
     *
     * @param oz
     *            The z coordinate of the ray origin (in world coords).
     *
     * @param dx
     *            The x vector of the ray direction (in world coords).
     *
     * @param dy
     *            The y vector of the ray direction (in world coords).
     *
     * @param dz
     *            The z vector of the ray direction (in world coords).
     *
     * @return  a {@link SXRPicker.SXRPickedObject} containing the picking information
     *
     */
    public static final SXRPickedObject pickNode(SXRNode sceneObject, float ox, float oy, float oz, float dx,
                                                        float dy, float dz) {
        return NativePicker.pickNode(sceneObject.getNative(), ox, oy, oz, dx, dy, dz);
    }

    /**
     * Tests the {@link SXRNode} against the ray information passed to the function.
     *
     * @param sceneObject
     *            The {@link SXRNode} to be tested.
     *
     * @return  a {@link SXRPicker.SXRPickedObject} containing the picking information
     *
     */
    public static final SXRPickedObject pickNode(SXRNode sceneObject) {
        SXRCameraRig cam = sceneObject.getSXRContext().getMainScene().getMainCameraRig();
        SXRTransform t = cam.getHeadTransform();
        float[] lookat = cam.getLookAt();
        return NativePicker.pickNode(sceneObject.getNative(), t.getPositionX(), t.getPositionY(), t.getPositionZ(),
                lookat[0], lookat[1], lookat[2]);
    }

    /**
     *
     * Tests the {@link SXRNode} against the specified ray.
     *
     * The ray is defined by its origin {@code [ox, oy, oz]} and its direction
     * {@code [dx, dy, dz]}.
     *
     * <p>
     * The ray origin may be [0, 0, 0] and the direction components should be
     * normalized from -1 to 1: Note that the y direction runs from -1 at the
     * bottom to 1 at the top.
     *
     * @param sceneObject
     *            The {@link SXRNode} to be tested.
     *
     * @param ox
     *            The x coordinate of the ray origin.
     *
     * @param oy
     *            The y coordinate of the ray origin.
     *
     * @param oz
     *            The z coordinate of the ray origin.
     *
     * @param dx
     *            The x vector of the ray direction.
     *
     * @param dy
     *            The y vector of the ray direction.
     *
     * @param dz
     *            The z vector of the ray direction.
     *
     * @param readbackBuffer The readback buffer is a small optimization on this call. Instead of
     *                       creating a new float array every time this call is made, the
     *                       readback buffer allows the caller to forward a dedicated array that
     *                       can be populated by the native layer every time there is a
     *                       successful hit. Make use of the return value to know if the contents
     *                       of the buffer is valid or not. For multiple calls to this method a
     *                       {@link ByteBuffer} can be created once and used multiple times.
     *
     * @return <code>true</code> on a successful hit, <code>false</code> otherwise.
     */
    static final boolean pickNodeAgainstBoundingBox(
            SXRNode sceneObject, float ox, float oy, float oz, float dx,
            float dy, float dz, ByteBuffer readbackBuffer) {
        sFindObjectsLock.lock();
        try {
            return NativePicker.pickNodeAgainstBoundingBox(
                    sceneObject.getNative(), ox, oy, oz, dx, dy, dz, readbackBuffer);
        } finally {
            sFindObjectsLock.unlock();
        }
    }

    /**
     * Casts a ray into the scene graph, and returns the objects it intersects.
     *
     * The ray is defined by its origin {@code [ox, oy, oz]} and its direction
     * {@code [dx, dy, dz]}.
     *
     * <p>
     * The ray origin may be [0, 0, 0] and the direction components should be
     * normalized from -1 to 1: Note that the y direction runs from -1 at the
     * bottom to 1 at the top. To construct a picking ray originating at the
     * user's head and pointing into the scene along the camera lookat vector,
     * pass in 0, 0, 0 for the origin and 0, 0, -1 for the direction.
     *
     * <p>
     * This method is thread safe because it guarantees that only
     * one thread at a time is doing a ray cast into a particular scene graph,
     * and it extracts the hit data during within its synchronized block. You
     * can then examine the return list without worrying about another thread
     * corrupting your hit data.
     * <p>
     * Depending on the type of collider, that the hit location may not be exactly
     * where the ray would intersect the scene object itself. Rather, it is
     * where the ray intersects the collision geometry associated with the collider.
     *
     * @param scene
     *            The {@link SXRScene} with all the objects to be tested.
     *
     * @param ox
     *            The x coordinate of the ray origin.
     *
     * @param oy
     *            The y coordinate of the ray origin.
     *
     * @param oz
     *            The z coordinate of the ray origin.
     *
     * @param dx
     *            The x vector of the ray direction.
     *
     * @param dy
     *            The y vector of the ray direction.
     *
     * @param dz
     *            The z vector of the ray direction.
     * @return A list of {@link SXRPickedObject}, sorted by distance from the
     *         camera rig. Each {@link SXRPickedObject} contains the scene object
     *         which owns the {@link SXRCollider} along with the hit
     *         location and distance from the camera.
     *
     * @since 1.6.6
     */
    public static final SXRPickedObject[] pickObjects(SXRScene scene, float ox, float oy, float oz, float dx,
                                                      float dy, float dz) {
        sFindObjectsLock.lock();
        try {
            final SXRPickedObject[] result = NativePicker.pickObjects(scene.getNative(), 0L, ox, oy, oz, dx, dy, dz);
            return result;
        } finally {
            sFindObjectsLock.unlock();
        }
    }

    /**
     * Casts a ray into the scene graph, and returns the closest object
     * to origin of the pick ray.
     * <p/>
     * The ray is defined by its origin {@code [ox, oy, oz]} and its direction
     * {@code [dx, dy, dz]}. The ray is in the coordinate system of the
     * input transform, allowing it to be with respect to a scene object.
     *
     * <p>
     * The ray origin may be [0, 0, 0] and the direction components should be
     * normalized from -1 to 1: Note that the y direction runs from -1 at the
     * bottom to 1 at the top. To construct a picking ray originating at the
     * center of a scene object and pointing where that scene object looks,
     * attach the SXRPicker to the scene object and  pass (0, 0, 0) as
     * the ray origin and (0, 0, -1) for the direction.
     *
     * <p>
     * This method is thread safe because it guarantees that only
     * one thread at a time is doing a ray cast into a particular scene graph,
     * and it extracts the hit data during within its synchronized block. You
     * can then examine the return list without worrying about another thread
     * corrupting your hit data.
     * <p/>
     * Depending on the type of collider, that the hit location may not be exactly
     * where the ray would intersect the scene object itself. Rather, it is
     * where the ray intersects the collision geometry associated with the collider.
     *
     * @param scene
     *            The {@link SXRScene} with all the objects to be tested.
     * @param trans
     *            The {@link SXRTransform} establishing the coordinate system of the ray.
     * @param ox
     *            The x coordinate of the ray origin.
     *
     * @param oy
     *            The y coordinate of the ray origin.
     *
     * @param oz
     *            The z coordinate of the ray origin.
     *
     * @param dx
     *            The x vector of the ray direction.
     *
     * @param dy
     *            The y vector of the ray direction.
     *
     * @param dz
     *            The z vector of the ray direction.
     * @return The {@link SXRPickedObject} closest to the ray origin or null if nothing picked.
     *         Each {@link SXRPickedObject} contains the scene object
     *         which owns the {@link SXRCollider} along with the hit
     *         location and distance.
     *
     * @since 1.6.6
     */
    public static final SXRPickedObject pickClosest(SXRScene scene, SXRTransform trans,
                                                    float ox, float oy, float oz,
                                                    float dx, float dy, float dz)
    {
        sFindObjectsLock.lock();
        try {
            long nativeTrans = (trans != null) ? trans.getNative() : 0L;
            final SXRPickedObject result =
                    NativePicker.pickClosest(scene.getNative(),
                            nativeTrans,
                            ox, oy, oz, dx, dy, dz);
            return result;
        } finally {
            sFindObjectsLock.unlock();
        }
    }

    /**
     * Casts a ray into the scene graph, and returns the objects it intersects.
     * <p/>
     * The ray is defined by its origin {@code [ox, oy, oz]} and its direction
     * {@code [dx, dy, dz]}. The ray is in the coordinate system of the
     * input transform, allowing it to be with respect to a scene object.
     *
     * <p>
     * The ray origin may be [0, 0, 0] and the direction components should be
     * normalized from -1 to 1: Note that the y direction runs from -1 at the
     * bottom to 1 at the top. To construct a picking ray originating at the
     * center of a scene object and pointing where that scene object looks,
     * attach the SXRPicker to the scene object and  pass (0, 0, 0) as
     * the ray origin and (0, 0, -1) for the direction.
     *
     * <p>
     * This method is thread safe because it guarantees that only
     * one thread at a time is doing a ray cast into a particular scene graph,
     * and it extracts the hit data during within its synchronized block. You
     * can then examine the return list without worrying about another thread
     * corrupting your hit data.
     * <p/>
     * Depending on the type of collider, that the hit location may not be exactly
     * where the ray would intersect the scene object itself. Rather, it is
     * where the ray intersects the collision geometry associated with the collider.
     *
     * @param scene
     *            The {@link SXRScene} with all the objects to be tested.
     * @param trans
     *            The {@link SXRTransform} establishing the coordinate system of the ray.
     * @param ox
     *            The x coordinate of the ray origin.
     *
     * @param oy
     *            The y coordinate of the ray origin.
     *
     * @param oz
     *            The z coordinate of the ray origin.
     *
     * @param dx
     *            The x vector of the ray direction.
     *
     * @param dy
     *            The y vector of the ray direction.
     *
     * @param dz
     *            The z vector of the ray direction.
     * @return A list of {@link SXRPickedObject}, sorted by distance from the
     *         pick ray origin. Each {@link SXRPickedObject} contains the scene object
     *         which owns the {@link SXRCollider} along with the hit
     *         location and distance.
     *
     * @since 1.6.6
     */
    public static final SXRPickedObject[] pickObjects(SXRScene scene, SXRTransform trans, float ox, float oy, float oz, float dx,
                                                      float dy, float dz) {
        sFindObjectsLock.lock();
        try {
            long nativeTrans = (trans != null) ? trans.getNative() : 0L;
            final SXRPickedObject[] result = NativePicker.pickObjects(scene.getNative(), nativeTrans, ox, oy, oz, dx, dy, dz);
            return result;
        } finally {
            sFindObjectsLock.unlock();
        }
    }

    /**
     * Internal utility to help JNI add hit objects to the pick list.
     */
    static SXRPickedObject makeHit(long colliderPointer, float distance, float hitx, float hity, float hitz)
    {
        SXRCollider collider = SXRCollider.lookup(colliderPointer);
        if (collider == null)
        {
            Log.d(TAG, "makeHit: cannot find collider for %x", colliderPointer);
            return null;
        }
        return new SXRPicker.SXRPickedObject(collider, new float[] { hitx, hity, hitz }, distance);
    }


    /**
     * Internal utility to help JNI add hit objects to the pick list. Specifically for MeshColliders with picking
     * for UV, Barycentric, and normal coordinates enabled
     */
    static SXRPickedObject makeHitMesh(long colliderPointer, float distance, float hitx, float hity, float hitz,
                                   int faceIndex, float barycentricx, float barycentricy, float barycentricz,
                                   float texu, float texv,  float normalx, float normaly, float normalz)
    {
        SXRCollider collider = SXRCollider.lookup(colliderPointer);
        if (collider == null)
        {
            Log.d(TAG, "makeHit: cannot find collider for %x", colliderPointer);
            return null;
        }
        return new SXRPicker.SXRPickedObject(collider, new float[] { hitx, hity, hitz }, distance, faceIndex,
                new float[] {barycentricx, barycentricy, barycentricz},
                new float[]{ texu, texv },
                new float[]{normalx, normaly, normalz});
    }

    /**
     * The result of a pick request which hits an object.
     * <p/>
     * When a pick request is performed, each collision is
     * described as a SXRPickedObject.
     *
     * @since 1.6.6
     * @see SXRPicker#pickObjects(SXRScene, float, float, float, float, float, float)
     */
    public static final class SXRPickedObject {
        public final SXRNode hitObject;
        public final SXRCollider hitCollider;
        public SXRPicker picker;
        public final float[] hitLocation;
        public final float hitDistance;
        public boolean touched;
        public MotionEvent motionEvent;
        public int collidableIndex;
        public final int faceIndex;
        public final float[] barycentricCoords;
        public final float[] textureCoords;
        public final float[] normalCoords;

        /**
         * Creates a new instance of {@link SXRPickedObject}.
         *
         * @param hitCollider
         *            The {@link SXRCollider} that the ray intersected.
         * @param hitDistance
         *            The distance from the origin if the ray.
         * @param hitLocation
         *            The hit location, as an [x, y, z] array.
         * @param faceIndex
         *            The index of the face intersected if a {@link SXRMeshCollider} was attached
         *            to the {@link SXRNode}, -1 otherwise
         * @param barycentricCoords
         *            The barycentric coordinates of the hit location on the intersected face
         *            if a {@link SXRMeshCollider} was attached to the {@link SXRNode},
         *            [ -1.0f, -1.0f, -1.0f ] otherwise.
         * @param textureCoords
         *            The texture coordinates of the hit location on the intersected face
         *            if a {@link SXRMeshCollider} was attached to the {@link SXRNode},
         *            [ -1.0f, -1.0f ] otherwise.
         *
         * @see SXRPicker#pickObjects(SXRScene, float, float, float, float, float, float)
         * @see SXRCollider
         */
        public SXRPickedObject(SXRCollider hitCollider, float[] hitLocation, float hitDistance, int faceIndex,
                               float[] barycentricCoords, float[] textureCoords, float[] normalCoords) {
            hitObject = hitCollider.getOwnerObject();
            this.hitDistance = hitDistance;
            this.hitCollider = hitCollider;
            this.hitLocation = hitLocation;
            this.faceIndex = faceIndex;
            this.barycentricCoords = barycentricCoords;
            this.textureCoords = textureCoords;
            this.normalCoords = normalCoords;
            this.touched = false;
            this.collidableIndex = -1;
            this.motionEvent = null;
        }

        public SXRPickedObject(SXRCollider hitCollider, float[] hitLocation, float hitDistance) {
            hitObject = hitCollider.getOwnerObject();
            this.hitDistance = hitDistance;
            this.hitCollider = hitCollider;
            this.hitLocation = hitLocation;
            this.faceIndex = -1;
            this.barycentricCoords = null;
            this.textureCoords = null;
            this.normalCoords = null;
            this.touched = false;
            this.collidableIndex = -1;
            this.motionEvent = null;
        }

        public SXRPickedObject(SXRNode hitObject, float[] hitLocation) {
            this.hitObject = hitObject;
            this.hitLocation = hitLocation;
            this.hitDistance = -1;
            this.hitCollider = null;
            this.faceIndex = -1;
            this.barycentricCoords = null;
            this.textureCoords = null;
            this.normalCoords = null;
            this.touched = false;
            this.collidableIndex = -1;
            this.motionEvent = null;
        }

        /**
         * The {@link SXRNode} that the ray intersected.
         *
         * This is the owner of the collider hit.
         *
         * @return scene object hit
         * @see SXRComponent#getOwnerObject()
         */
        public SXRNode getHitObject() {
            return hitObject;
        }

        /**
         * The {@link SXRCollider} that the ray intersected.
         *
         * @return collider hit
         */
        public SXRCollider getHitCollider() {
            return hitCollider;
        }

        /**
         * The {@link SXRPicker} this collision came from.
         * This will be null if the collision was generated
         * from {@link #pickObjects(SXRScene, float, float, float, float, float, float)}
         */
        public SXRPicker getPicker() { return picker; }

        /**
         * The hit location, as an [x, y, z] array.
         *
         * @return A copy of the hit result
         */
        public float[] getHitLocation() {
            return Arrays.copyOf(hitLocation, hitLocation.length);
        }

        /**
         * The distance from the origin of the pick ray
         */
        public float getHitDistance() {
            return hitDistance;
        }

        /**
         * The barycentric coordinates of the hit location on the collided face
         * This will return -1 if the faceIndex isn't calculated
         */
        public int getFaceIndex() {
            return faceIndex;
        }

        /**
         * The barycentric coordinates of the hit location on the collided face
         * Returns null if the coordinates haven't been calculated.
         */
        public float[] getBarycentricCoords() {
            if(barycentricCoords != null)
                return Arrays.copyOf(barycentricCoords, barycentricCoords.length);
            else
                return null;
        }

        /**
         * The UV texture coordinates of the hit location on the mesh
         * Returns null if the coordinates haven't been calculated.
         */
        public float[] getTextureCoords() {
            if(textureCoords != null)
                return Arrays.copyOf(textureCoords, textureCoords.length);
            else
                return null;
        }

        /**
         * The normalized surface normal of the hit location on the mesh (in local coordinates).
         * Returns null if the coordinates haven't been calculated.
         */
        public float[] getNormalCoords() {
            if(normalCoords != null)
                return Arrays.copyOf(normalCoords, normalCoords.length);
            else
                return null;
        }

        /**
         * Determines whether the collider is touched.
         * <p>
         * If the "touch" button of the cursor controller associated
         * with the picker is pressed, the collider is considered "touched".
         * Which button indicates touch is controller dependent.
         * @returns true if collider is touched, else false.
         */
        public boolean isTouched() { return touched; }

    }

    static final ReentrantLock sFindObjectsLock = new ReentrantLock();
}

final class NativePicker {
    static native SXRPicker.SXRPickedObject pickClosest(long scene, long transform,
                                                        float ox, float oy, float oz,
                                                        float dx, float dy, float dz);

    static native SXRPicker.SXRPickedObject[] pickObjects(long scene, long transform, float ox, float oy, float oz,
                                                          float dx, float dy, float dz);

    static native SXRPicker.SXRPickedObject[] pickBounds(long scene, List<SXRNode> collidables);

    static native SXRPicker.SXRPickedObject pickNode(long sceneObject, float ox, float oy, float oz,
                                                            float dx, float dy, float dz);

    static native SXRPicker.SXRPickedObject[] pickVisible(long scene);

    static native boolean pickNodeAgainstBoundingBox(long sceneObject,
                                                            float ox, float oy, float oz, float dx, float dy, float dz, ByteBuffer readbackBuffer);
}
