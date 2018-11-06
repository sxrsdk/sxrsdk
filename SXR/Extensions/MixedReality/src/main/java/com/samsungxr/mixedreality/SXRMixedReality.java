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
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.IActivityEvents;
import com.samsungxr.mixedreality.arcore.ARCoreSession;
import com.samsungxr.mixedreality.CVLibrary.CVLibrarySession;

import java.util.ArrayList;

/**
 * Component to enable AR functionalities on SXRf.
 */
public class SXRMixedReality extends SXRBehavior implements IMRCommon {
    private final IActivityEvents mActivityEventsHandler;
    private MRCommon mSession = null;
    private SessionState mState;

    /**
     * Create a instace of SXRMixedReality component.
     *
     * @param gvrContext
     */
    public SXRMixedReality(final SXRContext gvrContext) {
        this(gvrContext, false, null, 0);
    }

    /**
     * Create a instace of SXRMixedReality component.
     *
     * @param gvrContext
     * @param platform which cv library to use.  0 for arcore. non-zero for sxr
     */
    public SXRMixedReality(final SXRContext gvrContext, int platform) {
        this(gvrContext, false, null, platform);
    }

    /**
     * Create a instace of SXRMixedReality component and specifies the use of cloud anchors.
     *
     * @param gvrContext
     * @param enableCloudAnchor
     */
    public SXRMixedReality(final SXRContext gvrContext, boolean enableCloudAnchor, int platform) {
        this(gvrContext, enableCloudAnchor, null, platform);
    }

    /**
     * Create a instance of SXRMixedReality component and add it to the specified scene.
     *
     * @param gvrContext
     * @param scene
     */
    public SXRMixedReality(final SXRContext gvrContext, SXRScene scene) {
        this(gvrContext, false, scene, 0);
    }

    /**
     * Create a instance of SXRMixedReality component and add it to the specified scene.
     *
     * @param gvrContext
     * @param scene
     * @param platform which cv library to use.  0 for arcore. non-zero for sxr
     */
    public SXRMixedReality(final SXRContext gvrContext, SXRScene scene, int platform) {
        this(gvrContext, false, scene, platform);
    }

    /**
     * Default SXRMixedReality constructor. Create a instace of SXRMixedReality component, set
     * the use of cloud anchors and add it to the specified scened.
     *
     * @param gvrContext
     * @param enableCloudAnchor
     * @param scene
     * @param platform which cv library to use.  0 for arcore. non-zero for sxr
     */
    public SXRMixedReality(SXRContext gvrContext, boolean enableCloudAnchor, SXRScene scene, int platform) {
        super(gvrContext, 0);


        if (scene == null) {
            scene = gvrContext.getMainScene();
        }

        mActivityEventsHandler = new ActivityEventsHandler();
        selectARPlatform(platform, enableCloudAnchor);
        mState = SessionState.ON_PAUSE;

        scene.getMainCameraRig().getOwnerObject().attachComponent(this);
    }

    /**
     * Default SXRMixedReality constructor. Create a instace of SXRMixedReality component, set
     * the use of cloud anchors and add it to the specified scened.
     *
     * @param scene
     * @param platform which cv library to use.  0 for arcore. non-zero for sxr
     * @param enableCloudAnchor
     */
    public SXRMixedReality(SXRScene scene, int platform, boolean enableCloudAnchor) {
        super(scene.getSXRContext(), 0);
        mActivityEventsHandler = new ActivityEventsHandler();
        selectARPlatform(platform, enableCloudAnchor);
        mState = SessionState.ON_PAUSE;

        scene.getMainCameraRig().getOwnerObject().attachComponent(this);
    }

    /**
     * selectARPlatform. select which AR platform to use.
     *
     * @param platform which cv library to use.  0 for arcore. non-zero for sxr
     * @param enableCloudAnchor
     */
    public void selectARPlatform(int platform, boolean enableCloudAnchor)
    {
        String prop = System.getProperty("debug.samsungxr.hmt");
        if(prop != null && prop.equals("AR-DROP-IN2")) {
            platform = 1;
        }

        if (platform != 0)
        {
            mSession = new CVLibrarySession(getSXRContext(), enableCloudAnchor);
        }
        else
        {
            mSession = new ARCoreSession(getSXRContext(), enableCloudAnchor);
        }
    }

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
    public void registerPlaneListener(IPlaneEventsListener listener) {
        mSession.registerPlaneListener(listener);
    }

    @Override
    public void registerAnchorListener(IAnchorEventsListener listener) {
        mSession.registerAnchorListener(listener);
    }

    @Override
    public void registerAugmentedImageListener(IAugmentedImageEventsListener listener) {
        mSession.registerAugmentedImageListener(listener);
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
    public SXRAnchor createAnchor(float[] pose, SXRNode sceneObject) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.createAnchor(pose, sceneObject);
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
    public void hostAnchor(SXRAnchor anchor, ICloudAnchorListener listener) {
        mSession.hostAnchor(anchor, listener);
    }

    @Override
    public void resolveCloudAnchor(String anchorId, ICloudAnchorListener listener) {
        mSession.resolveCloudAnchor(anchorId, listener);
    }

    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor) {
        mSession.setEnableCloudAnchor(enableCloudAnchor);
    }

    @Override
    public SXRHitResult hitTest(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.hitTest(sceneObj, collision);
    }

    @Override
    public SXRLightEstimate getLightEstimate() {
        if (mState == SessionState.ON_PAUSE) {
            throw new UnsupportedOperationException("Session is not resumed");
        }
        return mSession.getLightEstimate();
    }

    @Override
    public void setAugmentedImage(Bitmap image) {
        mSession.setAugmentedImage(image);
    }

    @Override
    public void setAugmentedImages(ArrayList<Bitmap> imagesList) {
        mSession.setAugmentedImages(imagesList);
    }

    @Override
    public ArrayList<SXRAugmentedImage> getAllAugmentedImages() {
        return mSession.getAllAugmentedImages();
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
