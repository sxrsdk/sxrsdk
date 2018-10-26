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

package com.samsungxr.mixedreality.arcore;

import com.google.ar.core.Plane;

import com.samsungxr.SXRContext;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.SXRTrackingState;

import java.nio.FloatBuffer;


class ARCorePlane extends SXRPlane {
    private Plane mARPlane;
    private ARCorePose mPose;

    protected ARCorePlane(SXRContext gvrContext, Plane plane) {
        super(gvrContext);
        mPose = new ARCorePose();
        mARPlane = plane;

        if (mARPlane.getType() == Plane.Type.HORIZONTAL_DOWNWARD_FACING) {
            mType = Type.HORIZONTAL_DOWNWARD_FACING;
        }
        else if (mARPlane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
            mType = Type.HORIZONTAL_UPWARD_FACING;
        }
        else {
            mType = Type.VERTICAL;
        }
    }

    /**
     * Set the plane tracking state
     *
     * @param state
     */
    protected void setTrackingState(SXRTrackingState state) {
        mTrackingState = state;
    }

    /**
     * Set the parent plane (only when plane is merged)
     *
     * @param plane
     */
    protected void setParentPlane(SXRPlane plane) {
        mParentPlane = plane;
    }

    @Override
    public SXRTrackingState getTrackingState() {
        return mTrackingState;
    }

    @Override
    public float[] getCenterPose() {
        float[] centerPose = new float[16];
        mARPlane.getCenterPose().toMatrix(centerPose, 0);
        return centerPose;
    }

    @Override
    public Type getPlaneType() {
        return mType;
    }

    @Override
    public float getWidth() {
        return mARPlane.getExtentX();
    }

    @Override
    public float getHeight() {
        return mARPlane.getExtentZ();
    }

    @Override
    public FloatBuffer getPolygon() {
        return mARPlane.getPolygon();
    }

    @Override
    public SXRPlane getParentPlane() {
        return mParentPlane;
    }

    /**
     * Update the plane based on arcore best knowledge of the world
     *
     * @param viewmtx
     * @param gvrmatrix
     * @param scale
     */
    protected void update(float[] viewmtx, float[] gvrmatrix, float scale) {
        // Updates only when the plane is in the scene
        if (getParent() == null || !isEnabled()) {
            return;
        }

        convertFromARtoVRSpace(viewmtx, gvrmatrix, scale);

        if (mNode != null) {
            mNode.getTransform().setScale(mARPlane.getExtentX() * 0.95f,
                    mARPlane.getExtentZ() * 0.95f, 1.0f);
        }
    }
    
    /**
     * Converts from ARCore world space to SXRf's world space.
     *
     * @param arViewMatrix Phone's camera view matrix
     * @param vrCamMatrix SXRf Camera matrix
     * @param scale Scale from AR to SXRf world
     */
    private void convertFromARtoVRSpace(float[] arViewMatrix, float[] vrCamMatrix, float scale) {
        mPose.update(mARPlane.getCenterPose(), arViewMatrix, vrCamMatrix, scale);
        getTransform().setModelMatrix(mPose.getPoseMatrix());
    }
}
