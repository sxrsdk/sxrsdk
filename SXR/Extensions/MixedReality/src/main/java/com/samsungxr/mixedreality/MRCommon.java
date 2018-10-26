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

import com.samsungxr.SXRContext;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;

import java.util.ArrayList;

public abstract class MRCommon implements IMRCommon {
    public static String TAG = MRCommon.class.getSimpleName();

    protected final SXRContext mGvrContext;

    public MRCommon(SXRContext gvrContext) {
        mGvrContext = gvrContext;
    }

    @Override
    public void resume() {
        onResume();
    }

    @Override
    public void pause() {
        onPause();
    }

    @Override
    public SXRNode getPassThroughObject() {
        return onGetPassThroughObject();
    }

    @Override
    public void registerPlaneListener(IPlaneEventsListener listener) {
        onRegisterPlaneListener(listener);
    }

    @Override
    public void registerAnchorListener(IAnchorEventsListener listener) {
        onRegisterAnchorListener(listener);
    }

    @Override
    public void registerAugmentedImageListener(IAugmentedImageEventsListener listener) {
        onRegisterAugmentedImageListener(listener);
    }

    @Override
    public ArrayList<SXRPlane> getAllPlanes() {
        return onGetAllPlanes();
    }

    @Override
    public SXRAnchor createAnchor(float[] pose) {
        return onCreateAnchor(pose, null);
    }

    @Override
    public SXRAnchor createAnchor(float[] pose, SXRNode sceneObject) {
        return onCreateAnchor(pose, sceneObject);
    }

    @Override
    public void updateAnchorPose(SXRAnchor anchor, float[] pose) {
        onUpdateAnchorPose(anchor, pose);
    }

    @Override
    public void removeAnchor(SXRAnchor anchor) {
        onRemoveAnchor(anchor);
    }

    @Override
    public void hostAnchor(SXRAnchor anchor, ICloudAnchorListener listener) {
        onHostAnchor(anchor, listener);
    }

    @Override
    public void resolveCloudAnchor(String anchorId, ICloudAnchorListener listener) {
        onResolveCloudAnchor(anchorId, listener);
    }

    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor) {
        onSetEnableCloudAnchor(enableCloudAnchor);
    }

    @Override
    public SXRHitResult hitTest(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {
        return onHitTest(sceneObj, collision);
    }

    @Override
    public SXRLightEstimate getLightEstimate() {
        return onGetLightEstimate();
    }

    @Override
    public void setAugmentedImage(Bitmap image) {
        onSetAugmentedImage(image);
    }

    @Override
    public void setAugmentedImages(ArrayList<Bitmap> imagesList) {
        onSetAugmentedImages(imagesList);
    }

    @Override
    public ArrayList<SXRAugmentedImage> getAllAugmentedImages() {
        return onGetAllAugmentedImages();
    }

    protected abstract void onResume();

    protected abstract void onPause();

    protected abstract SXRNode onGetPassThroughObject();

    protected abstract void onRegisterPlaneListener(IPlaneEventsListener listener);

    protected abstract void onRegisterAnchorListener(IAnchorEventsListener listener);

    protected abstract void onRegisterAugmentedImageListener(IAugmentedImageEventsListener listener);

    protected abstract ArrayList<SXRPlane> onGetAllPlanes();

    protected abstract SXRAnchor onCreateAnchor(float[] pose, SXRNode sceneObject);

    protected abstract void onUpdateAnchorPose(SXRAnchor anchor, float[] pose);

    protected abstract void onRemoveAnchor(SXRAnchor anchor);

    protected  abstract void onHostAnchor(SXRAnchor anchor, ICloudAnchorListener listener);

    protected abstract void onResolveCloudAnchor(String anchorId, ICloudAnchorListener listener);

    protected abstract void onSetEnableCloudAnchor(boolean enableCloudAnchor);

    protected abstract SXRHitResult onHitTest(SXRNode sceneObj, SXRPicker.SXRPickedObject collision);

    protected abstract SXRLightEstimate onGetLightEstimate();

    protected abstract void onSetAugmentedImage(Bitmap image);

    protected abstract void onSetAugmentedImages(ArrayList<Bitmap> imagesList);

    protected abstract ArrayList<SXRAugmentedImage> onGetAllAugmentedImages();
}
