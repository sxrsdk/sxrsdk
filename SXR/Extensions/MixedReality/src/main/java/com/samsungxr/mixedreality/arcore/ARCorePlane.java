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

import android.opengl.Matrix;
import android.support.annotation.NonNull;

import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
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
            mPlaneType = Type.HORIZONTAL_DOWNWARD_FACING;
        }
        else if (mARPlane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
            mPlaneType = Type.HORIZONTAL_UPWARD_FACING;
        }
        else {
            mPlaneType = Type.VERTICAL;
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
    public void getCenterPose(@NonNull float[] poseOut) {
        if(poseOut.length != 16 ){
            throw new IllegalArgumentException("Array must be 16");
        }
        mARPlane.getCenterPose().toMatrix(poseOut, 0);
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

    @Override
    public boolean isPoseInPolygon(float[] pose) {

        float[] translation = new float[3];
        float[] rotation = new float[4];
        float[] arPose;

        arPose = pose.clone();

        ARCoreSession.gvr2ar(arPose);
        ARCoreSession.convertMatrixPoseToVector(arPose, translation, rotation);

        return mARPlane.isPoseInPolygon(new Pose(translation, rotation));
    }

    /**
     * Update the plane based on arcore best knowledge of the world
     *
     * @param scale
     */
    protected void update(float scale) {
        SXRNode owner = getOwnerObject();
        if (isEnabled() && (owner != null) && owner.isEnabled())
        {
            float w = getWidth();
            float h = getHeight();
            mPose.update(mARPlane.getCenterPose(), scale);

            Matrix.scaleM(mPose.getPoseMatrix(), 0, w * 0.95f, 1.0f, h * 0.95f);

            owner.getTransform().setModelMatrix(mPose.getPoseMatrix());
        }
    }
    
    /**
     * Converts from ARCore world space to SXRf's world space.
     *
     * @param scale Scale from AR to SXRf world
     */
    private void convertFromARtoVRSpace(float scale) {
        mPose.update(mARPlane.getCenterPose(), scale);
        getTransform().setModelMatrix(mPose.getPoseMatrix());
    }
}
