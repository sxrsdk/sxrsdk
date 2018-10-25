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
final class MonoscopicActivityDelegate extends SXRApplication.ActivityDelegateStubs {
    @Override
    public void onCreate(SXRApplication activity) {
        if (null == activity) {
            throw new IllegalArgumentException();
        }

        mApplication = activity;
    }

    @Override
    public SXRViewManager makeViewManager() {
        return new MonoscopicViewManager(mApplication, mApplication.getMain(), mXmlParser);
    }

    @Override
    public SXRCameraRig makeCameraRig(SXRContext context) {
        return new SXRCameraRig(context);
    }

    @Override
    public SXRConfigurationManager makeConfigurationManager() {
        return new MonoscopicConfigurationManager(mApplication);
    }

    @Override
    public void parseXmlSettings(AssetManager assetManager, String dataFilename) {
        mXmlParser = new MonoscopicXMLParser(assetManager, dataFilename, mApplication.getAppSettings());
    }

    @Override
    public boolean onBackPress() {
        return true;
    }

    @Override
    public boolean setMain(SXRMain gvrMain, String dataFileName) {
        return true;
    }

    @Override
    public VrAppSettings makeVrAppSettings() {
        return new MonoscopicVrAppSettings();
    }

    private SXRApplication mApplication;
    private MonoscopicXMLParser mXmlParser;
}
