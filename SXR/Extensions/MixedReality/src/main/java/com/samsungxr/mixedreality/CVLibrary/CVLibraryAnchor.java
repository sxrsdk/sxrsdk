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

package com.samsungxr.mixedreality.CVLibrary;

//import com.google.ar.core.Anchor;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.mixedreality.arcore.ARCorePose;

/**
 * Represents a ARCore anchor in the scene.
 *
 */
public class CVLibraryAnchor extends SXRAnchor {
    protected CVLibraryAnchor(SXRContext ctx)
    {
        super(ctx);
    }

    /**
     * Sets ARCore anchor
     *
     *   ARCore Anchor instance
     */
    protected void setAnchorAR() {

        //this.mAnchor = anchor;
        return;
    }

    /**
     * Set the anchor tracking state
     *
     * @param state
     */
    protected void setTrackingState(SXRTrackingState state) { mTrackingState = state; }

    /**
     * @return ARCore Anchor instance
     */
//    protected Anchor getAnchorAR() {
//
//        //return this.mAnchor;
//        return null;
//    }

    @Override
    public SXRTrackingState getTrackingState() {
        return mTrackingState;
    }

    @Override
    public String getCloudAnchorId() {

        //return mAnchor.getCloudAnchorId();
        return null;
    }

    /**
     * Update the anchor based on arcore best knowledge of the world
     *
     * @param viewmtx
     * @param gvrmatrix
     * @param scale
     */
    protected void update(float[] viewmtx, float[] gvrmatrix, float scale)
    {
        SXRNode owner = getOwnerObject();

        if (owner != null && isEnabled())
        {
            convertFromARtoVRSpace(viewmtx, gvrmatrix, scale);
        }
    }

    public float[] getPose()
    {
        return new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    }

    /**
     * Converts from ARCore world space to SXRf's world space.
     *
     * @param arViewMatrix Phone's camera view matrix.
     * @param vrCamMatrix SXRf Camera matrix.
     * @param scale Scale from AR to SXRf world.
     */
    protected void convertFromARtoVRSpace(float[] arViewMatrix, float[] vrCamMatrix, float scale) {
//        mPose.update(mAnchor.getPose(), arViewMatrix, vrCamMatrix, scale);
//        getTransform().setModelMatrix(mPose.getPoseMatrix());
    }
}
