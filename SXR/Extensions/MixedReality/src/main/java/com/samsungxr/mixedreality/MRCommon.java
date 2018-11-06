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
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRNode;
import com.samsungxr.debug.cli.CLIException;

import java.util.ArrayList;

public abstract class MRCommon implements IMixedReality
{
    public static String TAG = MRCommon.class.getSimpleName();

    protected final SXRContext mSXRContext;
    protected SXREventReceiver mListeners;

    public MRCommon(SXRContext SXRContext) {
        mSXRContext = SXRContext;
        mListeners = new SXREventReceiver(this);
    }

    @Override
    public SXREventReceiver getEventReceiver() { return mListeners; }

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
    public ArrayList<SXRPlane> getAllPlanes() {
        return onGetAllPlanes();
    }

    @Override
    public SXRNode createAnchorNode(float[] pose)
    {
        SXRAnchor anchor = createAnchor(pose);
        if (anchor != null)
        {
            SXRNode node = new SXRNode(anchor.getSXRContext());
            node.attachComponent(anchor);
            return node;
        }
        return null;
    }

    @Override
    public SXRAnchor createAnchor(float[] pose) {
        return onCreateAnchor(pose);
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
    public void hostAnchor(SXRAnchor anchor, CloudAnchorCallback cb) {
        onHostAnchor(anchor, cb);
    }

    @Override
    public void resolveCloudAnchor(String anchorId, CloudAnchorCallback cb) {
        onResolveCloudAnchor(anchorId, cb);
    }

    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor) {
        onSetEnableCloudAnchor(enableCloudAnchor);
    }

    @Override
    public SXRHitResult hitTest(SXRPicker.SXRPickedObject collision) {
        return onHitTest(collision);
    }

    @Override
    public SXRHitResult hitTest(float x, float y) {
        return onHitTest(x, y);
    }

    @Override
    public SXRLightEstimate getLightEstimate() {
        return onGetLightEstimate();
    }

    @Override
    public void setMarker(Bitmap image) {
        onSetMarker(image);
    }

    @Override
    public void setMarkers(ArrayList<Bitmap> imagesList) {
        onSetMarkers(imagesList);
    }

    @Override
    public ArrayList<SXRMarker> getAllMarkers() {
        return onGetAllMarkers();
    }

    @Override
    public float[] makeInterpolated(float[] poseA, float[] poseB, float t) {
        return onMakeInterpolated(poseA, poseB, t);
    }

    protected abstract void onResume();

    protected abstract void onPause();

    protected abstract SXRNode onGetPassThroughObject();

    protected abstract ArrayList<SXRPlane> onGetAllPlanes();

    protected abstract SXRAnchor onCreateAnchor(float[] pose);

    protected abstract void onUpdateAnchorPose(SXRAnchor anchor, float[] pose);

    protected abstract void onRemoveAnchor(SXRAnchor anchor);

    protected abstract void onHostAnchor(SXRAnchor anchor, CloudAnchorCallback cb);

    protected abstract void onResolveCloudAnchor(String anchorId, CloudAnchorCallback cb);

    protected abstract void onSetEnableCloudAnchor(boolean enableCloudAnchor);

    protected abstract SXRHitResult onHitTest(SXRPicker.SXRPickedObject collision);

    protected abstract SXRHitResult onHitTest(float x, float y);

    protected abstract SXRLightEstimate onGetLightEstimate();

    protected abstract void onSetMarker(Bitmap image);

    protected abstract void onSetMarkers(ArrayList<Bitmap> imagesList);

    protected abstract ArrayList<SXRMarker> onGetAllMarkers();

    protected abstract float[] onMakeInterpolated(float[] poseA, float[] poseB, float t);
}
