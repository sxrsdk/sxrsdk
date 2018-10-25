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
package com.samsungxr.accessibility;

import java.util.concurrent.Future;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRShaderId;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRSceneObject;
import com.samsungxr.SXRTexture;

public class SXRAccessiblityObject extends SXRSceneObject {

    private SXRAccessibilityTalkBack mTalkBack;

    public SXRAccessiblityObject(SXRContext gvrContext, float width, float height, SXRTexture texture, SXRShaderId shaderId) {
        super(gvrContext, width, height, texture, shaderId);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, float width, float height, SXRTexture texture) {
        super(gvrContext, width, height, texture);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, float width, float height) {
        super(gvrContext, width, height);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, SXRAndroidResource mesh, SXRAndroidResource texture) {
        super(gvrContext, mesh, texture);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, SXRMesh mesh, SXRTexture texture, SXRShaderId shaderId) {
        super(gvrContext, mesh, texture, shaderId);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, SXRMesh mesh, SXRTexture texture) {
        super(gvrContext, mesh, texture);

    }

    public SXRAccessiblityObject(SXRContext gvrContext, SXRMesh mesh) {
        super(gvrContext, mesh);

    }

    public SXRAccessiblityObject(SXRContext gvrContext) {
        super(gvrContext);

    }

    public SXRAccessibilityTalkBack getTalkBack() {
        return mTalkBack;
    }

    public void setTalkBack(SXRAccessibilityTalkBack mTalkBack) {
        this.mTalkBack = mTalkBack;
    }

}
