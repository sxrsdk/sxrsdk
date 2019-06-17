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
final class OvrActivityDelegate extends SXRApplication.ActivityDelegateStubs {
    private SXRApplication mApplication;
    private OvrViewManager mActiveViewManager;
    private OvrActivityNative mActivityNative;

    @Override
    public void onCreate(SXRApplication application) {
        mApplication = application;

        mActivityNative = new OvrActivityNative(mApplication);
        mActivityHandler = new OvrVrapiActivityHandler(application, mActivityNative);
    }

    @Override
    public OvrActivityNative getActivityNative() {
        return mActivityNative;
    }

    @Override
    public SXRViewManager makeViewManager() {
        return new OvrViewManager(mApplication, mApplication.getMain(), mXmlParser);
    }

    @Override
    public SXRCameraRig makeCameraRig(SXRContext context) {
        return new SXRCameraRig(context);
    }

    @Override
    public SXRConfigurationManager makeConfigurationManager() {
        return new OvrConfigurationManager(mApplication);
    }

    @Override
    public void parseXmlSettings(AssetManager assetManager, String dataFilename) {
        mXmlParser = new OvrXMLParser(assetManager, dataFilename, mApplication.getAppSettings());
    }

    @Override
    public boolean onBackPress() {
        if (null != mActivityHandler) {
            return mActivityHandler.onBack();
        }
        return false;
    }

    @Override
    public void onPause() {
        if (null != mActivityHandler) {
            mActivityHandler.onPause();
        }
    }

    @Override
    public void onResume() {
        if (null != mActivityHandler) {
            mActivityHandler.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (null != mActivityHandler) {
            mActivityHandler.onDestroy();
        }
    }

    @Override
    public boolean setMain(SXRMain gvrMain, String dataFileName) {
        if (null != mActivityHandler) {
            mActivityHandler.onSetScript();
        }
        return true;
    }

    @Override
    public void setViewManager(SXRViewManager viewManager) {
        mActiveViewManager = (OvrViewManager)viewManager;
        mActivityHandler.setViewManager(mActiveViewManager);
    }

    @Override
    public VrAppSettings makeVrAppSettings() {
        final VrAppSettings settings = new OvrVrAppSettings();
        final VrAppSettings.EyeBufferParams params = settings.getEyeBufferParams();
        return settings;
    }

    private OvrXMLParser mXmlParser;
    private OvrActivityHandler mActivityHandler;
}
