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

package com.samsungxr;

import android.opengl.GLSurfaceView;
import android.view.WindowManager;

import com.google.vr.ndk.base.AndroidCompat;
import com.google.vr.ndk.base.GvrLayout;

import com.samsungxr.io.SXRGearCursorController;

class DaydreamViewManager extends SXRViewManager {
    private static final String TAG = DaydreamViewManager.class.getSimpleName();
    private GvrLayout gvrLayout;
    private DaydreamRenderer renderer;
    private GLSurfaceView surfaceView;
    private SXRCameraRig cameraRig;
    private boolean sensoredSceneUpdated = false;
    private  SXRRenderTarget mDaydreamRenderTarget = null;
    private int defaultWidth = 1024;
    private int defaultHeight = 1024;

    // This is done on the GL thread because refreshViewerProfile isn't thread-safe.
    private final Runnable refreshViewerProfileRunnable =
            new Runnable() {
                @Override
                public void run() {
                    gvrLayout.getGvrApi().refreshViewerProfile();
                }
            };

    DaydreamViewManager(final SXRApplication application, SXRMain sxrMain) {
        super(application, sxrMain);

        // Initialize GvrLayout and the native renderer.
        gvrLayout = new GvrLayout(application.getActivity());

        surfaceView = new GLSurfaceView(application.getActivity());
        surfaceView.setEGLContextClientVersion(3);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 8);
        surfaceView.setPreserveEGLContextOnPause(true);

        renderer = new DaydreamRenderer(this, gvrLayout.getGvrApi().getNativeGvrContext());
        surfaceView.setRenderer(renderer);

        gvrLayout.setPresentationView(surfaceView);

        // Add the GvrLayout to the View hierarchy.
        application.getActivity().setContentView(gvrLayout);

        // Enable scan line racing.
        if (gvrLayout.setAsyncReprojectionEnabled(true)) {
            // Scanline racing decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(application.getActivity(), true);
        }

        // Enable VR Mode.
        AndroidCompat.setVrModeEnabled(application.getActivity(), true);

        // Prevent screen from dimming/locking.
        application.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mControllerReader = new DayDreamControllerReader(this);

    }

    public long getNativeRenderer(){
        return renderer.getNativeDaydreamRenderer();
    }
    public SXRRenderTarget getRenderTarget(){
        if(null == mDaydreamRenderTarget){
            mDaydreamRenderTarget = new SXRRenderTarget(mApplication.getSXRContext().getMainScene(),
                    defaultWidth, defaultHeight);
        }
        return mDaydreamRenderTarget;
    }
    @Override
    void onResume() {
        super.onResume();
        renderer.onResume();
        gvrLayout.onResume();
        surfaceView.onResume();
        surfaceView.queueEvent(refreshViewerProfileRunnable);
    }

    @Override
    void onSurfaceCreated()
    {
        super.onSurfaceCreated();
        SXRGearCursorController gearController = mInputManager.getGearController();
        if (gearController != null)
            gearController.attachReader(mControllerReader);
    }

    @Override
    void onPause() {
        super.onPause();
        renderer.onPause();
        gvrLayout.onPause();
        surfaceView.onPause();
    }

    @Override
    void onDestroy() {
        super.onDestroy();
        gvrLayout.shutdown();
        renderer.onDestroy();
    }
    void onDrawEye(int eye) {
        if (cameraRig == null) {
            return;
        }
        if (!sensoredSceneUpdated) {
            sensoredSceneUpdated = updateSensoredScene();
        }

        if (eye == 0) {
            SXRRenderTarget renderTarget = getRenderTarget();
            SXRCamera leftCamera = cameraRig.getLeftCamera();
            renderTarget.cullFromCamera(mMainScene,mMainScene.getMainCameraRig().getCenterCamera(),mRenderBundle.getShaderManager());
            captureCenterEye(renderTarget, false);
            capture3DScreenShot(renderTarget, false);

            renderTarget.render(mMainScene,leftCamera,mRenderBundle.getShaderManager(),mRenderBundle.getPostEffectRenderTextureA(),
                    mRenderBundle.getPostEffectRenderTextureB());


            captureLeftEye(renderTarget, false);
        } else {
            SXRCamera rightCamera = cameraRig.getRightCamera();
            SXRRenderTarget renderTarget = getRenderTarget();

            renderTarget.render(mMainScene, rightCamera, mRenderBundle.getShaderManager(),mRenderBundle.getPostEffectRenderTextureA(),
                    mRenderBundle.getPostEffectRenderTextureB());
            captureRightEye(renderTarget,false);
        }
        captureFinish();
    }

    void setCameraRig(SXRCameraRig cameraRig) {
        this.cameraRig = cameraRig;
        renderer.setCameraRig(cameraRig);
        sensoredSceneUpdated = false;
    }
}
