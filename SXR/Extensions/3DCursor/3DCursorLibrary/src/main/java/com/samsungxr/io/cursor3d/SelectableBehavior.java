package com.samsungxr.io.cursor3d;

import android.util.SparseArray;

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSphereCollider;
import com.samsungxr.SXRSwitch;
import com.samsungxr.SXRTransform;
import com.samsungxr.IEvents;
import com.samsungxr.ITouchEvents;
import org.joml.Vector3f;

import java.util.HashMap;

/**
 * This class defines a {@link SXRBehavior} that can be attached to a {@link SXRNode}, it
 * handles all the {@link ITouchEvents} on the associated {@link SXRNode} and
 * maintains the correct {@link ObjectState} based on the {@link ICursorEvents} received. This
 * class can associate a child {@link SXRNode} with each {@link ObjectState} and is
 * responsible for making the correct {@link SXRNode} visible according to the
 * {@link ObjectState}.
 */
public class SelectableBehavior extends SXRBehavior {
    private static final String TAG = SelectableBehavior.class.getSimpleName();
    static private long TYPE_SELECTABLE = newComponentType(SelectableBehavior.class);
    private static final String DEFAULT_ASSET_NEEDED = "Asset for Default state should be " +
            "specified";
    private SXRSwitch gvrSwitch;
    private ObjectState state = ObjectState.DEFAULT;
    private HashMap<ObjectState, Integer> stateIndexMap;
    private boolean previousActive;
    private boolean previousOver;
    private SparseArray<ObjectState> states;
    protected ObjectState currentState = ObjectState.DEFAULT;
    private StateChangedListener stateChangedListener;
    final protected CursorManager cursorManager;

    // required for scaling
    private Cursor clickCursor1 = null;
    private Cursor clickCursor2 = null;
    private float prevDistance = 0;
    private Vector3f vecDistance = new Vector3f();


    /**
     * Set a {@link StateChangedListener} using
     * {@link SelectableBehavior#setStateChangedListener(StateChangedListener)} to get updates about
     * the change of the state of the {@link SelectableBehavior}
     */
    public interface StateChangedListener {
        /**
         * is called whenever the state of the {@link SelectableBehavior} changes.
         *
         * @param behavior   the instance of {@link SelectableBehavior} associated with the
         *                   SXRNode
         * @param previous   the previous state
         * @param current    current state to be set.
         * @param cursor     the instance of {@link Cursor} that caused the state change
         * @param hit        the collision object which cause the change
         */
        void onStateChanged(SelectableBehavior behavior, ObjectState previous, ObjectState current,
                            Cursor cursor, SXRPicker.SXRPickedObject hit);
    }

    /**
     * Define all possible states the {@link SelectableBehavior} can be in
     */
    public enum ObjectState {
        /**
         * The state in which the {@link SelectableBehavior} is initially. This represents the
         * normal
         * appearance of the associated {@link SXRNode}
         */
        DEFAULT,
        /**
         * This state means that a {@link Cursor} is behind the associated {@link SXRNode}.
         * Recommended appearance of the associated {@link SXRNode} in this state is a wire
         * frame or a transparent texture, to allow the {@link Cursor} to be visible to the user.
         */
        BEHIND,
        /**
         * This state means that a
         * {@link Cursor} is intersecting with the associated {@link SXRNode}
         * It is recommended that the appearance of the associated {@link SXRNode} in this
         * state changes from the {@link ObjectState#DEFAULT} to make the user aware of a collision.
         */
        COLLIDING,
        /**
         * This state means that a {@link Cursor} is intersecting with the associated
         * {@link SXRNode}. It is recommended
         * that the appearance of the associated {@link SXRNode} in this state changes
         * from the {@link ObjectState#DEFAULT} to make the user aware of the clicked state.
         */
        CLICKED,
        /**
         * This state means that two {@link Cursor}s have selected this {@link SXRNode} it
         * will be scaled based on the relative distance between these two {@link Cursor}s
         */
        SCALE
    }

    /**
     * Creates a {@link SelectableBehavior} to be attached to any {@link SXRNode}. The
     * instance thus created will not change the appearance of the linked {@link SXRNode}
     * according to the {@link ObjectState}.
     *
     * @param cursorManager
     */
    public SelectableBehavior(CursorManager cursorManager) {
        this(cursorManager, false);
    }

