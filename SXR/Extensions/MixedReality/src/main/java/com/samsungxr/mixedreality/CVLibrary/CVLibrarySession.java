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
import android.util.DisplayMetrics;
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

import com.google.ar.core.Camera;
import com.google.ar.core.HitResult;
import com.samsungxr.SXRCameraRig;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRDrawFrameListener;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRPerspectiveCamera;
import com.samsungxr.SXRPicker;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTexture;
import com.samsungxr.mixedreality.IMixedReality;
import com.samsungxr.mixedreality.IPlaneEvents;
import com.samsungxr.mixedreality.SXRAnchor;
import com.samsungxr.mixedreality.SXRHitResult;
import com.samsungxr.mixedreality.SXRLightEstimate;
import com.samsungxr.mixedreality.SXRMarker;
import com.samsungxr.mixedreality.SXRPlane;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;


public class CVLibrarySession implements IMixedReality, SXRDrawFrameListener
{

    private SXRContext mContext;
    private static float AR2VR_SCALE = 100;
    private SXREventReceiver    mListeners;
    private SXRScene mVRScene;
    private SXRNode mARPassThroughObject;
    private boolean mEnableCloudAnchor;
    private ArrayList<SXRAnchor> mAnchors = new ArrayList<>();
    private ArrayList<SXRPlane> mPlanes = new ArrayList<>();

    /* From AR to SXR space matrices */
    private float[] mARViewMatrix = new float[16];
    private float[] mSXRCamMatrix = new float[16];
    private Vector2f mScreenToCamera = new Vector2f(1, 1);
    private Vector3f mDisplayGeometry;
    private float mScreenDepth;

    //private final HashMap<Anchor, ICloudAnchorListener> pendingAnchors = new HashMap<>();

    public CVLibrarySession(SXRScene scene, boolean enableCloudAnchor)
    {
        mContext = scene.getSXRContext();
        mVRScene = scene;
        mEnableCloudAnchor = enableCloudAnchor;
        mListeners = new SXREventReceiver(this);
        initSession(scene.getSXRContext());
    }

    public SXREventReceiver getEventReceiver() { return mListeners; }

    private void initSession(SXRContext ctx)
    {
        final DisplayMetrics metrics = new DisplayMetrics();
        ctx.getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        mScreenToCamera.x = metrics.widthPixels;
        mScreenToCamera.y = metrics.heightPixels;
/*
        mARPassThroughObject = new SXRNode(ctx, mDisplayGeometry.x, mDisplayGeometry.y);
        mARPassThroughObject.getRenderData().setRenderingOrder(SXRRenderData.SXRRenderingOrder.BACKGROUND);
        mARPassThroughObject.getRenderData().setDepthTest(false);
        mARPassThroughObject.getTransform().setPosition(0, 0, mDisplayGeometry.z);
        mARPassThroughObject.attachComponent(new SXRMeshCollider(ctx, true));
        mARPassThroughObject.getRenderData().setRenderMask(0);
        mVRScene.addNode(mARPassThroughObject);
*/
        /* AR main loop */
        configDisplayGeometry(mVRScene.getMainCameraRig());
        ctx.registerDrawFrameListener(this);
    }

    public void pause() { }

    public void resume() { }

    public float getScreenDepth() { return mScreenDepth; }

    public float getARToVRScale() { return AR2VR_SCALE; }

    public void onDrawFrame(float t)
    {
        updatePlanes(mARViewMatrix, mSXRCamMatrix, AR2VR_SCALE);
        updateAnchors(mARViewMatrix, mSXRCamMatrix, AR2VR_SCALE);
    }

    @Override
    public SXRNode getPassThroughObject()
    {
        return mARPassThroughObject;
    }

    @Override
    public ArrayList<SXRPlane> getAllPlanes()
    {
        return mPlanes;
    }

