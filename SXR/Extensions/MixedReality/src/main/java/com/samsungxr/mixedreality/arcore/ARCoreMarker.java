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
import com.google.ar.core.AugmentedImage;

import com.google.ar.core.Pose;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRTrackingState;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents an ARCore Augmented Image
 */
public class ARCoreMarker extends SXRMarker
{
    private final AugmentedImage mAugmentedImage;
    private final ARCoreSession mSession;
    private SXRAnchor mAnchor;

    protected ARCoreMarker(ARCoreSession session, AugmentedImage augmentedImage)
    {
        super(session.getSXRContext());
        mSession = session;
        mAugmentedImage = augmentedImage;
        mTrackingState = SXRTrackingState.PAUSED;
        mAnchor = null;
    }

    /**
     * @return the ARCore AugmentedImage associated with this marker
     */
    public AugmentedImage getImage() { return mAugmentedImage; }

    /**
     * @return the name of the marker
     */
    @Override
    public String getName() { return mAugmentedImage.getName(); }

    /**
     * @return Returns the estimated width
     */
    @Override
    public float getExtentX() {
        return mAugmentedImage.getExtentX() * mSession.getARToVRScale();
    }

    /**
     * @return Returns the estimated height
     */
    @Override
    public float getExtentZ() {
        return mAugmentedImage.getExtentZ() * mSession.getARToVRScale();
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

    public SXRAnchor getAnchor() { return mAnchor; }

    @Override
    public SXRAnchor createAnchor(SXRNode owner)
    {
        Pose arpose = mAugmentedImage.getCenterPose();
        Anchor aranchor = mAugmentedImage.createAnchor(arpose);
        mAnchor = mSession.addAnchor(aranchor, owner);
        return mAnchor;
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