    /**
     * Creates a {@link SelectableBehavior} which is to be attached to a {@link SXRNode}
     * with a specific hierarchy where, the attached {@link SXRNode} has a separate child
     * {@link SXRNode} for each {@link ObjectState}. The order of the child nodes has to
     * follow {@link ObjectState#DEFAULT}, {@link ObjectState#BEHIND}, {@link ObjectState#COLLIDING},
     * and {@link ObjectState#CLICKED} from left to right where {@link ObjectState#DEFAULT}
     * corresponds to the first/left most child. The {@link SelectableBehavior} handles all the
     * {@link ITouchEvents} on the linked {@link SXRNode} and maintains the
     * {@link ObjectState}. It also makes the correct child {@link SXRNode} visible according
     * to the {@link ObjectState}. It is recommended that the different nodes representing different
     * {@link ObjectState} share a common set of vertices if possible. Not having the needed
     * hierarchy will result in an {@link IllegalArgumentException} when calling
     * {@link SXRNode#attachComponent(SXRComponent)}
     *
     * @param cursorManager       the {@link CursorManager} instance
     * @param initializeAllStates flag to indicate whether to initialize all
     *                            possible {@link ObjectState}s. <code>true</code> initialize all
     *                            states in order from left to right:{@link ObjectState#DEFAULT},
     *                            {@link ObjectState#BEHIND}, {@link ObjectState#COLLIDING}, and
     *                            {@link ObjectState#CLICKED}. <code>false</code> initializes only
     *                            the {@link ObjectState#DEFAULT} state. Which does not require the
     *                            attached  {@link SXRNode} to have a hierarchy.
     */
    public SelectableBehavior(CursorManager cursorManager, boolean initializeAllStates) {
        this(cursorManager, initializeAllStates ? new ObjectState[]{ObjectState.DEFAULT,
                ObjectState.BEHIND, ObjectState.COLLIDING, ObjectState.CLICKED} : new ObjectState[]{
                ObjectState.DEFAULT});
    }

    /**
     * Creates a {@link SelectableBehavior} which is to be attached to a {@link SXRNode}
     * with a specific hierarchy where, the {@link SXRNode} to be attached has a root node at
     * the top and a child for each of the states of the {@link SelectableBehavior}.
     * The {@link SelectableBehavior} handles all the {@link ITouchEvents} on the linked
     * {@link SXRNode} and maintains the {@link ObjectState}. It also makes the correct child
     * {@link SXRNode} visible according to the {@link ObjectState}. It is recommended to
     * have a child for each {@link ObjectState} value, however it is possible to have lesser
     * children as long as the mapping is correctly specified in {@param objectStates}. If the
     * {@param objectStates} does not match the {@link SXRNode} hierarchy it will result in
     * an {@link IllegalArgumentException} when calling
     * {@link SXRNode#attachComponent(SXRComponent)}. To save on memory it is suggested that
     * the children of the {@link SXRNode} representing different {@link ObjectState}s share
     * a common set of vertices if possible.
     *
     * @param cursorManager the {@link CursorManager} instance
     * @param objectStates  array of {@link ObjectState}s that maps each child of the attached
     *                      {@link SXRNode} with an {@link ObjectState}. Where the first
     *                      element of the array maps to the left most/first child of the attached
     *                      {@link SXRNode}. This array should contain
     *                      {@link ObjectState#DEFAULT} as one of its elements else it will result
     *                      in an {@link IllegalArgumentException}.
     */
    public SelectableBehavior(CursorManager cursorManager, ObjectState[] objectStates) {
        super(cursorManager.getSXRContext());
        mType = getComponentType();
        this.cursorManager = cursorManager;
        states = new SparseArray<ObjectState>();
        stateIndexMap = new HashMap<ObjectState, Integer>();
        gvrSwitch = new SXRSwitch(cursorManager.getSXRContext());

        boolean defaultState = false;
        for (int i = 0; i < objectStates.length; i++) {
            if (objectStates[i] == ObjectState.DEFAULT) {
                defaultState = true;
            }
            stateIndexMap.put(objectStates[i], i);
        }

        if (!defaultState) {
            throw new IllegalArgumentException(DEFAULT_ASSET_NEEDED);
        }
        setState(ObjectState.DEFAULT, null, null);
    }

