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

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.view.Surface;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.mixedreality.CameraPermissionHelper;
import com.samsungxr.mixedreality.IMarkerEvents;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IMixedRealityEvents;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRLightEstimate;
import com.samsungxr.mixedreality.SXRMixedReality;
import com.samsungxr.mixedreality.SXRPlane;
import com.samsungxr.mixedreality.IAnchorEvents;
import com.samsungxr.mixedreality.SXRPointCloud;
import com.samsungxr.mixedreality.SXRTrackingState;
import com.samsungxr.utility.Log;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ARCoreSession implements IMixedReality
{
    private final SXRContext mContext;
    private static final String TAG = "ARCORE";
    private static float mARtoVRScale = 100.0f;
    protected SXREventReceiver mListeners;
    private Session mSession;
    private boolean mInstallRequested;
    private Config mConfig;
    private SXRScene mVRScene;
    private SXRNode mARPassThroughObject;
    private Frame mLastARFrame;
    private Frame arFrame;
    private boolean mEnableCloudAnchor;
    private Vector2f mScreenToCamera = new Vector2f(1, 1);
    private float[] mSXRCamMatrix = new float[16]; /* From AR to SXR space matrices */
    private Vector3f mDisplayGeometry;
    private float mScreenDepth;
    private Map<Plane, ARCorePlane> mArPlanes;
    private Map<AugmentedImage, ARCoreMarker> mArAugmentedImages;
    private List<ARCoreAnchor> mArAnchors;

    private Camera mCamera;// ARCore camera
    private final Map<Anchor, CloudAnchorCallback> pendingAnchors = new HashMap<>();

    public ARCoreSession(SXRScene scene, boolean enableCloudAnchor)
    {
        mContext = scene.getSXRContext();
        mListeners = new SXREventReceiver(this);
        mSession = null;
        mLastARFrame = null;
        mVRScene = scene;
        mArPlanes = new HashMap<>();
        mArAugmentedImages = new HashMap<>();
        mArAnchors = new ArrayList<>();
        mEnableCloudAnchor = enableCloudAnchor;
    }

    public SXRContext getSXRContext() { return mContext; }

    @Override
    public SXREventReceiver getEventReceiver() { return mListeners; }

    @Override
    public float getARToVRScale() { return mARtoVRScale; }

    @Override
    public void setARToVRScale(float scale) { mARtoVRScale = scale; }

    @Override
    public float getScreenDepth()
    {
        return mScreenDepth;
    }

    @Override
    public void resume()
    {
        if (mSession == null)
        {
            if (!checkARCoreAndCamera())
            {
                return;
            }
            // Create default config and check if supported.
            mConfig = new Config(mSession);
            if (mEnableCloudAnchor) {
                mConfig.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
            }
            mConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            ArCoreApk arCoreApk = ArCoreApk.getInstance();
            ArCoreApk.Availability availability = arCoreApk.checkAvailability(mContext.getContext());
            if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                showSnackbarMessage("This device does not support AR", true);
            }
            mSession.configure(mConfig);
        }
        showLoadingMessage();
        try
        {
            mSession.resume();
        }
        catch (CameraNotAvailableException e)
        {
            e.printStackTrace();
        }

        mContext.runOnGlThread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    onInitARCoreSession(mContext);
                }
                catch (CameraNotAvailableException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void pause() {
        Log.d(TAG, "onPause");

        if (mSession != null) {
            mSession.pause();
        }
    }


    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor)
    {
        mEnableCloudAnchor = enableCloudAnchor;
    }

    @Override
    public SXRHitResult hitTest(SXRPicker.SXRPickedObject collision)
    {
        Vector2f tapPosition = convertToDisplayGeometrySpace(collision.hitLocation[0], collision.hitLocation[1]);
        List<HitResult> hitResult = mLastARFrame.hitTest(tapPosition.x, tapPosition.y);
        return hitTest(hitResult);
    }

    @Override
    public SXRHitResult hitTest(float x, float y)
    {
        x *= mScreenToCamera.x;
        y *= mScreenToCamera.y;
        List<HitResult> hitResult = mLastARFrame.hitTest(x, y);
        return hitTest(hitResult);
    }

    @Override
    public SXRLightEstimate getLightEstimate()
    {
        if (mLastARFrame != null)
        {
            LightEstimate lightEstimate = mLastARFrame.getLightEstimate();
            ARCoreLightEstimate arCoreLightEstimate = new ARCoreLightEstimate();
            SXRLightEstimate.SXRLightEstimateState state;

            arCoreLightEstimate.setPixelIntensity(lightEstimate.getPixelIntensity());
            state = (lightEstimate.getState() == LightEstimate.State.VALID) ?
                SXRLightEstimate.SXRLightEstimateState.VALID :
                SXRLightEstimate.SXRLightEstimateState.NOT_VALID;
            arCoreLightEstimate.setState(state);

            return arCoreLightEstimate;
        }
        return null;
    }

    @Override
    public void setMarker(Bitmap image)
    {
        ArrayList<Bitmap> imagesList = new ArrayList<>();
        imagesList.add(image);
        setMarkers(imagesList);
    }

    @Override
    public void setMarkers(ArrayList<Bitmap> imagesList)
    {
        AugmentedImageDatabase augmentedImageDatabase = new AugmentedImageDatabase(mSession);
        for (Bitmap image : imagesList)
        {
            augmentedImageDatabase.addImage("image_name", image);
        }
        mConfig.setAugmentedImageDatabase(augmentedImageDatabase);
        mSession.configure(mConfig);
    }

    @Override
    public void updateAnchorPose(SXRAnchor anchor, float[] pose)
    {
        final float[] translation = new float[3];
        final float[] rotation = new float[4];
        final float[] arPose = pose.clone();
        ARCoreAnchor coreAnchor = (ARCoreAnchor) anchor;

        convertMatrixPoseToVector(arPose, translation, rotation);

        Anchor arAnchor = mSession.createAnchor(new Pose(translation, rotation));
        if (coreAnchor.getAnchorAR() != null)
        {
            coreAnchor.getAnchorAR().detach();
        }
        coreAnchor.setAnchorAR(arAnchor);
    }

    @Override
    public float[] makeInterpolated(float[] poseA, float[] poseB, float t)
    {
        float[] translation = new float[3];
        float[] rotation = new float[4];
        float[] newMatrixPose = new float[16];

        convertMatrixPoseToVector(poseA, translation, rotation);
        Pose ARPoseA = new Pose(translation, rotation);

        convertMatrixPoseToVector(poseB, translation, rotation);
        Pose ARPoseB = new Pose(translation, rotation);

        Pose newPose = Pose.makeInterpolated(ARPoseA, ARPoseB, t);
        newPose.toMatrix(newMatrixPose, 0);

        return newMatrixPose;
    }

    @Override
    public SXRPointCloud acquirePointCloud()
    {
        ARCorePointCloud pointCloud = new ARCorePointCloud(mARtoVRScale);
        pointCloud.setARPointCloud(mLastARFrame.acquirePointCloud());

        return pointCloud;
    }

    @Override
    public void setPlaneFindingMode(SXRMixedReality.PlaneFindingMode mode)
    {
        if (mConfig == null)
        {
            return;
        }
        switch (mode)
        {
            case HORIZONTAL:
                mConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
                break;

            case HORIZONTAL_AND_VERTICAL:
                mConfig.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
                break;

            case VERTICAL:
                mConfig.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
                break;

            case DISABLED:
            default:
                mConfig.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        }
        mSession.configure(mConfig);
    }

    public void updatePlanes(Collection<Plane> allPlanes)
    {
        // Don't update planes (or notify) when the plane listener is empty, i.e., there is
        // no listener registered.
        float scale = mARtoVRScale;
        ARCorePlane arCorePlane;
        for (Plane plane: allPlanes)
        {
            if (plane.getTrackingState() != TrackingState.TRACKING
                || mArPlanes.containsKey(plane))
            {
                continue;
            }
            arCorePlane = new ARCorePlane(this, plane);
            mArPlanes.put(plane, arCorePlane);
            arCorePlane.update();
            notifyPlaneDetectionListeners(arCorePlane);
        }
        for (Plane plane: mArPlanes.keySet())
        {
            arCorePlane = mArPlanes.get(plane);

            if (plane.getTrackingState() == TrackingState.TRACKING &&
                arCorePlane.getTrackingState() != SXRTrackingState.TRACKING)
            {
                arCorePlane.setTrackingState(SXRTrackingState.TRACKING);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.TRACKING);
            }
            else if (plane.getTrackingState() == TrackingState.PAUSED &&
                arCorePlane.getTrackingState() != SXRTrackingState.PAUSED)
            {
                arCorePlane.setTrackingState(SXRTrackingState.PAUSED);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.PAUSED);
            }
            else if (plane.getTrackingState() == TrackingState.STOPPED &&
                arCorePlane.getTrackingState() != SXRTrackingState.STOPPED)
            {
                arCorePlane.setTrackingState(SXRTrackingState.STOPPED);
                notifyPlaneStateChangeListeners(arCorePlane, SXRTrackingState.STOPPED);
            }

            if (plane.getSubsumedBy() != null && arCorePlane.getParentPlane() == null)
            {
                arCorePlane.setParentPlane(mArPlanes.get(plane.getSubsumedBy()));
                notifyMergedPlane(arCorePlane, arCorePlane.getParentPlane());
            }
            arCorePlane.update();
            if (arCorePlane.geometryChange())
            {
                notifyPlaneGeometryChange(arCorePlane);
            }
        }
    }

    public void updateMarkers(Collection<AugmentedImage> allAugmentedImages)
    {
        ARCoreMarker arCoreMarker;

        for (AugmentedImage augmentedImage: allAugmentedImages)
        {
            if (augmentedImage.getTrackingState() != TrackingState.TRACKING
                || mArAugmentedImages.containsKey(augmentedImage))
            {
                continue;
            }

            arCoreMarker = new ARCoreMarker(this, augmentedImage);
            notifyMarkerDetectionListeners(arCoreMarker);
            mArAugmentedImages.put(augmentedImage, arCoreMarker);
        }

        for (AugmentedImage augmentedImage: mArAugmentedImages.keySet())
        {
            arCoreMarker = mArAugmentedImages.get(augmentedImage);

            if (augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                arCoreMarker.getTrackingState() != SXRTrackingState.TRACKING)
            {
                arCoreMarker.setTrackingState(SXRTrackingState.TRACKING);
                notifyMarkerStateChangeListeners(arCoreMarker, SXRTrackingState.TRACKING);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.PAUSED &&
                arCoreMarker.getTrackingState() != SXRTrackingState.PAUSED)
            {
                arCoreMarker.setTrackingState(SXRTrackingState.PAUSED);
                notifyMarkerStateChangeListeners(arCoreMarker, SXRTrackingState.PAUSED);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.STOPPED &&
                arCoreMarker.getTrackingState() != SXRTrackingState.STOPPED)
            {
                arCoreMarker.setTrackingState(SXRTrackingState.STOPPED);
                notifyMarkerStateChangeListeners(arCoreMarker, SXRTrackingState.STOPPED);
            }
        }
    }

    public void updateAnchors()
    {
        for (ARCoreAnchor anchor: mArAnchors)
        {
            Anchor arAnchor = anchor.getAnchorAR();

            if (arAnchor.getTrackingState() == TrackingState.TRACKING &&
                anchor.getTrackingState() != SXRTrackingState.TRACKING)
            {
                anchor.setTrackingState(SXRTrackingState.TRACKING);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.TRACKING);
            }
            else if (arAnchor.getTrackingState() == TrackingState.PAUSED &&
                anchor.getTrackingState() != SXRTrackingState.PAUSED)
            {
                anchor.setTrackingState(SXRTrackingState.PAUSED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.PAUSED);
            }
            else if (arAnchor.getTrackingState() == TrackingState.STOPPED &&
                anchor.getTrackingState() != SXRTrackingState.STOPPED)
            {
                anchor.setTrackingState(SXRTrackingState.STOPPED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.STOPPED);
            }
            anchor.update();
        }
    }

    @Override
    public ArrayList<SXRMarker> getAllMarkers()
    {
        ArrayList<SXRMarker> allAugmentedImages = new ArrayList<>();

        for (AugmentedImage augmentedImage: mArAugmentedImages.keySet())
        {
            allAugmentedImages.add(mArAugmentedImages.get(augmentedImage));
        }

        return allAugmentedImages;
    }

    @Override
    public SXRAnchor createAnchor(float[] pose, SXRNode owner)
    {
        Pose arpose = makePose(pose);
        Anchor anchor = mSession.createAnchor(arpose);
        return addAnchor(anchor, owner);
    }

    @Override
    public void removeAnchor(SXRAnchor anchor)
    {
        ARCoreAnchor arAnchor = (ARCoreAnchor) anchor;
        arAnchor.detach();
        mArAnchors.remove(anchor);
        SXRNode anchorNode = anchor.getOwnerObject();
        anchorNode.detachComponent(SXRAnchor.getComponentType());
    }

    private boolean checkARCoreAndCamera()
    {
        Activity activity = mContext.getApplication().getActivity();
        Exception exception = null;
        String message = null;
        try {
            switch (ArCoreApk.getInstance().requestInstall(activity, !mInstallRequested))
            {
                case INSTALL_REQUESTED:
                    mInstallRequested = true;
                    return false;
                case INSTALLED:
                    break;
            }

            // ARCore requires camera permissions to operate. If we did not yet obtain runtime
            // permission on Android M and above, now is a good time to ask the user for it.
            if (!CameraPermissionHelper.hasCameraPermission(activity))
            {
                CameraPermissionHelper.requestCameraPermission(activity);
                return false;
            }

            mSession = new Session(/* context= */ activity);
        }
        catch (UnavailableArcoreNotInstalledException |
            UnavailableUserDeclinedInstallationException e)
        {
            message = "Please install ARCore";
            exception = e;
        }
        catch (UnavailableApkTooOldException e)
        {
            message = "Please update ARCore";
            exception = e;
        }
        catch (UnavailableSdkTooOldException e)
        {
            message = "Please update this app";
            exception = e;
        }
        catch (Exception e)
        {
            message = "This device does not support AR";
            exception = e;
        }
        if (message != null)
        {
            showSnackbarMessage(message, true);
            android.util.Log.e(TAG, "Exception creating session", exception);
            mContext.getEventManager().sendEvent(this, IMixedRealityEvents.class, "onMixedRealityError", message);
            return false;
        }

        return true;
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss)
    {
        Log.d(TAG, message);
        if (finishOnDismiss)
        {
            //FIXME: finish();
        }
    }

    private void showLoadingMessage() {
        showSnackbarMessage("Searching for surfaces...", false);
    }

    private void onInitARCoreSession(SXRContext gvrContext) throws CameraNotAvailableException
    {
        SXRTexture passThroughTexture = new SXRExternalTexture(gvrContext);

        mSession.setCameraTextureName(passThroughTexture.getId());

        configDisplayAspectRatio(mContext.getActivity());

        mLastARFrame = mSession.update();
        final SXRCameraRig cameraRig = mVRScene.getMainCameraRig();

        mDisplayGeometry = configDisplayGeometry(mLastARFrame.getCamera(), cameraRig);
        mSession.setDisplayGeometry(Surface.ROTATION_90,
                                    (int) mDisplayGeometry.x, (int) mDisplayGeometry.y);

        final SXRMesh mesh = SXRMesh.createQuad(mContext, "float3 a_position float2 a_texcoord",
                                                mDisplayGeometry.x, mDisplayGeometry.y);

        final FloatBuffer texCoords = mesh.getTexCoordsAsFloatBuffer();
        final int capacity = texCoords.capacity();
        final int FLOAT_SIZE = 4;

        ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(capacity * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());

        FloatBuffer quadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();

        mLastARFrame.transformDisplayUvCoords(texCoords, quadTexCoordTransformed);

        float[] uv = new float[capacity];
        quadTexCoordTransformed.get(uv);

        mesh.setTexCoords(uv);

        /* To render texture from phone's camera */
        mARPassThroughObject = new SXRNode(gvrContext, mesh,
                                           passThroughTexture, SXRMaterial.SXRShaderType.OES.ID);

        mARPassThroughObject.getRenderData().setRenderingOrder(SXRRenderData.SXRRenderingOrder.BACKGROUND);
        mARPassThroughObject.getRenderData().setDepthTest(false);
        mARPassThroughObject.getRenderData().setCastShadows(false);
        mARPassThroughObject.getTransform().setPosition(0, 0, mDisplayGeometry.z);
        mARPassThroughObject.attachComponent(new SXRMeshCollider(gvrContext, true));
        mARPassThroughObject.setName("ARPassThrough");
        mVRScene.getMainCameraRig().addChildObject(mARPassThroughObject);

        /* AR main loop */

        gvrContext.registerDrawFrameListener(mOnDrawFrame);
        syncARCamToVRCam(mLastARFrame.getCamera(), cameraRig);
        gvrContext.getEventManager().sendEvent(this,
                                               IMixedRealityEvents.class,
                                               "onMixedRealityStart",
                                               this);
    }

    private SXRHitResult hitTest(List<HitResult> hitResult)
    {
        for (HitResult hit : hitResult)
        {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            Trackable trackable = hit.getTrackable();
            // Creates an anchor if a plane or an oriented point was hit.
            if ((trackable instanceof Plane &&
                ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) &&
                ((Plane) trackable).getSubsumedBy() == null)
            {
                SXRHitResult gvrHitResult = new SXRHitResult();
                float[] hitPose = new float[16];

                hit.getHitPose().toMatrix(hitPose, 0);
//                ar2gvr(hitPose);
                gvrHitResult.setPose(hitPose);
                gvrHitResult.setDistance(hit.getDistance() * mARtoVRScale);
                gvrHitResult.setPlane(mArPlanes.get(trackable));
                return gvrHitResult;
            }
        }
        return null;
    }

    private SXRDrawFrameListener mOnDrawFrame = new  SXRDrawFrameListener ()
    {
        @Override
        public void onDrawFrame(float v)
        {
            try
            {
                arFrame = mSession.update();
            }
            catch (CameraNotAvailableException e)
            {
                e.printStackTrace();
                mContext.unregisterDrawFrameListener(this);
                mContext.getEventManager().sendEvent(ARCoreSession.this,
                                                     IMixedRealityEvents.class,
                                                     "onMixedRealityError",
                                                     ARCoreSession.this,
                                                     e.getMessage());

                mContext.getEventManager().sendEvent(ARCoreSession.this,
                                                     IMixedRealityEvents.class,
                                                     "onMixedRealityStop",
                                                     ARCoreSession.this);
                return;
            }
            if (arFrame.getTimestamp() == mLastARFrame.getTimestamp())
            {
                // FIXME: ARCore works at 30fps.
                return;
            }
            Camera arCamera = arFrame.getCamera();
            syncARCamToVRCam(arCamera, mVRScene.getMainCameraRig());
            if (arCamera.getTrackingState() != TrackingState.TRACKING)
            {
                return;
            }
            updatePlanes(mSession.getAllTrackables(Plane.class));
            updateMarkers(arFrame.getUpdatedTrackables(AugmentedImage.class));
            updateAnchors();
            updateCloudAnchors(arFrame.getUpdatedAnchors());
            mLastARFrame = arFrame;
            mContext.getEventManager().sendEvent(ARCoreSession.this,
                                                 IMixedRealityEvents.class,
                                                 "onMixedRealityUpdate",
                                                 ARCoreSession.this);
        }
    };

    private void syncARCamToVRCam(Camera arCamera, SXRCameraRig cameraRig)
    {
        float x = mSXRCamMatrix[12];
        float y = mSXRCamMatrix[13];
        float z = mSXRCamMatrix[14];

        arCamera.getDisplayOrientedPose().toMatrix(mSXRCamMatrix, 0);

        // FIXME: This is a workaround because the AR camera's pose is changing its
        // position values even if it is stopped! To avoid the scene looks trembling
        mSXRCamMatrix[12] = (mSXRCamMatrix[12] * mARtoVRScale + x) * 0.5f;
        mSXRCamMatrix[13] = (mSXRCamMatrix[13] * mARtoVRScale + y) * 0.5f;
        mSXRCamMatrix[14] = (mSXRCamMatrix[14] * mARtoVRScale + z) * 0.5f;
        cameraRig.getTransform().setModelMatrix(mSXRCamMatrix);
    }

    private void configDisplayAspectRatio(Activity activity)
    {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        mScreenToCamera.x = metrics.widthPixels;
        mScreenToCamera.y = metrics.heightPixels;
        mSession.setDisplayGeometry(Surface.ROTATION_90, metrics.widthPixels, metrics.heightPixels);
    }

    private Vector3f configDisplayGeometry(Camera arCamera, SXRCameraRig cameraRig)
    {
        SXRPerspectiveCamera centerCamera = cameraRig.getCenterCamera();
        float near = centerCamera.getNearClippingDistance();
        float far = centerCamera.getFarClippingDistance();

        // Get phones' cam projection matrix.
        float[] m = new float[16];
        arCamera.getProjectionMatrix(m, 0, near, far);
        Matrix4f projmtx = new Matrix4f();
        projmtx.set(m);

        float aspectRatio = projmtx.m11() / projmtx.m00();
        float arCamFOV = projmtx.perspectiveFov();
        float tanfov =  (float) Math.tan(arCamFOV * 0.5f);
        float quadDistance = far - 1;
        float quadHeight = quadDistance * tanfov * 2;
        float quadWidth = quadHeight * aspectRatio;

        // Use the same fov from AR to VR Camera as default value.
        float vrFov = (float) Math.toDegrees(arCamFOV);
        setVRCameraFov(cameraRig, vrFov);

        // VR Camera will be updated by AR pose, not by internal sensors.
        cameraRig.getHeadTransform().setRotation(1, 0, 0, 0);
        cameraRig.setCameraRigType(SXRCameraRig.SXRCameraRigType.Freeze.ID);

        android.util.Log.d(TAG, "ARCore configured to: passthrough[w: "
                + quadWidth + ", h: " + quadHeight +", z: " + quadDistance
                + "], cam fov: " +vrFov + ", aspect ratio: " + aspectRatio);
        mScreenToCamera.x = quadWidth / mScreenToCamera.x;    // map [0, ScreenSize] to [-Display, +Display]
        mScreenToCamera.y = quadHeight / mScreenToCamera.y;
        mScreenDepth = quadHeight / tanfov;
        return new Vector3f(quadWidth, quadHeight, -quadDistance);
    }

    private static void setVRCameraFov(SXRCameraRig camRig, float degreesFov)
    {
        camRig.getCenterCamera().setFovY(degreesFov);
        ((SXRPerspectiveCamera)camRig.getLeftCamera()).setFovY(degreesFov);
        ((SXRPerspectiveCamera)camRig.getRightCamera()).setFovY(degreesFov);
    }

    @Override
    public SXRNode getPassThroughObject() {
        return mARPassThroughObject;
    }

    @Override
    public ArrayList<SXRPlane> getAllPlanes()
    {
        ArrayList<SXRPlane> allPlanes = new ArrayList<>();

        for (Plane plane: mArPlanes.keySet())
        {
            allPlanes.add(mArPlanes.get(plane));
        }
        return allPlanes;
    }

    /**
     * This method hosts an anchor. The {@code listener} will be invoked when the results are
     * available.
     */
    @Override
    synchronized public void hostAnchor(SXRAnchor anchor, CloudAnchorCallback cb)
    {
        Anchor newAnchor = mSession.hostCloudAnchor(((ARCoreAnchor) anchor).getAnchorAR());
        pendingAnchors.put(newAnchor, cb);
    }

    /**
     * This method resolves an anchor. The {@link IAnchorEvents} will be invoked when the results are
     * available.
     */
    synchronized public void resolveCloudAnchor(String anchorId, CloudAnchorCallback cb)
    {
        Anchor newAnchor = mSession.resolveCloudAnchor(anchorId);
        pendingAnchors.put(newAnchor, cb);
    }

    /**
     * Should be called with the updated anchors available after a {@link Session#update()} call.
     */
    synchronized void updateCloudAnchors(Collection<Anchor> updatedAnchors)
    {
        for (Anchor anchor : updatedAnchors)
        {
            if (pendingAnchors.containsKey(anchor))
            {
                Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
                if (isReturnableState(cloudState))
                {
                    CloudAnchorCallback cb = pendingAnchors.get(anchor);
                    pendingAnchors.remove(anchor);
                    cb.onCloudUpdate(addAnchor(anchor, null));
                }
            }
        }
    }

    SXRAnchor addAnchor(Anchor anchor, SXRNode owner)
    {
        ARCoreAnchor arCoreAnchor = new ARCoreAnchor(this);

        if (owner != null)
        {
            owner.attachComponent(arCoreAnchor);
        }
        arCoreAnchor.setAnchorAR(anchor);
        mArAnchors.add(arCoreAnchor);
        arCoreAnchor.update();
        return arCoreAnchor;
    }

    Pose makePose(final float[] pose)
    {
        final float[] translation = new float[3];
        final float[] rotation = new float[4];

        convertMatrixPoseToVector(pose, translation, rotation);
        return new Pose(translation, rotation);
    }


    /**
     * Used to clear any currently registered listeners, so they wont be called again.
     */
    synchronized void clearListeners()
    {
        pendingAnchors.clear();
    }

    private static boolean isReturnableState(Anchor.CloudAnchorState cloudState)
    {
        switch (cloudState)
        {
            case NONE:
            case TASK_IN_PROGRESS: return false;
            default: return true;
        }
    }

    private void notifyPlaneDetectionListeners(SXRPlane plane) {
        mContext.getEventManager().sendEvent(this,
                                             IPlaneEvents.class,
                                             "onPlaneDetected",
                                             plane);
    }

    private void notifyPlaneStateChangeListeners(SXRPlane plane, SXRTrackingState trackingState) {
        mContext.getEventManager().sendEvent(this,
                                             IPlaneEvents.class,
                                             "onPlaneStateChange",
                                             plane,
                                             trackingState);
    }

    private void notifyMergedPlane(SXRPlane childPlane, SXRPlane parentPlane) {
        mContext.getEventManager().sendEvent(this,
                                             IPlaneEvents.class,
                                             "onPlaneMerging",
                                             childPlane,
                                             parentPlane);
    }

    private void notifyPlaneGeometryChange(SXRPlane plane) {
        mContext.getEventManager().sendEvent(this,
                                             IPlaneEvents.class,
                                             "onPlaneGeometryChange",
                                             plane);
    }


    private void notifyAnchorStateChangeListeners(SXRAnchor anchor, SXRTrackingState trackingState) {
        mContext.getEventManager().sendEvent(this,
                                             IAnchorEvents.class,
                                             "onAnchorStateChange",
                                             anchor,
                                             trackingState);
    }

    private void notifyMarkerDetectionListeners(SXRMarker image) {
        mContext.getEventManager().sendEvent(this,
                                             IMarkerEvents.class,
                                             "onMarkerDetected",
                                             image);
    }

    private void notifyMarkerStateChangeListeners(SXRMarker image, SXRTrackingState trackingState) {
        mContext.getEventManager().sendEvent(this,
                                             IMarkerEvents.class,
                                             "onMarkerStateChange",
                                             image,
                                             trackingState);
    }

    private Vector2f convertToDisplayGeometrySpace(float x, float y)
    {
        final float hitX = x + 0.5f * mDisplayGeometry.x;
        final float hitY = 0.5f * mDisplayGeometry.y - y;

        return new Vector2f(hitX, hitY);
    }

    /**
     * Converts from AR world space to SXRf world space.
     */
    void ar2gvr(float[] poseMatrix) {
        poseMatrix[12] *= mARtoVRScale;
        poseMatrix[13] *= mARtoVRScale;
        poseMatrix[14] *= mARtoVRScale;
    }

    void gvr2ar(float[] transformModelMatrix)
    {
        float sf = 1 / mARtoVRScale;
        transformModelMatrix[12] *= sf;
        transformModelMatrix[13] *= sf;
        transformModelMatrix[14] *= sf;
    }

    static void convertMatrixPoseToVector(final float[] pose, float[] translation, float[] rotation)
    {
        Vector3f vectorTranslation = new Vector3f();
        Quaternionf quaternionRotation = new Quaternionf();
        Matrix4f matrixPose = new Matrix4f();

        matrixPose.set(pose);
        matrixPose.getTranslation(vectorTranslation);
        translation[0] = vectorTranslation.x;
        translation[1] = vectorTranslation.y;
        translation[2] = vectorTranslation.z;
        matrixPose.getNormalizedRotation(quaternionRotation);
        quaternionRotation.normalize();
        rotation[0] = quaternionRotation.x;
        rotation[1] = quaternionRotation.y;
        rotation[2] = quaternionRotation.z;
        rotation[3] = quaternionRotation.w;
    }
}
