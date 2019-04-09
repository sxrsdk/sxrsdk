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

package com.samsungxr.mixedreality;


import android.graphics.Bitmap;

import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;
import com.samsungxr.IEventReceiver;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * This interface defines the AR functionalities of the MixedReality API.
 */
public interface IMixedReality extends IEventReceiver
{
    public interface CloudAnchorCallback
    {
        /**
         * Called when the cloud anchor feature finishes an anchor processing.
         *
         * @param anchor
         */
        void onCloudUpdate(SXRAnchor anchor);
    };

    /**
     * Resume the usage of AR functions.
     */
    void resume();

    /**
     * Pause the usage of AR functions.
     */
    void pause();

    /**
     *
     * @return the scale factor from AR to VR
     */
    float getARToVRScale();


    /**
     * Set the scale factor from AR to VR
     */
    void setARToVRScale(float scale);

    /**
     * Get the Z depth of the touch screen.
     * <p>
     * This is useful if you want to do hit testing
     * based on coordinates on the touch screen.
     * The virtual display has the same dimensions
     * as the real display. The Z coordinate gives
     * the distance of the display from the camera
     * in the virtual scene.
     */
    float getScreenDepth();

    /**
     * The passthrough object is used for AR which
     * uses passthrough video. It is the node in the
     * scene which has the video texture.
     * For an AR headset, the passthrough node will
     * be null.
     * @return The video passthrough object if it exists
     */
    SXRNode getPassThroughObject();

    /**
     * Gets all detected planes.
     *
     * @return A ArrayList of SXRPlanes
     */
    ArrayList<SXRPlane> getAllPlanes();

    /**
     * Create an anchor with the specified pose.
     *
     * @param pose 4x4 matrix with real world position and orientation
     * @param owner {@link SXRNode} to attach the anchor component to.
     *                             This node will be moved and rotated by the anchor.
     * @return The anchor created
     */
    SXRAnchor createAnchor(float[] pose, SXRNode owner);


    /**
     * Update the pose of an anchor.
     * <p>
     * The pose matrix for an anchor is with respect to the real
     * world camera, not the camera for the SXR scene.
     *
     * @param anchor anchor to update
     * @param pose   float array with 4x4 matrix with new pose
     */
    void updateAnchorPose(SXRAnchor anchor, float[] pose);

    /**
     * Remove the anchor specified
     *
     * @param anchor
     */
    void removeAnchor(SXRAnchor anchor);

    /**
     * Host an anchor to be shared
     *
     * @param anchor
     */
    void hostAnchor(SXRAnchor anchor, CloudAnchorCallback cb);

    /**
     * Get an anchor previously hosted
     *
     * @param anchorId
     */
    void resolveCloudAnchor(String anchorId, CloudAnchorCallback cb);

    /**
     * Set if cloud anchors will be available or not
     *
     * @param enableCloudAnchor
     */
    void setEnableCloudAnchor(boolean enableCloudAnchor);

    /**
     * Test collision on plane
     *
     * @param pick    collision returned from SXRPicker
     * @return
     */
    SXRHitResult hitTest(SXRPicker.SXRPickedObject pick);

    /**
     * Test collision on plane
     *
     * @param x
     * @param y
     * @return
     */
    SXRHitResult hitTest(float x, float y);

    /**
     * @return The light estimate
     */
    SXRLightEstimate getLightEstimate();

    /**
     * Set an image to be detected
     *
     * @param image
     */
    void setMarker(Bitmap image);

    /**
     * Set a list of reference images to be detected
     *
     * @param imagesList
     */
    void setMarkers(ArrayList<Bitmap> imagesList);

    /**
     * Get all detected markers
     *
     * @return An ArrayList of SXRMarkers
     */
    ArrayList<SXRMarker> getAllMarkers();

    float[] makeInterpolated(float[] poseA, float[] poseB, float t);

    /**
     * Acquires the current set of estimated 3d points
     * attached to real-world geometry.
     *
     * @return SXRPointCloud with points info
     */
    SXRPointCloud acquirePointCloud();

    /**
     * Set the behavior of the plane detection subsystem.
     *
     * @param mode The mode to find planes (see {@link SXRMixedReality.PlaneFindingMode})
     */
    void setPlaneFindingMode(SXRMixedReality.PlaneFindingMode mode);

}
