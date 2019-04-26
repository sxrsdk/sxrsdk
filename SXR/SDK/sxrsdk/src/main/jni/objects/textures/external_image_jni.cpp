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
#include <jni.h>

#include <engine/renderer/renderer.h>
#include "external_image.h"

namespace sxr {
extern "C" {
JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_ctor(JNIEnv * env,
        jobject obj);
JNIEXPORT void JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_setData(JNIEnv * env,
        jobject obj, jlong ptr, jlong data);
JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_getData(JNIEnv * env,
        jobject obj, jlong ptr);
}
;

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_ctor(JNIEnv * env, jobject obj) {
    return reinterpret_cast<jlong>(Renderer::getInstance()->createTexture(Texture::TextureType::TEXTURE_EXTERNAL_RENDERER));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_setData(JNIEnv * env,
        jobject obj, jlong ptr, jlong data) {
    reinterpret_cast<ExternalImage*>(ptr)->setData(data);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeExternalRendererTexture_getData(JNIEnv * env,
        jobject obj, jlong ptr) {
    return reinterpret_cast<ExternalImage*>(ptr)->getData();
}

}
