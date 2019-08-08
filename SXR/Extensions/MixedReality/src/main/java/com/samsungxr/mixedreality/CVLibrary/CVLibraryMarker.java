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

//import com.google.ar.core.AugmentedImage;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRTrackingState;

/**
 * Represents an ARCore Augmented Image
 */
public class CVLibraryMarker extends SXRMarker
{
    private final CVLibrarySession mSession;
    private final String mName;

    //private AugmentedImage mAugmentedImage;

    protected CVLibraryMarker(CVLibrarySession session, String name) {
        super(session.getContext());
        mSession = session;
        mName = name;
        mTrackingState = SXRTrackingState.PAUSED;
    }

    public String getName() { return mName; }

    /**
     * @return Returns the estimated width
     */
    @Override
    public float getExtentX() {
        //return mAugmentedImage.getExtentX();
        return 1.0f;
    }

    /**
     * @return Returns the estimated height
     */
    @Override
    public float getExtentZ() {

        return 1.0f;
        //return mAugmentedImage.getExtentZ();
    }

    /**
     * @return The augmented image center pose
     */
    @Override
    public float[] getCenterPose() {
        float[] centerPose = new float[16];
        //mAugmentedImage.getCenterPose().toMatrix(centerPose, 0);
        return centerPose;
    }

    @Override
    public SXRAnchor createAnchor(SXRNode owner)
    {
        return mSession.createAnchor(getCenterPose(), owner);
    }

    /**
     *
     * @return The tracking state
     */
    @Override
    public SXRTrackingState getTrackingState() {
        return mTrackingState;
    }

    /**
     * Set the augmented image tracking state
     *
     * @param state
     */
    protected void setTrackingState(SXRTrackingState state) {
        mTrackingState = state;
    }
}
