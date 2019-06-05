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

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;

/**
 * Represents a ARCore anchor in the scene.
 *
 */
public class ARCoreAnchor extends SXRAnchor
{
    private final ARCoreSession mSession;
    private Anchor mAnchor;
    private float[] mPose = new float[16];

    protected ARCoreAnchor(ARCoreSession session)
    {
        super(session.getSXRContext());
        mSession = session;
    }

    /**
     * Sets ARCore anchor
     *
     * @param anchor ARCore Anchor instance
     */
    protected void setAnchorAR(Anchor anchor) {
        this.mAnchor = anchor;
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
    protected Anchor getAnchorAR() {
        return this.mAnchor;
    }

    @Override
    public SXRTrackingState getTrackingState() {
        return mTrackingState;
    }

    @Override
    public String getCloudAnchorId() {
        return mAnchor.getCloudAnchorId();
    }

    /**
     * Update the anchor based on arcore best knowledge of the world
     */
    protected void update()
    {
        // Updates only when the plane is in the scene
        SXRNode owner = getOwnerObject();

        if ((owner != null) && isEnabled() && owner.isEnabled())
        {
            float[] mtx = getPose();
            Log.v("NOLA", "anchor %f, %f, %f", mtx[12], mtx[13], mtx[14]);
            getOwnerObject().getTransform().setModelMatrix(mtx);
        }
    }

    public final float[] getPose()
    {
        mAnchor.getPose().toMatrix(mPose, 0);
        //mSession.ar2gvr(mPose);
        return mPose;
    }

    public void detach()
    {
        if (mAnchor != null)
        {
            mAnchor.detach();
        }
    }
}