    @Override
    public void onAttach(SXRNode sceneObject) {
        super.onAttach(sceneObject);
        if (stateIndexMap.size() > 1 && sceneObject.getChildrenCount() != stateIndexMap.size()) {
            throw new IllegalArgumentException("Num of children in model:" + sceneObject
                    .getChildrenCount() + " does not match the states array:" + stateIndexMap
                    .size());
        }

        if (stateIndexMap.size() > 1) {
            if(!sceneObject.attachComponent(gvrSwitch)) {
                throw new IllegalArgumentException("Cannot attach selectable behavior on a scene" +
                        " object with a SXRSwitch component");
            }
        }
        cursorManager.addSelectableObject(sceneObject);
        sceneObject.getEventReceiver().addListener(clickListener);
        if (sceneObject.getCollider() == null) {
            sceneObject.attachCollider(new SXRSphereCollider(getSXRContext()));
        }
    }

    @Override
    public void onDetach(SXRNode sceneObject) {
        super.onDetach(sceneObject);
        sceneObject.getEventReceiver().removeListener(clickListener);
        sceneObject.detachComponent(SXRSwitch.getComponentType());
        cursorManager.removeSelectableObject(sceneObject);
    }

    /**
     * Gets the current {@link ObjectState} of the {@link SelectableBehavior}
     *
     * @return the {@link ObjectState} associated with this {@link SelectableBehavior}
     */
    public ObjectState getState() {
        return state;
    }

    private boolean isHigherOrEqualStatePresent(ObjectState targetState) {

        for (int i = 0; i < states.size(); i++) {
            if (states.get(i) != null && targetState.ordinal() <= states.get(i).ordinal()) {
                return true;
            }
        }
        return false;
    }

    public void setScale(Cursor cursor, SXRPicker.SXRPickedObject hit) {
        int cursorId = cursor.getId();
        states.remove(cursorId);
        if(!isHigherOrEqualStatePresent(ObjectState.SCALE)) {
            currentState = ObjectState.SCALE;
            setState(currentState, cursor, hit);
        }
        states.put(cursorId, ObjectState.SCALE);
    }

    public void setButtonPress(Cursor cursor, SXRPicker.SXRPickedObject hit) {
        int cursorId = cursor.getId();
        states.remove(cursorId);
        if (!isHigherOrEqualStatePresent(ObjectState.CLICKED)) {
            currentState = ObjectState.CLICKED;
            setState(currentState, cursor, hit);
        }
        states.put(cursorId, ObjectState.CLICKED);
    }

    public void setIntersect(Cursor cursor, SXRPicker.SXRPickedObject hit) {
        int cursorId = cursor.getId();
        states.remove(cursorId);
        if (!isHigherOrEqualStatePresent(ObjectState.CLICKED)) {
            currentState = ObjectState.COLLIDING;
            setState(currentState, cursor, hit);
        }
        states.put(cursorId, ObjectState.COLLIDING);
    }

    public void setWireFrame(Cursor cursor, SXRPicker.SXRPickedObject hit) {
        int cursorId = cursor.getId();
        states.remove(cursorId);
        if (!isHigherOrEqualStatePresent(ObjectState.BEHIND)) {
            currentState = ObjectState.BEHIND;
            setState(currentState, cursor, hit);
        }
        states.put(cursorId, ObjectState.BEHIND);
    }

    private ObjectState getHighestPriorityState() {
        ObjectState highestPriority = ObjectState.DEFAULT;
        for (int i = 0; i < states.size(); i++) {
            ObjectState state = states.get(i);

            if (state != null && state.ordinal() > highestPriority.ordinal()) {
                highestPriority = state;
            }
        }
        return highestPriority;
    }

    public void setDefault(Cursor cursor, SXRPicker.SXRPickedObject hit) {
        int cursorId = cursor.getId();
        states.remove(cursorId);
        ObjectState highestPriority = getHighestPriorityState();
        if (currentState != highestPriority) {
            currentState = highestPriority;
            setState(currentState, cursor, hit);
        }
        states.put(cursorId, ObjectState.DEFAULT);
    }

    private void setState(ObjectState state, Cursor cursor, SXRPicker.SXRPickedObject hit) {
        ObjectState prevState = this.state;
        this.state = state;
        Integer childIndex = stateIndexMap.get(state);
        if (childIndex != null && gvrSwitch != null) {
            gvrSwitch.setSwitchIndex(childIndex);
        }
        if (prevState != this.state && stateChangedListener != null) {
            stateChangedListener.onStateChanged(this, prevState, currentState, cursor, hit);
        }
    }


