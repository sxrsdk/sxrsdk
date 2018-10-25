package com.samsungxr.io.cursor3d;

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRComponent;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRSceneObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * This class defines a {@link SelectableBehavior} that can be attached to a {@link SXRSceneObject}.
 * In addition to all the functionality of a {@link SelectableBehavior} this makes the associated
 * {@link SXRSceneObject} movable with a {@link Cursor}.
 */
public class MovableBehavior extends SelectableBehavior {
    public static final String TAG = MovableBehavior.class.getSimpleName();
    static private long TYPE_MOVABLE = newComponentType(MovableBehavior.class);
    private Vector3f mCursorPosition;
    private Quaternionf mRotation;
    private Vector3f mTempCross;

    private SXRSceneObject mSelected;
    private Cursor mCurrentCursor;
    private SXRSceneObject mOwnerParent;
    private static final Matrix4f mTempParentMatrix = new Matrix4f();
    private static final Matrix4f mTempSelectedMatrix = new Matrix4f();

    /**
     * Creates a {@link MovableBehavior} to be attached to any {@link SXRSceneObject}. The
     * instance thus created will not change the appearance of the linked {@link SXRSceneObject}
     * according to the {@link ObjectState}.
     *
     * @param cursorManager the {@link CursorManager} instance
     */
    public MovableBehavior(CursorManager cursorManager) {
        this(cursorManager, false);
    }

    /**
     * Creates a {@link MovableBehavior} which is to be attached to a {@link SXRSceneObject}
     * with a specific hierarchy where, the {@link SXRSceneObject} to be attached has a root node at
     * the top and a child for each of the states of the {@link MovableBehavior}.
     * The order of the child nodes has to follow {@link ObjectState#DEFAULT},
     * {@link ObjectState#BEHIND}, {@link ObjectState#COLLIDING}, and {@link ObjectState#CLICKED}
     * from left to right.The {@link MovableBehavior} handles all the {@link ICursorEvents} on the
     * linked {@link SXRSceneObject} and maintains the {@link ObjectState} as well as moving the
     * {@link SXRSceneObject} with the movement of a {@link Cursor}. It also makes the correct child
     * {@link SXRSceneObject} visible according to the {@link ObjectState}. It is recommended that
     * the different nodes representing different {@link ObjectState} share a common set of vertices
     * if possible. Not having the needed hierarchy will result in an
     * {@link IllegalArgumentException} when calling
     * {@link SXRSceneObject#attachComponent(SXRComponent)}
     *
     * @param cursorManager       the {@link CursorManager} instance
     * @param initializeAllStates flag to indicate whether to initialize all
     *                            possible {@link ObjectState}s. <code>true</code> initialize all
     *                            states in order from left to right:{@link ObjectState#DEFAULT},
     *                            {@link ObjectState#BEHIND}, {@link ObjectState#COLLIDING}, and
     *                            {@link ObjectState#CLICKED}. <code>false</code> initializes only
     *                            the {@link ObjectState#DEFAULT} state. Which does not require the
     *                            attached  {@link SXRSceneObject} to have a hierarchy.
     */
    public MovableBehavior(CursorManager cursorManager, boolean initializeAllStates) {
        super(cursorManager, initializeAllStates);
        initialize(cursorManager);
    }

    /**
     * Creates a {@link MovableBehavior} which is to be attached to a {@link SXRSceneObject}
     * with a specific hierarchy where, the {@link SXRSceneObject} to be attached has a root node at
     * the top and a child for each of the states of the {@link MovableBehavior}.
     * The {@link MovableBehavior} handles all the {@link ICursorEvents} on the linked
     * {@link SXRSceneObject} and maintains the {@link ObjectState} as well as moves the associated
     * {@link SXRSceneObject} with the {@link Cursor}. It also makes the correct child
     * {@link SXRSceneObject} visible according to the {@link ObjectState}. It is recommended to
     * have a child for each {@link ObjectState} value, however it is possible to have lesser
     * children as long as the mapping is correctly specified in {@param objectStates}. If the
     * {@param objectStates} does not match the {@link SXRSceneObject} hierarchy it will result in
     * an {@link IllegalArgumentException} when calling
     * {@link SXRSceneObject#attachComponent(SXRComponent)}. To save on memory it is suggested that
     * the children of the {@link SXRSceneObject} representing different {@link ObjectState}s share
     * a common set of vertices if possible.
     *
     * @param cursorManager the {@link CursorManager} instance
     * @param objectStates  array of {@link ObjectState}s that maps each child of the attached
     *                      {@link SXRSceneObject} with an {@link ObjectState}. Where the first
     *                      element of the array maps to the left most/first child of the attached
     *                      {@link SXRSceneObject}. This array should contain
     *                      {@link ObjectState#DEFAULT} as one of its elements else it will result
     *                      in an {@link IllegalArgumentException}.
     */
    public MovableBehavior(CursorManager cursorManager, ObjectState[] objectStates) {
        super(cursorManager, objectStates);
        initialize(cursorManager);
    }

