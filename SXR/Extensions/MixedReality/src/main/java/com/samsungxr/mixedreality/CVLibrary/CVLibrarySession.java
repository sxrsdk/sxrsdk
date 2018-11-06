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

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;
import android.app.Activity;

//import com.google.ar.core.Anchor;
//import com.google.ar.core.ArCoreApk;
//import com.google.ar.core.AugmentedImage;
//import com.google.ar.core.AugmentedImageDatabase;
//import com.google.ar.core.Camera;
//import com.google.ar.core.Config;
//import com.google.ar.core.Frame;
//import com.google.ar.core.HitResult;
//import com.google.ar.core.Plane;
//import com.google.ar.core.Pose;
//import com.google.ar.core.Session;
//import com.google.ar.core.TrackingState;
//import com.google.ar.core.exceptions.CameraNotAvailableException;
//import com.google.ar.core.exceptions.UnavailableApkTooOldException;
//import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
//import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRAugmentedImage;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRLightEstimate;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.IAnchorEventsListener;
import com.samsungxr.mixedreality.IAugmentedImageEventsListener;
import com.samsungxr.mixedreality.ICloudAnchorListener;
import com.samsungxr.mixedreality.IPlaneEventsListener;
import com.samsungxr.mixedreality.MRCommon;
import com.samsungxr.mixedreality.CameraPermissionHelper;
import com.samsungxr.mixedreality.CVLibrary.CVLibraryHelper;
import com.samsungxr.utility.Log;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.Math.toDegrees;


public class CVLibrarySession extends MRCommon{


    private Context context;

    private static float PASSTHROUGH_DISTANCE = 100.0f;
    private static float AR2VR_SCALE = 100;
    private static Activity sActivity;

    private boolean mInstallRequested;

    private SXRScene mVRScene;
    private SXRNode mARPassThroughObject;
    private ARCoreHandler mARCoreHandler;
    private boolean mEnableCloudAnchor;


