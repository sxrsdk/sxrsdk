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

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.IActivityEvents;
import com.samsungxr.mixedreality.arcore.ARCoreSession;
import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * Component to enable AR functionalities on SXRf.
 */
public class SXRMixedReality extends SXRBehavior implements IMixedReality
{
    static private long TYPE_MIXEDREALITY = newComponentType(SXRMixedReality.class);
    private final IMixedReality mSession;
    private SessionState mState;
    private Vector3f mTempVec1 = new Vector3f();
    private Vector3f mTempVec2 = new Vector3f();

    /**
     * Create a instace of SXRMixedReality component.
     *
     * @param SXRContext
     */
    public SXRMixedReality(final SXRContext SXRContext)
    {
        this(SXRContext, false);
    }

    /**
     * Create a instace of SXRMixedReality component and specifies the use of cloud anchors.
     *
     * @param SXRContext
     * @param enableCloudAnchor
     */
    public SXRMixedReality(final SXRContext SXRContext, boolean enableCloudAnchor)
    {
        this(SXRContext.getMainScene(), enableCloudAnchor);
    }

    /**
     * Create a instance of SXRMixedReality component and add it to the specified scene.
     *
     * @param scene
     */
    public SXRMixedReality(SXRScene scene)
    {
        this(scene, false);
    }

    /**
     * Default SXRMixedReality constructor. Create a instace of SXRMixedReality component, set
     * the use of cloud anchors and add it to the specified scened.
     *
     * @param scene
     */
    public SXRMixedReality(SXRScene scene, boolean enableCloudAnchor)
    {
        this(scene, enableCloudAnchor, "arcore");
    }

    /**
     * Default SXRMixedReality constructor. Create a instace of SXRMixedReality component, set
     * the use of cloud anchors and add it to the specified scened.
     *
     * @param scene scene containing the virtual objects
     * @param enableCloudAnchor true to enable cloud anchors, false to disable
     * @param arPlatform    string with name of underlying AR platform to use:
     *                      "arcore" indicates to use Google AR Core.
     */
    public SXRMixedReality(SXRScene scene, boolean enableCloudAnchor, String arPlatform)
    {
        super(scene.getSXRContext());
        mType = getComponentType();
        if (arPlatform.equals("arcore"))
        {
            mSession = new ARCoreSession(scene, enableCloudAnchor);
        }
        else throw new IllegalArgumentException("ARCore is the only AR platform currently supported");
        mState = SessionState.ON_PAUSE;
        scene.getMainCameraRig().getOwnerObject().attachComponent(this);
    }

    static public long getComponentType() { return TYPE_MIXEDREALITY; }

    @Override
    public float getARToVRScale() { return mSession.getARToVRScale(); }

    @Override
    public float getScreenDepth() { return mSession.getScreenDepth(); }

    @Override
    public SXREventReceiver getEventReceiver() { return mSession.getEventReceiver(); }

    @Override
    public void resume() {
        if (mState == SessionState.ON_RESUME) {
            return;
        }
        mSession.resume();
        mState = SessionState.ON_RESUME;
    }

    @Override
    public void pause() {
        if (mState == SessionState.ON_PAUSE) {
            return;
        }
        mSession.pause();
        mState = SessionState.ON_PAUSE;
    }

    @Override
    public SXRNode getPassThroughObject() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getPassThroughObject();
    }

    @Override
    public ArrayList<SXRPlane> getAllPlanes() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getAllPlanes();
    }

    @Override
    public SXRAnchor createAnchor(float[] pose) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.createAnchor(pose);
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
    public void updateAnchorPose(SXRAnchor anchor, float[] pose) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        mSession.updateAnchorPose(anchor, pose);
    }

    @Override
    public void removeAnchor(SXRAnchor anchor) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        mSession.removeAnchor(anchor);
    }

    @Override
    public void hostAnchor(SXRAnchor anchor, CloudAnchorCallback cb) {
        mSession.hostAnchor(anchor, cb);
    }

    @Override
    public void resolveCloudAnchor(String anchorId, CloudAnchorCallback cb) {
        mSession.resolveCloudAnchor(anchorId, cb);
    }

    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor) {
        mSession.setEnableCloudAnchor(enableCloudAnchor);
    }

    @Override
    public SXRHitResult hitTest(SXRPicker.SXRPickedObject collision)
    {
        if (mState == SessionState.ON_PAUSE)
        {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        collision.picker.getWorldPickRay(mTempVec1, mTempVec2);
        if (collision.hitObject != getPassThroughObject())
        {
            mTempVec2.set(collision.hitLocation[0],
                          collision.hitLocation[1],
                          collision.hitLocation[2]);
        }
        SXRPicker.SXRPickedObject hit = SXRPicker.pickNode(getPassThroughObject(), mTempVec1.x, mTempVec1.y, mTempVec1.z,
                                                           mTempVec2.x, mTempVec2.y, mTempVec2.z);
        if (hit == null)
        {
            return null;
        }
        return mSession.hitTest(hit);
    }

    @Override
    public SXRHitResult hitTest(float x, float y) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.hitTest(x, y);
    }

    @Override
    public SXRLightEstimate getLightEstimate() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getLightEstimate();
    }

    @Override
    public void setMarker(Bitmap image) {
        mSession.setMarker(image);
    }

    @Override
    public void setMarkers(ArrayList<Bitmap> imagesList) {
        mSession.setMarkers(imagesList);
    }

    @Override
    public ArrayList<SXRMarker> getAllMarkers() {
        return mSession.getAllMarkers();
    }

    @Override
    public float[] makeInterpolated(float[] poseA, float[] poseB, float t) {
        return mSession.makeInterpolated(poseA, poseB, t);
    }

    private class ActivityEventsHandler extends SXREventListeners.ActivityEvents {
        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onResume() {
            resume();
        }
    }

    private enum SessionState {
        ON_RESUME,
        ON_PAUSE
    };
}
