package com.samsungxr.physics;

import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTransform;
import com.samsungxr.io.SXRCursorController;
import com.samsungxr.io.SXRInputManager;
import com.samsungxr.utility.Log;

/**
 * This is a helper class to drag a rigid body.
 * It start/stop the drag of the internal virtual pivot object that is used by the physics world
 * as the pivotB of the point joint that is connected to the rigid bodyA that is the target
 * of the dragging action.
 *
 * [Target object with RigidBody]-----(point joint connect to) --->[Pivot object
 *                                                                  that is been dragged by cursor]
 */
class PhysicsDragger {
    private static final String TAG = PhysicsDragger.class.getSimpleName();

    private static final float COLLIDER_HALF_EXT_X = 1f;
    private static final float COLLIDER_HALF_EXT_Y = 1f;
    private static final float COLLIDER_HALF_EXT_Z = 1f;

    private final Object mLock = new Object();

    private final SXRContext mContext;

    private SXRNode mPivotObject = null;
    private SXRNode mDragMe = null;
    private final SXRCursorController mCursorController;

    PhysicsDragger(SXRCursorController controller)
    {
        mContext = controller.getSXRContext();
        mCursorController = controller;
    }

    private static SXRNode onCreatePivotObject(SXRContext gvrContext) {
        SXRNode virtualObj = new SXRNode(gvrContext);
        SXRBoxCollider collider = new SXRBoxCollider(gvrContext);
        collider.setHalfExtents(COLLIDER_HALF_EXT_X, COLLIDER_HALF_EXT_Y, COLLIDER_HALF_EXT_Z);
        virtualObj.attachComponent(collider);

        return virtualObj;
    }

    /**
     * Start the drag of the pivot object.
     *
     * @param dragMe Scene object with a rigid body.
     * @param relX rel position in x-axis.
     * @param relY rel position in y-axis.
     * @param relZ rel position in z-axis.
     *
     * @return Pivot instance if success, otherwise returns null.
     */
    public SXRNode startDrag(SXRNode dragMe, float relX, float relY, float relZ) {
        synchronized (mLock) {
            if (mCursorController == null) {
                Log.w(TAG, "Physics drag failed: Cursor controller not found!");
                return null;
            }

            if (mDragMe != null) {
                Log.w(TAG, "Physics drag failed: Previous drag wasn't finished!");
                return null;
            }

            if (mPivotObject == null) {
                mPivotObject = onCreatePivotObject(mContext);
            }

            mDragMe = dragMe;

            SXRTransform t = dragMe.getTransform();

            /* It is not possible to drag a rigid body directly, we need a pivot object.
            We are using the pivot object's position as pivot of the dragging's physics constraint.
             */
            mPivotObject.getTransform().setPosition(t.getPositionX() + relX,
                    t.getPositionY() + relY, t.getPositionZ() + relZ);

            mCursorController.startDrag(mPivotObject);
        }

        return mPivotObject;
    }

    /**
     * Stops the drag of the pivot object.
     */
    public void stopDrag() {
        if (mDragMe == null)
            return;

        mDragMe = null;
        mCursorController.stopDrag();
    }
}