    /* From AR to SXR space matrices */
    private float[] mSXRModelMatrix = new float[16];
    private float[] mARViewMatrix = new float[16];
    private float[] mSXRCamMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];

    private Vector3f mDisplayGeometry;

    private CVLibraryHelper mCVLibraryHelper;

    //private final HashMap<Anchor, ICloudAnchorListener> pendingAnchors = new HashMap<>();

    public CVLibrarySession(SXRContext gvrContext, boolean enableCloudAnchor) {
        super(gvrContext);
        mVRScene = gvrContext.getMainScene();
        mCVLibraryHelper = new CVLibraryHelper(gvrContext, mVRScene);
        mEnableCloudAnchor = enableCloudAnchor;

    }


    @Override
    protected void onResume() {

        Log.d(TAG, "onResumeAR");
//
//        if (mSession == null) {
//
//            if (!checkARCoreAndCamera()) {
//                return;
//            }
//
//            // Create default config and check if supported.
//            mConfig = new Config(mSession);
////            if (mEnableCloudAnchor) {
////                mConfig.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
////            }
//            mConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
//            if (!mSession.isSupported(mConfig)) {
//                showSnackbarMessage("This device does not support AR", true);
//            }
//            mSession.configure(mConfig);
//        }
//
//        showLoadingMessage();
//
//        try {
//            mSession.resume();
//        } catch (CameraNotAvailableException e) {
//            e.printStackTrace();
//        }
//
        mGvrContext.runOnGlThread(new Runnable() {
            @Override
            public void run() {
                try {
                    onInitARCoreSession(mGvrContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
//
//        if (mSession != null) {
//            mSession.pause();
//        }
    }

    private void onInitARCoreSession(SXRContext gvrContext)  {
//        SXRTexture passThroughTexture = new SXRExternalTexture(gvrContext);
//
//        mSession.setCameraTextureName(passThroughTexture.getId());

        // FIXME: detect VR screen aspect ratio. Using empirical 16:9 aspect ratio
        /* Try other aspect ration whether virtual objects looks jumping ou sliding
        during camera's rotation.
         */
//        mSession.setDisplayGeometry(Surface.ROTATION_90 , 160, 90);
//
//        mLastARFrame = mSession.update();
//        mDisplayGeometry = configDisplayGeometry(mLastARFrame.getCamera());
//
//        mSession.setDisplayGeometry(Surface.ROTATION_90 ,
//                (int)mDisplayGeometry.x, (int)mDisplayGeometry.y);
//
//        /* To render texture from phone's camera */
//        mARPassThroughObject = new SXRNode(gvrContext, mDisplayGeometry.x, mDisplayGeometry.y,
//                passThroughTexture, SXRMaterial.SXRShaderType.OES.ID);

//        mARPassThroughObject.getRenderData().setRenderingOrder(SXRRenderData.SXRRenderingOrder.BACKGROUND);
//        mARPassThroughObject.getRenderData().setDepthTest(false);
//        mARPassThroughObject.getTransform().setPosition(0, 0, mDisplayGeometry.z);
//        mARPassThroughObject.attachComponent(new SXRMeshCollider(gvrContext, true));
//
//        mVRScene.addNode(mARPassThroughObject);

        /* AR main loop */
        mARCoreHandler = new ARCoreHandler();
        gvrContext.registerDrawFrameListener(mARCoreHandler);

        //mSXRCamMatrix = mVRScene.getMainCameraRig().getHeadTransform().getModelMatrix();

        //updateAR2SXRMatrices(mLastARFrame.getCamera(), mVRScene.getMainCameraRig());
    }


    public class ARCoreHandler implements SXRDrawFrameListener {
        @Override
        public void onDrawFrame(float v) {

//            try {
//                arFrame = mSession.update();
//            } catch (CameraNotAvailableException e) {
//                e.printStackTrace();
//                mGvrContext.unregisterDrawFrameListener(this);
//                return;
//            }

//            Camera arCamera = arFrame.getCamera();
//
//            if (arFrame.getTimestamp() == mLastARFrame.getTimestamp()) {
//                // FIXME: ARCore works at 30fps.
//                return;
//            }
//
//            if (arCamera.getTrackingState() != TrackingState.TRACKING) {
//                // Put passthrough object in from of current VR cam at paused states.
//                updateAR2SXRMatrices(arCamera, mVRScene.getMainCameraRig());
//                updatePassThroughObject(mARPassThroughObject);
//
//                return;
//            }

            // Update current AR cam's view matrix.
            //arCamera.getViewMatrix(mARViewMatrix, 0);

            // Update passthrough object with last VR cam matrix
           // updatePassThroughObject(mARPassThroughObject);

            mCVLibraryHelper.updatePlanes(mARViewMatrix, mSXRCamMatrix, AR2VR_SCALE);

            //mCVLibraryHelper.updateAugmentedImages(arFrame.getUpdatedTrackables(AugmentedImage.class));

            mCVLibraryHelper.updateAnchors(mARViewMatrix, mSXRCamMatrix, AR2VR_SCALE);

//            updateCloudAnchors(arFrame.getUpdatedAnchors());

            //mLastARFrame = arFrame;

            // Update current VR cam's matrix to next update of passtrhough and virtual objects.
            // AR/30fps vs VR/60fps
//            mSXRCamMatrix = mVRScene.getMainCameraRig().getHeadTransform().getModelMatrix();
        }
    }

    @Override
    protected SXRNode onGetPassThroughObject() {
        return mARPassThroughObject;
    }

    @Override
    protected void onRegisterPlaneListener(IPlaneEventsListener listener) {
        mCVLibraryHelper.registerPlaneListener(listener);
    }

    @Override
    protected void onRegisterAnchorListener(IAnchorEventsListener listener) {
        mCVLibraryHelper.registerAnchorListener(listener);
    }

    @Override
    protected void onRegisterAugmentedImageListener(IAugmentedImageEventsListener listener) {
        mCVLibraryHelper.registerAugmentedImageListener(listener);
    }

    @Override
    protected ArrayList<SXRPlane> onGetAllPlanes() {
        return mCVLibraryHelper.getAllPlanes();
    }

    @Override
    protected SXRAnchor onCreateAnchor(float[] pose, SXRNode sceneObject) {
//        float[] translation = new float[3];
//        float[] rotation = new float[4];
//
//        convertMatrixPoseToVector(pose, translation, rotation);
//
//        Anchor anchor = mSession.createAnchor(new Pose(translation, rotation));
        return mCVLibraryHelper.createAnchor(sceneObject);
    }

    @Override
    protected void onUpdateAnchorPose(SXRAnchor anchor, float[] pose) {
//        float[] translation = new float[3];
//        float[] rotation = new float[4];
//
//        convertMatrixPoseToVector(pose, translation, rotation);
//
//        Anchor arAnchor = mSession.createAnchor(new Pose(translation, rotation));
        mCVLibraryHelper.updateAnchorPose((CVLibraryAnchor)anchor);
    }

    @Override
    protected void onRemoveAnchor(SXRAnchor anchor) {
//        mCVLibraryHelper.removeAnchor((ARCoreAnchor)anchor);
        return;
    }

    /**
     * This method hosts an anchor. The {@code listener} will be invoked when the results are
     * available.
     */
    @Override
    synchronized protected void onHostAnchor(SXRAnchor anchor, ICloudAnchorListener listener) {
//        Anchor newAnchor = mSession.hostCloudAnchor(((ARCoreAnchor)anchor).getAnchorAR());
//        pendingAnchors.put(newAnchor, listener);
        return;
    }

    /**
     * This method resolves an anchor. The {@code listener} will be invoked when the results are
     * available.
     */
    synchronized protected void onResolveCloudAnchor(String anchorId, ICloudAnchorListener listener) {
//        Anchor newAnchor = mSession.resolveCloudAnchor(anchorId);
//        pendingAnchors.put(newAnchor, listener);
        return;
    }

    /** Should be called with the updated anchors available after a {@link Session#update()} call. */
//    synchronized void updateCloudAnchors(Collection<Anchor> updatedAnchors) {
//        for (Anchor anchor : updatedAnchors) {
//            if (pendingAnchors.containsKey(anchor)) {
//                Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
////                if (isReturnableState(cloudState)) {
////                    ICloudAnchorListener listener = pendingAnchors.remove(anchor);
////                    SXRAnchor newAnchor = mCVLibraryHelper.createAnchor(anchor, null);
////                    listener.onTaskComplete(newAnchor);
////                }
//            }
//        }
//    }

    /** Used to clear any currently registered listeners, so they wont be called again. */
//    synchronized void clearListeners() {
//        pendingAnchors.clear();
//    }

//    private static boolean isReturnableState(Anchor.CloudAnchorState cloudState) {
//        switch (cloudState) {
//            case NONE:
//            case TASK_IN_PROGRESS:
//                return false;
//            default:
//                return true;
//        }
//    }

    @Override
    protected void onSetEnableCloudAnchor(boolean enableCloudAnchor) {
        mEnableCloudAnchor = enableCloudAnchor;
    }

    @Override
    protected SXRHitResult onHitTest(SXRNode sceneObj, SXRPicker.SXRPickedObject collision) {
//        if (sceneObj != mARPassThroughObject)
//            return null;
//
//        Vector2f tapPosition = convertToDisplayGeometrySpace(collision.getHitLocation());
//        List<HitResult> hitResult = arFrame.hitTest(tapPosition.x, tapPosition.y);
        return mCVLibraryHelper.hitTest();
    }

    @Override
    protected SXRLightEstimate onGetLightEstimate() {
        return mCVLibraryHelper.getLightEstimate();
    }

    @Override
    protected void onSetAugmentedImage(Bitmap image) {
//        ArrayList<Bitmap> imagesList = new ArrayList<>();
//        imagesList.add(image);
//        onSetAugmentedImages(imagesList);
    }

    @Override
    protected void onSetAugmentedImages(ArrayList<Bitmap> imagesList) {
//        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(mSession);
//        for (Bitmap image: imagesList) {
//            augmentedImageDatabase.addImage("image_name", image);
//        }
//
//        mConfig.setAugmentedImageDatabase(augmentedImageDatabase);
//        mSession.configure(mConfig);
    }

    @Override
    protected ArrayList<SXRAugmentedImage> onGetAllAugmentedImages() {
//        return mCVLibraryHelper.getAllAugmentedImages();
        return null;
    }
}