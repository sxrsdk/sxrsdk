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

package com.samsungxr.scene_objects;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import com.samsungxr.SXRContext;
import com.samsungxr.SXREventListeners;
import com.samsungxr.SXRExternalTexture;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMaterial.SXRShaderType;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.SXRTexture;
import com.samsungxr.utility.Log;

import java.io.IOException;

/**
 * A {@linkplain SXRSceneObject scene object} that shows live video from one of
 * the device's cameras
 */
public class SXRCameraSceneObject extends SXRSceneObject {
    private static String TAG = SXRCameraSceneObject.class.getSimpleName();
    private final SurfaceTexture mSurfaceTexture;
    private boolean mPaused = false;
    private Camera camera;
    private SXRContext gvrContext;
    private boolean cameraSetUpStatus;
    private int fpsMode = -1;
    private boolean isCameraOpen = false;
    private CameraActivityEvents cameraActivityEvents;

    /**
     * Create a {@linkplain SXRSceneObject scene object} (with arbitrarily
     * complex geometry) that shows live video from one of the device's cameras
     *
     * @param gvrContext current {@link SXRContext}
     * @param mesh       an arbitrarily complex {@link SXRMesh} object - see
     *                   {@link SXRContext#loadMesh(com.samsungxr.SXRAndroidResource)}
     *                   and {@link SXRContext#createQuad(float, float)}
     * @param camera     an Android {@link Camera}. <em>Note</em>: this constructor
     *                   calls {@link Camera#setPreviewTexture(SurfaceTexture)} so you
     *                   should be sure to call it before you call
     *                   {@link Camera#startPreview()}.
     * @deprecated This call does not ensure the activity lifecycle is correctly
     * handled by the {@link SXRCameraSceneObject}. Use
     * {@link #SXRCameraSceneObject(SXRContext, SXRMesh)} instead.
     */
    public SXRCameraSceneObject(SXRContext gvrContext, SXRMesh mesh,
                                Camera camera) {
        super(gvrContext, mesh);
        SXRTexture texture = new SXRExternalTexture(gvrContext);
        SXRMaterial material = new SXRMaterial(gvrContext, SXRShaderType.OES.ID);
        material.setMainTexture(texture);
        getRenderData().setMaterial(material);

        this.gvrContext = gvrContext;
        this.camera = camera;
        isCameraOpen = true;
        mSurfaceTexture = new SurfaceTexture(texture.getId());
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            Runnable onFrameAvailableGLCallback = new Runnable() {
                @Override
                public void run() {
                    mSurfaceTexture.updateTexImage();
                }
            };

            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                SXRCameraSceneObject.this.gvrContext.runOnGlThread(onFrameAvailableGLCallback);
            }
        });

        try {
            this.camera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a {@linkplain SXRSceneObject scene object} (with arbitrarily
     * complex geometry) that shows live video from one of the device's cameras
     *
     * @param gvrContext current {@link SXRContext}
     * @param mesh       an arbitrarily complex {@link SXRMesh} object - see
     *                   {@link SXRContext#loadMesh(com.samsungxr.SXRAndroidResource)}
     *                   and {@link SXRContext#createQuad(float, float)}
     * @throws SXRCameraAccessException returns this exception when the camera cannot be
     *                                  initialized correctly.
     */
    public SXRCameraSceneObject(SXRContext gvrContext, SXRMesh mesh) throws
            SXRCameraAccessException {
        super(gvrContext, mesh);

        SXRTexture texture = new SXRExternalTexture(gvrContext);
        SXRMaterial material = new SXRMaterial(gvrContext, SXRShaderType.OES.ID);
        material.setMainTexture(texture);
        getRenderData().setMaterial(material);
        this.gvrContext = gvrContext;

        mSurfaceTexture = new SurfaceTexture(texture.getId());
        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            Runnable onFrameAvailableGLCallback = new Runnable() {
                @Override
                public void run() {
                    mSurfaceTexture.updateTexImage();
                }
            };

            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                SXRCameraSceneObject.this.gvrContext.runOnGlThread(onFrameAvailableGLCallback);
            }
        });

        if (!openCamera()) {
            Log.e(TAG, "Cannot open the camera");
            throw new SXRCameraAccessException("Cannot open the camera");
        }

        cameraActivityEvents = new CameraActivityEvents();
        gvrContext.getApplication().getEventReceiver().addListener(cameraActivityEvents);
    }

    /**
     * Create a 2D, rectangular {@linkplain SXRSceneObject scene object} that
     * shows live video from one of the device's cameras
     *
     * @param gvrContext current {@link SXRContext}
     * @param width      the scene rectangle's width
     * @param height     the rectangle's height
     * @param camera     an Android {@link Camera}. <em>Note</em>: this constructor
     *                   calls {@link Camera#setPreviewTexture(SurfaceTexture)} so you
     *                   should be sure to call it before you call
     *                   {@link Camera#startPreview()}.
     * @deprecated This call does not ensure the activity lifecycle is correctly
     * handled by the {@link SXRCameraSceneObject}. Use
     * {@link #SXRCameraSceneObject(SXRContext, float, float)} instead.
     */
    public SXRCameraSceneObject(SXRContext gvrContext, float width,
                                float height, Camera camera) {
        this(gvrContext, gvrContext.createQuad(width, height), camera);
    }

    /**
     * Create a 2D, rectangular {@linkplain SXRSceneObject scene object} that
     * shows live video from one of the device's cameras.
     *
     * @param gvrContext current {@link SXRContext}
     * @param width      the scene rectangle's width
     * @param height     the rectangle's height
     *
     * @throws SXRCameraAccessException this exception is returned when the camera cannot be opened.
     */
    public SXRCameraSceneObject(SXRContext gvrContext, float width,
                                float height) throws SXRCameraAccessException {
        this(gvrContext, gvrContext.createQuad(width, height));
    }

    private boolean openCamera() {
        if (camera != null) {
            //already open
            return true;
        }

        if (!checkCameraHardware(gvrContext.getActivity())) {
            android.util.Log.d(TAG, "Camera hardware not available.");
            return false;
        }
        try {
            camera = Camera.open();

            if (camera == null) {
                android.util.Log.d(TAG, "Camera not available or is in use");
                return false;
            }
            camera.startPreview();
            camera.setPreviewTexture(mSurfaceTexture);
            isCameraOpen = true;
        } catch (Exception exception) {
            android.util.Log.d(TAG, "Camera not available or is in use");
            return false;
        }

        return true;
    }

    private void closeCamera() {
        if (camera == null) {
            //nothing to do
            return;
        }

        camera.stopPreview();
        camera.release();
        camera = null;
        isCameraOpen = false;
    }

    private class CameraActivityEvents extends SXREventListeners.ActivityEvents {
        @Override
        public void onPause() {
            mPaused = true;
            closeCamera();
        }

        @Override
        public void onResume() {
            if (openCamera()) {
                //restore fpsmode
                setUpCameraForVrMode(fpsMode);
            }
            mPaused = false;
        }
    }

    /**
     * Resumes camera preview
     *
     * <p>
     * Note: {@link #pause()} and {@code resume()} only affect the polling that
     * links the Android {@link Camera} to this {@linkplain SXRSceneObject SXRF
     * scene object:} they have <em>no affect</em> on the underlying
     * {@link Camera} object.
     */
    public void resume() {
        mPaused = false;
    }

    /**
     * Pauses camera preview
     *
     * <p>
     * Note: {@code pause()} and {@link #resume()} only affect the polling that
     * links the Android {@link Camera} to this {@linkplain SXRSceneObject SXRF
     * scene object:} they have <em>no affect</em> on the underlying
     * {@link Camera} object.
     */
    public void pause() {
        mPaused = true;
    }

    /**
     * Close the {@link SXRCameraSceneObject}.
     */
    public void close() {
        closeCamera();
        if(cameraActivityEvents != null){
            gvrContext.getApplication().getEventReceiver().removeListener(cameraActivityEvents);
        }
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }

    /**
     * Configure high fps settings in the camera for VR mode
     *
     * @param fpsMode integer indicating the desired fps: 0 means 30 fps, 1 means 60
     *                fps, and 2 means 120 fps. Any other value is invalid.
     * @return A boolean indicating the status of the method call. It may be false due
     * to multiple reasons including: 1) supplying invalid fpsMode as the input
     * parameter, 2) VR mode not supported.
     */
    public boolean setUpCameraForVrMode(final int fpsMode) {

        cameraSetUpStatus = false;
        this.fpsMode = fpsMode;

        if (!isCameraOpen) {
            Log.e(TAG, "Camera is not open");
            return false;
        }
        if (fpsMode < 0 || fpsMode > 2) {
            Log.e(TAG,
                    "Invalid fpsMode: %d. It can only take values 0, 1, or 2.", fpsMode);
        } else {
            Parameters params = camera.getParameters();

            // check if the device supports vr mode preview
            if ("true".equalsIgnoreCase(params.get("vrmode-supported"))) {

                Log.v(TAG, "VR Mode supported!");

                // set vr mode
                params.set("vrmode", 1);

                // true if the apps intend to record videos using
                // MediaRecorder
                params.setRecordingHint(true);

                // set preview size
                // params.setPreviewSize(640, 480);

                // set fast-fps-mode: 0 for 30fps, 1 for 60 fps,
                // 2 for 120 fps
                params.set("fast-fps-mode", fpsMode);

                switch (fpsMode) {
                    case 0: // 30 fps
                        params.setPreviewFpsRange(30000, 30000);
                        break;
                    case 1: // 60 fps
                        params.setPreviewFpsRange(60000, 60000);
                        break;
                    case 2: // 120 fps
                        params.setPreviewFpsRange(120000, 120000);
                        break;
                    default:
                }

                // for auto focus
                params.set("focus-mode", "continuous-video");

                params.setVideoStabilization(false);
                if ("true".equalsIgnoreCase(params.get("ois-supported"))) {
                    params.set("ois", "center");
                }

                camera.setParameters(params);
                cameraSetUpStatus = true;
            }
        }

        return cameraSetUpStatus;
    }

    /**
     * This Exception is returned when the {@link SXRCameraSceneObject} cannot be instantiated
     * when the camera is not available.
     */
    public class SXRCameraAccessException extends Exception {
        public SXRCameraAccessException(String message) {
            super(message);
        }
    }
}
