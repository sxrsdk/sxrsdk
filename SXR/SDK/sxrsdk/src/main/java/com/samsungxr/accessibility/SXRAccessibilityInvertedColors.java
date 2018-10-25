package com.samsungxr.accessibility;

/*
 * Copyright 2015 Samsung Electronics Co., LTD
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderId;

public class SXRAccessibilityInvertedColors {

    private SXRMaterial postEffect;
    private boolean mInverted;
    private SXRContext mGvrContext;

    /**
     * Initialize {@link SXRShaderData}
     *
     * @param gvrContext
     */
    public SXRAccessibilityInvertedColors(final SXRContext gvrContext) {
        mGvrContext = gvrContext;
        SXRShaderId shaderId = gvrContext.getShaderManager().getShaderType(SXRAccessibilityPostEffectShader.class);
        postEffect = new SXRMaterial(gvrContext, shaderId);
    }

    public void turnOn(final SXRScene... scene) {
        mInverted = true;
        for (SXRScene gvrScene : scene) {
            gvrScene.getMainCameraRig().getLeftCamera()
                            .addPostEffect(postEffect);
            gvrScene.getMainCameraRig().getRightCamera()
                            .addPostEffect(postEffect);
        }
    }

    public void turnOff(final SXRScene... scene) {
        mInverted = false;
        for (SXRScene gvrScene : scene) {
            gvrScene.getMainCameraRig().getLeftCamera()
                            .removePostEffect(postEffect);
            gvrScene.getMainCameraRig().getRightCamera()
                            .removePostEffect(postEffect);
        }
    }

    public boolean isInverted() {
        return mInverted;
    }

}
