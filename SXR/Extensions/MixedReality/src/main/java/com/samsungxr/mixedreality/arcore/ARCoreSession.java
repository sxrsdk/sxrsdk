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
import android.media.Image;
import android.util.DisplayMetrics;
import android.view.Surface;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import com.samsungxr.SXRCamera;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRExternalImage;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRTexture;
import com.samsungxr.mixedreality.IAnchorEvents;
import com.samsungxr.mixedreality.IMarkerEvents;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IMixedRealityEvents;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRLightEstimate;
import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRMixedReality;
import com.samsungxr.mixedreality.SXRPlane;
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
    private static final String TAG = "ARCORE"; // "ARCORE";
    private static float mARtoVRScale = 1.0f;
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
    private Vector3f mDisplayGeometry = new Vector3f();
    private float mScreenWidth;
    private float mScreenHeight;
    private float mScreenDepth;
    private Map<Plane, ARCorePlane> mPlanes;
    private ArrayList<SXRMarker> mMarkers;
    private List<ARCoreAnchor> mAnchors;
    private boolean mIsMono;
    private AugmentedImageDatabase mMarkerDB;
    private boolean mIsRunning = false;

    private Camera mCamera;// ARCore camera
    private final Map<Anchor, CloudAnchorCallback> pendingAnchors = new HashMap<>();

    public ARCoreSession(SXRScene scene, boolean enableCloudAnchor)
    {
        mContext = scene.getSXRContext();
        mListeners = new SXREventReceiver(this);
        mSession = null;
        mLastARFrame = null;
        mVRScene = scene;
        mPlanes = new HashMap<>();
        mMarkers = new ArrayList<>();
        mAnchors = new ArrayList<>();
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
            if (mEnableCloudAnchor)
            {
                mConfig.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
            }
            mConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            ArCoreApk arCoreApk = ArCoreApk.getInstance();
            ArCoreApk.Availability availability = arCoreApk.checkAvailability(mContext.getContext());
            if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE)
            {
                showSnackbarMessage("This device does not support AR", true);
            }
            mMarkerDB = new AugmentedImageDatabase(mSession);
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
                    mIsRunning = true;
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
        mIsRunning = false;
    }

    @Override
    public boolean isPaused()
    {
        return !mIsRunning;
    }


    @Override
    public void setEnableCloudAnchor(boolean enableCloudAnchor)
    {
        mEnableCloudAnchor = enableCloudAnchor;
    }

    @Override
    public SXRHitResult hitTest(SXRPicker.SXRPickedObject collision)
    {
        float x;
        float y
                ;
        if (collision.barycentricCoords != null)
        {
            x = collision.barycentricCoords[0];
            y = collision.barycentricCoords[1];
            x *= mScreenWidth;
            y *= mScreenHeight;
        }
        else
        {
            x = collision.hitLocation[0] / mDisplayGeometry.x;
            y = collision.hitLocation[1] / mDisplayGeometry.y;
            x = (x + 0.5f) * mScreenWidth;
            y = (0.5f - y) * mScreenHeight;
        }
        List<HitResult> hitResult = mLastARFrame.hitTest(x, y);
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
    public void addMarker(String name, Bitmap image)
    {
        mMarkerDB.addImage(name, image);
        mConfig.setAugmentedImageDatabase(mMarkerDB);
        mSession.configure(mConfig);
    }

    @Override
    public void addMarkers(Map<String, Bitmap> imagesList)
    {
        for (Map.Entry<String, Bitmap> entry : imagesList.entrySet())
        {
            mMarkerDB.addImage(entry.getKey(), entry.getValue());
        }
        mConfig.setAugmentedImageDatabase(mMarkerDB);
        mSession.configure(mConfig);
    }

    @Override
    public void updateAnchorPose(SXRAnchor anchor, float[] pose)
    {
        final float[] translation = new float[3];
        final float[] rotation = new float[4];
        final float[] arPose = pose.clone();
        ARCoreAnchor coreAnchor = (ARCoreAnchor) anchor;

        gvr2ar(arPose);
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
    public Image acquireCameraImage() throws NotYetAvailableException {
        return mLastARFrame.acquireCameraImage();
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
        ARCorePlane arCorePlane;
        for (Plane plane: allPlanes)
        {
            if (plane.getTrackingState() != TrackingState.TRACKING
                || mPlanes.containsKey(plane))
            {
                continue;
            }
            arCorePlane = new ARCorePlane(this, plane);
            mPlanes.put(plane, arCorePlane);
            arCorePlane.update();
            notifyPlaneDetectionListeners(arCorePlane);
        }
        for (Plane plane: mPlanes.keySet())
        {
            arCorePlane = mPlanes.get(plane);

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
                arCorePlane.setParentPlane(mPlanes.get(plane.getSubsumedBy()));
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
        /*
         * Make markers for newly added images.
         */
        for (AugmentedImage augmentedImage: allAugmentedImages)
        {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING)
            {
                boolean found = false;
                for (SXRMarker marker : mMarkers)
                {
                    if (((ARCoreMarker) marker).getImage() == augmentedImage)
                    {
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    ARCoreMarker marker = new ARCoreMarker(this, augmentedImage);
                    mMarkers.add(marker);
                    notifyMarkerDetectionListeners(marker);
                }
            }
        }
        for (SXRMarker marker : mMarkers)
        {
            ARCoreMarker am = (ARCoreMarker) marker;
            AugmentedImage augmentedImage = am.getImage();

            if (augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                marker.getTrackingState() != SXRTrackingState.TRACKING)
            {
                am.setTrackingState(SXRTrackingState.TRACKING);
                notifyMarkerStateChangeListeners(marker, SXRTrackingState.TRACKING);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.PAUSED &&
                     marker.getTrackingState() != SXRTrackingState.PAUSED)
            {
                am.setTrackingState(SXRTrackingState.PAUSED);
                notifyMarkerStateChangeListeners(marker, SXRTrackingState.PAUSED);
            }
            else if (augmentedImage.getTrackingState() == TrackingState.STOPPED &&
                     marker.getTrackingState() != SXRTrackingState.STOPPED)
            {
                am.setTrackingState(SXRTrackingState.STOPPED);
                notifyMarkerStateChangeListeners(marker, SXRTrackingState.STOPPED);
            }
        }
    }

    public void updateAnchors()
    {
        for (ARCoreAnchor anchor: mAnchors)
        {
            Anchor arAnchor = anchor.getAnchorAR();

            if (arAnchor.getTrackingState() == TrackingState.TRACKING)
            {
                anchor.update();
                if (anchor.getTrackingState() != SXRTrackingState.TRACKING)
                {
                    anchor.setTrackingState(SXRTrackingState.TRACKING);
                    notifyAnchorStateChangeListeners(anchor, SXRTrackingState.TRACKING);
                }
            }
            else if ((arAnchor.getTrackingState() == TrackingState.PAUSED) &&
                     (anchor.getTrackingState() != SXRTrackingState.PAUSED))
            {
                anchor.setTrackingState(SXRTrackingState.PAUSED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.PAUSED);
            }
            else if ((arAnchor.getTrackingState() == TrackingState.STOPPED) &&
                     (anchor.getTrackingState() != SXRTrackingState.STOPPED))
            {
                anchor.setTrackingState(SXRTrackingState.STOPPED);
                notifyAnchorStateChangeListeners(anchor, SXRTrackingState.STOPPED);
            }
        }
    }

    @Override
    public final ArrayList<SXRMarker> getAllMarkers()
    {
        return mMarkers;
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
        mAnchors.remove(anchor);
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
        final SXRTexture passThroughTexture = new SXRTexture(new SXRExternalImage(gvrContext));
        final SXRCameraRig cameraRig = mVRScene.getMainCameraRig();
        final SXRPerspectiveCamera centerCam = cameraRig.getCenterCamera();
        final SXRMesh mesh;
        final float aspect = centerCam.getAspectRatio();

        mIsMono = Math.abs(1.0f - aspect) > 0.0001f;
        mSession.setCameraTextureName(passThroughTexture.getId());

        configDisplayAspectRatio(mContext.getActivity());

        mLastARFrame = mSession.update();
        if (mIsMono)
        {
            mesh = configMonoDisplay(mLastARFrame, cameraRig);
        }
        else
        {
            mesh = configVRDisplay(mLastARFrame, cameraRig);
        }
        float[] oldTexCoords = mesh.getTexCoords();
        float[] newTexCoords = new float[oldTexCoords.length];

        mLastARFrame.transformCoordinates2d(Coordinates2d.VIEW_NORMALIZED, oldTexCoords,
                                            Coordinates2d.TEXTURE_NORMALIZED, newTexCoords);
        mesh.setTexCoords(newTexCoords);

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
            float[] hitPoseMtx = new float[16];
            Pose hitPose = hit.getHitPose();
            Trackable trackable = hit.getTrackable();

            hitPose.toMatrix(hitPoseMtx, 0);
            Log.d(TAG, "ARCORE hit %f, %f, %f", hitPoseMtx[12], hitPoseMtx[13], hitPoseMtx[14]);
            ar2gvr(hitPoseMtx);
            // Check if any plane was hit, and if it was hit inside the plane polygon
            // Creates an anchor if a plane or an oriented point was hit.
            if (trackable instanceof Plane)
            {
                Plane plane = (Plane) trackable;
                if ((plane.getSubsumedBy() == null) && plane.isPoseInPolygon(hitPose))
                {
                    SXRHitResult gvrHitResult = new SXRHitResult();
                    SXRPlane sxrPlane = mPlanes.get(plane);
                    SXRNode owner = sxrPlane.getOwnerObject();
                    if (owner != null)
                    {
                        Log.d(TAG, "SXR hit %f, %f, %f  plane = %s",
                              hitPoseMtx[12], hitPoseMtx[13], hitPoseMtx[14],
                              owner.getName());
                    }
                    gvrHitResult.setPose(hitPoseMtx);
                    gvrHitResult.setDistance(hit.getDistance() * mARtoVRScale);
                    gvrHitResult.setPlane(sxrPlane);
                    return gvrHitResult;
                }
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

            if (arCamera.getTrackingState() != TrackingState.TRACKING)
            {
                return;
            }
            syncARCamToVRCam(arCamera, mVRScene.getMainCameraRig());
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
        arCamera.getDisplayOrientedPose().toMatrix(mSXRCamMatrix, 0);
        cameraRig.getTransform().setModelMatrix(mSXRCamMatrix);
    }

    private void configDisplayAspectRatio(Activity activity)
    {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int width = mIsMono ? metrics.widthPixels : metrics.heightPixels;
        mScreenToCamera.x = mScreenWidth = width;
        mScreenToCamera.y = mScreenHeight = metrics.heightPixels;
        mSession.setDisplayGeometry(Surface.ROTATION_90, width, metrics.heightPixels);
    }

    private SXRMesh configMonoDisplay(Frame frame, SXRCameraRig cameraRig)
    {
        Camera arCamera = frame.getCamera();
        float[] m = new float[16];
        Matrix4f projmtx = new Matrix4f();
        SXRPerspectiveCamera centerCamera = cameraRig.getCenterCamera();
        float near = centerCamera.getNearClippingDistance() / mARtoVRScale;
        float far = centerCamera.getFarClippingDistance() / mARtoVRScale;

        // Get phones' cam projection matrix.
        arCamera.getProjectionMatrix(m, 0, near, far);
        projmtx.set(m);
        cameraRig.getHeadTransform().reset();
        cameraRig.getTransform().reset();
        cameraRig.setCameraRigType(SXRCameraRig.SXRCameraRigType.Freeze.ID);

        float aspectRatio = projmtx.m11() / projmtx.m00();
        float arCamFOV = projmtx.perspectiveFov();
        float tanfov =  (float) Math.tan(arCamFOV * 0.5f);
        float quadDistance = far - near;
        float quadHeight = quadDistance * tanfov * 2;
        float quadWidth = quadHeight * aspectRatio;

        // Use the same fov from AR to VR Camera as default value.
        centerCamera.setFovY((float) Math.toDegrees(arCamFOV));
        centerCamera.setAspectRatio(aspectRatio);
        mScreenToCamera.x = 1.0f;
        mScreenToCamera.y = 1.0f;
        mScreenDepth = quadDistance;
        android.util.Log.d(TAG, "ARCore configured to: passthrough[w: "
                + quadWidth + ", h: " + quadHeight +", z: " + quadDistance
                + "], cam fov: " + arCamFOV + ", aspect ratio: " + aspectRatio);
        mDisplayGeometry = new Vector3f(quadWidth, quadHeight, -quadDistance);
        return SXRMesh.createQuad(mContext,
                                  "float3 a_position float2 a_texcoord",
                                  quadWidth, quadHeight);
    }

    private SXRMesh configVRDisplay(Frame frame, SXRCameraRig cameraRig)
    {
        Camera arCamera = frame.getCamera();
        float[] m = new float[16];
        Matrix4f projmtx = new Matrix4f();
        SXRPerspectiveCamera leftCamera = (SXRPerspectiveCamera) cameraRig.getLeftCamera();

        cameraRig.getHeadTransform().setRotation(1, 0, 0, 0);
        cameraRig.setCameraRigType(SXRCameraRig.SXRCameraRigType.Freeze.ID);
        float near = leftCamera.getNearClippingDistance() / mARtoVRScale;
        float far = leftCamera.getFarClippingDistance() / mARtoVRScale;

        arCamera.getProjectionMatrix(m, 0, near, far);
        projmtx.set(m);

        float arDist = far - near;
        float aspectRatio = projmtx.m11() / projmtx.m00();
        float arFov = projmtx.perspectiveFov();
        float arTanfov =  (float) Math.tan(arFov * 0.5f);
        float arHeight = arDist * arTanfov * 2;
        float arWidth = arHeight * aspectRatio;

        mScreenToCamera.x = arWidth / mScreenToCamera.x;
        mScreenToCamera.y = arHeight / mScreenToCamera.y;
        mScreenDepth = arDist;
        android.util.Log.d(TAG, "ARCore configured to: passthrough[w: "
                + arWidth + ", h: " + arHeight +", z: " + arDist
                + "], cam fov: " + arFov + ", aspect ratio: " + aspectRatio);
        mDisplayGeometry = new Vector3f(arWidth, arHeight, -arDist);
        mSession.setDisplayGeometry(Surface.ROTATION_90, (int) arWidth, (int) arHeight);
        return SXRMesh.createQuad(mContext,
                                 "float3 a_position float2 a_texcoord",
                                  arHeight, arWidth);
    }

    @Override
    public SXRNode getPassThroughObject() {
        return mARPassThroughObject;
    }

    @Override
    public ArrayList<SXRPlane> getAllPlanes()
    {
        ArrayList<SXRPlane> allPlanes = new ArrayList<>();

        for (Plane plane: mPlanes.keySet())
        {
            allPlanes.add(mPlanes.get(plane));
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
        mAnchors.add(arCoreAnchor);
        arCoreAnchor.update();
        return arCoreAnchor;
    }

    Pose makePose(final float[] pose)
    {
        final float[] translation = new float[3];
        final float[] rotation = new float[4];
        final float[] newPose = pose.clone();

        gvr2ar(newPose);
        convertMatrixPoseToVector(newPose, translation, rotation);
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