    @Override
    public SXRAnchor createAnchor(float[] pose)
    {
        CVLibraryAnchor anchor = new CVLibraryAnchor(mContext);
        anchor.setAnchorAR();
        mAnchors.add(anchor);
        return anchor;
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
    public void updateAnchorPose(SXRAnchor anchor, float[] pose)
    {
    }

    @Override
    public void removeAnchor(SXRAnchor anchor)
    {
        mAnchors.remove(anchor);
        SXRNode owner = anchor.getOwnerObject();
        if (owner != null)
        {
            owner.detachComponent(SXRAnchor.getComponentType());
        }
    }

    /**
     * This method hosts an anchor. The {@code listener} will be invoked when the results are
     * available.
     */
    @Override
    synchronized public void hostAnchor(SXRAnchor anchor, IMixedReality.CloudAnchorCallback listener)
    {
//        Anchor newAnchor = mSession.hostCloudAnchor(((ARCoreAnchor)anchor).getAnchorAR());
//        pendingAnchors.put(newAnchor, listener);
        return;
    }

    /**
     * This method resolves an anchor. The {@code listener} will be invoked when the results are
     * available.
     */
    synchronized public void resolveCloudAnchor(String anchorId, IMixedReality.CloudAnchorCallback listener)
    {
//        Anchor newAnchor = mSession.resolveCloudAnchor(anchorId);
//        pendingAnchors.put(newAnchor, listener);
        return;
    }

    public void setEnableCloudAnchor(boolean enableCloudAnchor)
    {
        mEnableCloudAnchor = enableCloudAnchor;
    }

    public SXRLightEstimate getLightEstimate()
    {
        return  new CVLibraryLightEstimate();
    }

    @Override
    public void setMarker(Bitmap image)
    {
    }

    @Override
    public void setMarkers(ArrayList<Bitmap> imagesList)
    {
    }

    @Override
    public ArrayList<SXRMarker> getAllMarkers()
    {
        return null;
    }

    private void updatePlanes(float[] arViewMatrix, float[] vrCamMatrix, float scale)
    {
        if (mPlanes.size() == 0)
        {
            SXRPlane plane = new CVLibraryPlane(mContext);
            mPlanes.add(plane);
            mContext.getEventManager().sendEvent(this, IPlaneEvents.class, "onPlaneDetected", plane);
        }
    }

    private void updateAnchors(float[] arViewMatrix, float[] vrCamMatrix, float scale)
    {

    }

    public SXRHitResult hitTest(SXRPicker.SXRPickedObject pick)
    {
        return null;
    }

    public SXRHitResult hitTest(float x, float y)
    {
        x *= mScreenToCamera.x; // screen -> camera space
        y *= mScreenToCamera.y;
        return null;
    }

    public float[] makeInterpolated(float[] poseA, float[] poseB, float t)
    {
        Matrix4f mtxA = new Matrix4f();
        Matrix4f mtxB = new Matrix4f();
        Quaternionf qA = new Quaternionf();
        Quaternionf qB = new Quaternionf();
        Vector3f vA = new Vector3f();
        Vector3f vB = new Vector3f();
        float[] result = new float[16];

        mtxA.set(poseA);
        mtxB.set(poseB);
        mtxA.getTranslation(vA);
        mtxB.getTranslation(vB);
        mtxA.getUnnormalizedRotation(qA);
        mtxB.getUnnormalizedRotation(qB);
        qA.slerp(qB, t);
        vA.lerp(vB, t);
        mtxA.rotation(qA);
        mtxA.setTranslation(vA);
        mtxA.get(result);
        return result;
    }

    private Vector3f configDisplayGeometry(SXRCameraRig cameraRig)
    {
        SXRPerspectiveCamera centerCamera = cameraRig.getCenterCamera();
        float near = centerCamera.getNearClippingDistance();
        float far = centerCamera.getFarClippingDistance();
        float tanfov = (float) Math.tan(centerCamera.getFovY());
        float quadDistance = far - 1;
        float quadHeight = quadDistance * tanfov * 2;
        float quadWidth = quadHeight * centerCamera.getAspectRatio();

        mScreenToCamera.x = quadWidth / mScreenToCamera.x;    // map [0, ScreenSize] to [-Display, +Display]
        mScreenToCamera.y = quadHeight / mScreenToCamera.y;
        mScreenDepth = quadHeight / tanfov;
        return new Vector3f(quadWidth, quadHeight, -quadDistance);
    }
}