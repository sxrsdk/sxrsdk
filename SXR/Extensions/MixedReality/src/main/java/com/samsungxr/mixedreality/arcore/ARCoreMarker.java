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

import com.google.ar.core.AugmentedImage;

import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRTrackingState;

/**
 * Represents an ARCore Augmented Image
 */
public class ARCoreMarker extends SXRMarker {
    private AugmentedImage mAugmentedImage;

    protected ARCoreMarker(AugmentedImage augmentedImage) {
        mAugmentedImage = augmentedImage;
        mTrackingState = SXRTrackingState.PAUSED;
    }

    /**
     * @return Returns the estimated width
     */
    @Override
    public float getExtentX() {
        return mAugmentedImage.getExtentX();
    }

    /**
     * @return Returns the estimated height
     */
    @Override
    public float getExtentZ() {
        return mAugmentedImage.getExtentZ();
    }

    /**
     * @return The augmented image center pose
     */
    @Override
    public float[] getCenterPose() {
        float[] centerPose = new float[16];
        mAugmentedImage.getCenterPose().toMatrix(centerPose, 0);
        return centerPose;
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
