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
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRAugmentedImage;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRLightEstimate;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.mixedreality.IAnchorEventsListener;
import com.samsungxr.mixedreality.IAugmentedImageEventsListener;
import com.samsungxr.mixedreality.IPlaneEventsListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ARCoreHelper {
    private SXRContext mGvrContext;
    private SXRScene mGvrScene;

    private Map<Plane, ARCorePlane> mArPlanes;
    private Map<AugmentedImage, ARCoreAugmentedImage> mArAugmentedImages;
    private List<ARCoreAnchor> mArAnchors;

    private ArrayList<IPlaneEventsListener> planeEventsListeners = new ArrayList<>();
    private ArrayList<IAnchorEventsListener> anchorEventsListeners = new ArrayList<>();
    private ArrayList<IAugmentedImageEventsListener> augmentedImageEventsListeners = new ArrayList<>();

    public ARCoreHelper(SXRContext gvrContext, SXRScene gvrScene) {
        mGvrContext = gvrContext;
        mGvrScene = gvrScene;
        mArPlanes = new HashMap<>();
        mArAugmentedImages = new HashMap<>();
        mArAnchors = new ArrayList<>();
    }

    public void updatePlanes(Collection<Plane> allPlanes, float[] arViewMatrix,
                             float[] vrCamMatrix, float scale) {
        ARCorePlane arCorePlane;

        for (Plane plane: allPlanes) {
            if (plane.getTrackingState() != TrackingState.TRACKING
                    || mArPlanes.containsKey(plane)) {
                continue;
            }

            arCorePlane = createPlane(plane);
            // FIXME: New planes are updated two times
            arCorePlane.update(arViewMatrix, vrCamMatrix, scale);
            notifyPlaneDetectionListeners(arCorePlane);
        }

        for (Plane plane: mArPlanes.keySet()) {
            arCorePlane = mArPlanes.get(plane);

            if (plane.getTrackingState() == TrackingState.TRACKING &&
                    arCorePlane.getTrackingState() != SXRTrackingState.TRACKING) {
                arCorePlane.setTrackingState(SXRTrackingState.TRACKING);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.TRACKING);
            }
            else if (plane.getTrackingState() == TrackingState.PAUSED &&
                    arCorePlane.getTrackingState() != SXRTrackingState.PAUSED) {
                arCorePlane.setTrackingState(SXRTrackingState.PAUSED);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.PAUSED);
            }
            else if (plane.getTrackingState() == TrackingState.STOPPED &&
                    arCorePlane.getTrackingState() != SXRTrackingState.STOPPED) {
                arCorePlane.setTrackingState(SXRTrackingState.STOPPED);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.STOPPED);
            }

            if (plane.getSubsumedBy() != null && arCorePlane.getParentPlane() == null) {
                arCorePlane.setParentPlane(mArPlanes.get(plane.getSubsumedBy()));
                notifyMergedPlane(arCorePlane, arCorePlane.getParentPlane());
            }

            arCorePlane.update(arViewMatrix, vrCamMatrix, scale);
        }
    }

    public void updateAugmentedImages(Collection<AugmentedImage> allAugmentedImages){
        ARCoreAugmentedImage arCoreAugmentedImage;

        for (AugmentedImage augmentedImage: allAugmentedImages) {
            if (augmentedImage.getTrackingState() != TrackingState.TRACKING
                || mArAugmentedImages.containsKey(augmentedImage)) {
                continue;
            }

            arCoreAugmentedImage = createAugmentedImage(augmentedImage);
            notifyAugmentedImageDetectionListeners(arCoreAugmentedImage);

            mArAugmentedImages.put(augmentedImage, arCoreAugmentedImage);
        }

        for (AugmentedImage augmentedImage: mArAugmentedImages.keySet()) {
            arCoreAugmentedImage = mArAugmentedImages.get(augmentedImage);

            if (augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                    arCoreAugmentedImage.getTrackingState() != SXRTrackingState.TRACKING) {
                arCoreAugmentedImage.setTrackingState(SXRTrackingState.TRACKING);
                notifyAugmentedImageStateChangeListeners(arCoreAugmentedImage, SXRTrackingState.TRACKING);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.PAUSED &&
                    arCoreAugmentedImage.getTrackingState() != SXRTrackingState.PAUSED) {
                arCoreAugmentedImage.setTrackingState(SXRTrackingState.PAUSED);
                notifyAugmentedImageStateChangeListeners(arCoreAugmentedImage, SXRTrackingState.PAUSED);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.STOPPED &&
                    arCoreAugmentedImage.getTrackingState() != SXRTrackingState.STOPPED) {
                arCoreAugmentedImage.setTrackingState(SXRTrackingState.STOPPED);
                notifyAugmentedImageStateChangeListeners(arCoreAugmentedImage, SXRTrackingState.STOPPED);
            }
        }
    }

    public void updateAnchors(float[] arViewMatrix, float[] vrCamMatrix, float scale) {
        for (ARCoreAnchor anchor: mArAnchors) {
            Anchor arAnchor = anchor.getAnchorAR();

            if (arAnchor.getTrackingState() == TrackingState.TRACKING &&
                    anchor.getTrackingState() != SXRTrackingState.TRACKING) {
                anchor.setTrackingState(SXRTrackingState.TRACKING);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.TRACKING);
            }
            else if (arAnchor.getTrackingState() == TrackingState.PAUSED &&
                    anchor.getTrackingState() != SXRTrackingState.PAUSED) {
                anchor.setTrackingState(SXRTrackingState.PAUSED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.PAUSED);
            }
            else if (arAnchor.getTrackingState() == TrackingState.STOPPED &&
                    anchor.getTrackingState() != SXRTrackingState.STOPPED) {
                anchor.setTrackingState(SXRTrackingState.STOPPED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.STOPPED);
            }

            anchor.update(arViewMatrix, vrCamMatrix, scale);
        }
    }

    public ArrayList<SXRPlane> getAllPlanes() {
        ArrayList<SXRPlane> allPlanes = new ArrayList<>();

        for (Plane plane: mArPlanes.keySet()) {
            allPlanes.add(mArPlanes.get(plane));
        }

        return allPlanes;
    }

    public ArrayList<SXRAugmentedImage> getAllAugmentedImages() {
        ArrayList<SXRAugmentedImage> allAugmentedImages = new ArrayList<>();

        for (AugmentedImage augmentedImage: mArAugmentedImages.keySet()) {
            allAugmentedImages.add(mArAugmentedImages.get(augmentedImage));
        }

        return allAugmentedImages;
    }

    public ARCorePlane createPlane(Plane plane) {
        ARCorePlane arCorePlane = new ARCorePlane(mGvrContext, plane);
        mArPlanes.put(plane, arCorePlane);
        return arCorePlane;
    }

    public ARCoreAugmentedImage createAugmentedImage(AugmentedImage augmentedImage) {
        ARCoreAugmentedImage arCoreAugmentedImage = new ARCoreAugmentedImage(augmentedImage);
        return arCoreAugmentedImage;
    }

    public SXRAnchor createAnchor(Anchor arAnchor, SXRSceneObject sceneObject) {
        ARCoreAnchor arCoreAnchor = new ARCoreAnchor(mGvrContext);
        arCoreAnchor.setAnchorAR(arAnchor);
        mArAnchors.add(arCoreAnchor);

        if (sceneObject != null) {
            arCoreAnchor.attachSceneObject(sceneObject);
        }

        return arCoreAnchor;
    }

    public void updateAnchorPose(ARCoreAnchor anchor, Anchor arAnchor) {
        if (anchor.getAnchorAR() != null) {
            anchor.getAnchorAR().detach();
        }
        anchor.setAnchorAR(arAnchor);
    }

    public void removeAnchor(ARCoreAnchor anchor) {
        anchor.getAnchorAR().detach();
        mArAnchors.remove(anchor);
        mGvrScene.removeSceneObject(anchor);
    }

    public SXRHitResult hitTest(List<HitResult> hitResult) {
        for (HitResult hit : hitResult) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            Trackable trackable = hit.getTrackable();
            // Creates an anchor if a plane or an oriented point was hit.
            if ((trackable instanceof Plane
                    && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                    && ((Plane) trackable).getSubsumedBy() == null) {
                SXRHitResult gvrHitResult = new SXRHitResult();
                float[] hitPose = new float[16];

                hit.getHitPose().toMatrix(hitPose, 0);
                gvrHitResult.setPose(hitPose);
                gvrHitResult.setDistance(hit.getDistance());
                gvrHitResult.setPlane(mArPlanes.get(trackable));

                return gvrHitResult;
            }
        }

        return null;
    }

    public SXRLightEstimate getLightEstimate(LightEstimate lightEstimate) {
        ARCoreLightEstimate arCoreLightEstimate = new ARCoreLightEstimate();
        SXRLightEstimate.SXRLightEstimateState state;

        arCoreLightEstimate.setPixelIntensity(lightEstimate.getPixelIntensity());
        state = (lightEstimate.getState() == LightEstimate.State.VALID) ?
                SXRLightEstimate.SXRLightEstimateState.VALID :
                SXRLightEstimate.SXRLightEstimateState.NOT_VALID;
        arCoreLightEstimate.setState(state);

        return arCoreLightEstimate;
    }

    public void registerPlaneListener(IPlaneEventsListener listener) {
        planeEventsListeners.add(listener);
    }

    public void registerAnchorListener(IAnchorEventsListener listener) {
        anchorEventsListeners.add(listener);
    }

    public void registerAugmentedImageListener(IAugmentedImageEventsListener listener) {
        augmentedImageEventsListeners.add(listener);
    }

    private void notifyPlaneDetectionListeners(SXRPlane plane) {
        for (IPlaneEventsListener listener: planeEventsListeners) {
            listener.onPlaneDetection(plane);
        }
    }

    private void notifyPlaneStateChangeListeners(SXRPlane plane, SXRTrackingState trackingState) {
        for (IPlaneEventsListener listener: planeEventsListeners) {
            listener.onPlaneStateChange(plane, trackingState);
        }
    }

    private void notifyMergedPlane(SXRPlane childPlane, SXRPlane parentPlane) {
        for (IPlaneEventsListener listener: planeEventsListeners) {
            listener.onPlaneMerging(childPlane, parentPlane);
        }
    }

    private void notifyAnchorStateChangeListeners(SXRAnchor anchor, SXRTrackingState trackingState) {
        for (IAnchorEventsListener listener: anchorEventsListeners) {
            listener.onAnchorStateChange(anchor, trackingState);
        }
    }

    private void notifyAugmentedImageDetectionListeners(SXRAugmentedImage image) {
        for (IAugmentedImageEventsListener listener: augmentedImageEventsListeners) {
            listener.onAugmentedImageDetection(image);
        }
    }

    private void notifyAugmentedImageStateChangeListeners(SXRAugmentedImage image, SXRTrackingState trackingState) {
        for (IAugmentedImageEventsListener listener: augmentedImageEventsListeners) {
            listener.onAugmentedImageStateChange(image, trackingState);
        }
    }
}