    protected ICursorEvents clickListener = new ICursorEvents()
    {
        public void onCursorScale(Cursor cursor)
        {
            vecDistance.set(clickCursor1.getPositionX(), clickCursor1.getPositionY(), clickCursor1.getPositionZ());
            float distance = vecDistance.distance(clickCursor2.getPositionX(), clickCursor2.getPositionY(), clickCursor2.getPositionZ());
            if (prevDistance != 0)
            {
                float diff = distance / prevDistance;
                SXRTransform transform = getOwnerObject().getTransform();
                float scaleX = transform.getScaleX() * diff;
                float scaleY = transform.getScaleY() * diff;
                float scaleZ = transform.getScaleZ() * diff;
                transform.setScale(scaleX, scaleY, scaleZ);
            }
            prevDistance = distance;
        }


        public void onTouchStart(Cursor cursor, SXRPicker.SXRPickedObject hit)
        {
            if (clickCursor1 == null)
            {
                clickCursor1 = cursor;
            }
            else if (clickCursor1 != null && clickCursor2 == null)
            {
                clickCursor2 = cursor;
                setScale(clickCursor2, hit);
                setScale(clickCursor1, hit);
            }
        }

        public void onTouchEnd(Cursor cursor, SXRPicker.SXRPickedObject hit)
        {
            if (clickCursor1 == cursor)
            {
                clickCursor1 = null;
                if (clickCursor2 != null)
                {
                    setButtonPress(clickCursor2, hit);
                    prevDistance = 0;
                }
            }
            else if (cursor == clickCursor2)
            {
                clickCursor2 = null;
                if (clickCursor1 != null)
                {
                    setButtonPress(clickCursor1, hit);
                    prevDistance = 0;
                }
            }
        }

        public void onExit(Cursor cursor, SXRPicker.SXRPickedObject hit)
        {
            if (clickCursor1 == cursor)
            {
                clickCursor1 = null;
                if (clickCursor2 != null)
                {
                    setButtonPress(clickCursor2, hit);
                    prevDistance = 0;
                }
            }
            else if (cursor == clickCursor2)
            {
                clickCursor2 = null;
                if (clickCursor1 != null)
                {
                    setButtonPress(clickCursor1, hit);
                    prevDistance = 0;
                }
            }
        }
        public void onEnter(Cursor cursor, SXRPicker.SXRPickedObject hit) { }
        public void onDrag(Cursor cursor, SXRPicker.SXRPickedObject hit) { }
    };

    float getDistance(SXRNode object) {
        SXRTransform transform = object.getTransform();
        float x = transform.getPositionX();
        float y = transform.getPositionY();
        float z = transform.getPositionZ();
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    float getDistance(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    void onCursorDeactivated(Cursor cursor) {
        int cursorId = cursor.getId();
        ObjectState state = states.get(cursorId);
        if (state != null) {
            states.remove(cursorId);
            if (currentState == state) {
                ObjectState highestPriorityState = getHighestPriorityState();
                setState(highestPriorityState, cursor, null);
            }
        }
    }

    void onCursorActivated(Cursor cursor) {
        states.put(cursor.getId(), ObjectState.DEFAULT);
    }

    /**
     * Set the {@link StateChangedListener} to be associated with the {@link SelectableBehavior}.
     * The {@link StateChangedListener#onStateChanged(SelectableBehavior, ObjectState, ObjectState, Cursor, SXRPicker.SXRPickedObject)}
     * is called every time the state of the {@link SelectableBehavior} is changed.
     *
     * @param listener the {@link StateChangedListener}
     */
    public void setStateChangedListener(StateChangedListener listener) {
        stateChangedListener = listener;
    }

    /**
     * Returns a unique long value Associated with the {@link SelectableBehavior} class. Each
     * subclass of  {@link SXRBehavior} needs a unique component type value. Use this value to
     * get the instance of {@link SelectableBehavior} attached to any {@link SXRNode}
     * using {@link SXRNode#getComponent(long)}
     *
     * @return the component type value.
     */
    public static long getComponentType() {
        return TYPE_SELECTABLE;
    }
}