    private void initialize(final CursorManager cursorManager) {
        mCursorPosition = new Vector3f();
        mRotation = new Quaternionf();
        mTempCross = new Vector3f();
        mType = getComponentType();
        clickListener = new ICursorEvents()
        {
            public void onCursorScale(Cursor c) { }
            public void onEnter(Cursor c, SXRPicker.SXRPickedObject hit) { }
            public void onTouchStart(Cursor c, SXRPicker.SXRPickedObject hit)
            {
                synchronized (this)
                {
                    SXRCursorController controller = hit.getPicker().getController();

                    if (mSelected != null)
                    {
                        return;
                    }
                    mCurrentCursor = c;
                    mSelected = getOwnerObject();
                    controller.startDrag(mSelected);
                }
            }

            public void onDrag(Cursor c, SXRPicker.SXRPickedObject hit) { }

            public void onExit(Cursor c, SXRPicker.SXRPickedObject hit)
            {
                onTouchEnd(c, hit);
            }

            public void onTouchEnd(Cursor c, SXRPicker.SXRPickedObject hit)
            {
                synchronized (this)
                {
                    if ((mSelected == null) || (mCurrentCursor != c))
                    {
                        return;
                    }
                    SXRCursorController controller = hit.getPicker().getController();

                    controller.stopDrag();
                    mSelected = null;
                    // object has been moved, invalidate all other cursors to check for events
                    for (Cursor remaining : cursorManager.getActiveCursors())
                    {
                        if (mCurrentCursor != remaining)
                        {
                            remaining.invalidate();
                        }
                    }
                }
            }

            private void rotateObjectToFollowCursor(Vector3f cursorPosition)
            {
                computeRotation(mCursorPosition, cursorPosition);
                getOwnerObject().getTransform().rotateWithPivot(mRotation.w, mRotation.x, mRotation.y, mRotation.z, 0, 0, 0);
                getOwnerObject().getTransform().setRotation(1, 0, 0, 0);
            }

            /*
            formulae for quaternion mRotation taken from
            http://lolengine.net/blog/2014/02/24/quaternion-from-two-vectors-final
            */
            private void computeRotation(Vector3f start, Vector3f end)
            {
                float norm_u_norm_v = (float) Math.sqrt(start.dot(start) * end.dot(end));
                float real_part = norm_u_norm_v + start.dot(end);

                if (real_part < 1.e-6f * norm_u_norm_v)
                {
            /* If u and v are exactly opposite, rotate 180 degrees
             * around an arbitrary orthogonal axis. Axis normalisation
             * can happen later, when we normalise the quaternion. */
                    real_part = 0.0f;
                    if (Math.abs(start.x) > Math.abs(start.z))
                    {
                        mTempCross.set(-start.y, start.x, 0.f);
                    }
                    else
                    {
                        mTempCross.set(0.f, -start.z, start.y);
                    }
                }
                else
                {
                    /* Otherwise, build quaternion the standard way. */
                    start.cross(end, mTempCross);
                }
                mRotation.set(mTempCross.x, mTempCross.y, mTempCross.z, real_part).normalize();
            }

        };
    }


    /**
     * Returns a unique long value associated with the {@link MovableBehavior} class. Each
     * subclass of  {@link SXRBehavior} needs a unique component type value. Use this value to
     * get the instance of {@link MovableBehavior} attached to any {@link SXRSceneObject}
     * using {@link SXRSceneObject#getComponent(long)}
     *
     * @return the component type value.
     */
    public static long getComponentType() {
        return TYPE_MOVABLE;
    }
}

