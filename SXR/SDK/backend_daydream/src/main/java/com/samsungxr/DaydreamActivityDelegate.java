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

import android.content.res.AssetManager;

import com.samsungxr.utility.VrAppSettings;

/**
 * {@inheritDoc}
 */
final class DaydreamActivityDelegate extends SXRApplication.ActivityDelegateStubs implements IActivityNative {
    private SXRApplication mApplication;
    private DaydreamViewManager daydreamViewManager;

    @Override
    public void onCreate(SXRApplication application) {
        mApplication = application;
    }

    @Override
    public IActivityNative getActivityNative() {
        return this;
    }

    @Override
    public SXRViewManager makeViewManager() {
        return new DaydreamViewManager(mApplication, mApplication.getMain());
    }

    @Override
    public SXRCameraRig makeCameraRig(SXRContext context) {
        return new DaydreamCameraRig(context);
    }

    @Override
    public SXRConfigurationManager makeConfigurationManager() {
        return new SXRConfigurationManager(mApplication) {
            @Override
            public boolean isHmtConnected() {
                return false;
            }

            public boolean usingMultiview() {
                return false;
            }
        };
    }

    @Override
    public void onInitAppSettings(VrAppSettings appSettings) {
        // This is the only place where the setDockListenerRequired flag can be set before
        // the check in SXRActivity.
        mApplication.getConfigurationManager().setDockListenerRequired(false);
    }

    @Override
    public void parseXmlSettings(AssetManager assetManager, String dataFilename) {
        new DaydreamXMLParser(assetManager, dataFilename, mApplication.getAppSettings());
    }

    @Override
    public boolean setMain(SXRMain gvrMain, String dataFileName) {
        return true;
    }

    @Override
    public void setViewManager(SXRViewManager viewManager) {
        daydreamViewManager = (DaydreamViewManager) viewManager;
    }

    @Override
    public VrAppSettings makeVrAppSettings() {
        final VrAppSettings settings = new VrAppSettings();
        final VrAppSettings.EyeBufferParams params = settings.getEyeBufferParams();
        params.setResolutionHeight(VrAppSettings.DEFAULT_FBO_RESOLUTION);
        params.setResolutionWidth(VrAppSettings.DEFAULT_FBO_RESOLUTION);
        return settings;
    }

    @Override
    public void setCameraRig(SXRCameraRig cameraRig) {
        if (daydreamViewManager != null) {
            daydreamViewManager.setCameraRig(cameraRig);
        }
    }

    @Override
    public void onUndock() {

    }

    @Override
    public void onDock() {

    }

    @Override
    public long getNative() {
        return 0;
    }

    /**
     * The class ignores the perspective camera and attaches a custom left and right camera instead.
     * Daydream uses the glFrustum call to create the projection matrix. Using the custom camera
     * allows us to set the projection matrix from the glFrustum call against the custom camera
     * using the set_projection_matrix call in the native renderer.
     */
    static class DaydreamCameraRig extends SXRCameraRig {
        protected DaydreamCameraRig(SXRContext gvrContext) {
            super(gvrContext);
        }

        @Override
        public void attachLeftCamera(SXRCamera camera) {
            SXRCamera leftCamera = new SXRCustomCamera(getSXRContext());
            leftCamera.setRenderMask(SXRRenderData.SXRRenderMaskBit.Left);
            super.attachLeftCamera(leftCamera);
        }

        @Override
        public void attachRightCamera(SXRCamera camera) {
            SXRCamera rightCamera = new SXRCustomCamera(getSXRContext());
            rightCamera.setRenderMask(SXRRenderData.SXRRenderMaskBit.Right);
            super.attachRightCamera(rightCamera);
        }
    }
}
