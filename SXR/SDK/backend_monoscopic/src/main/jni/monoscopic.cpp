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

#include "objects/textures/render_texture.h"

namespace sxr {
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_samsungxr_MonoscopicViewManager_makeRenderTextureInfo(JNIEnv *, jobject, int fboId, int fboWidth, int fboHeight) {
    RenderTextureInfo* renderTextureInfo = new RenderTextureInfo;

    renderTextureInfo->fboWidth = fboWidth;
    renderTextureInfo->fboHeight = fboHeight;
    renderTextureInfo->fboId = fboId;
    renderTextureInfo->viewport[2] = fboWidth;
    renderTextureInfo->viewport[3] = fboHeight;

    renderTextureInfo->multisamples = 1;
    renderTextureInfo->texId = 0;
    renderTextureInfo->useMultiview = false;
    renderTextureInfo->layers = 0;
    renderTextureInfo->viewport[0] = 0;
    renderTextureInfo->viewport[1] = 0;

    return reinterpret_cast<jlong>(renderTextureInfo);
}

}
}