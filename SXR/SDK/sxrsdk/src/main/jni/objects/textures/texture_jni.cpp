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
#include "texture.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeTexture_constructor(JNIEnv * env, jobject obj);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeTexture_isReady(JNIEnv * env, jobject obj, jlong jtexture);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeTexture_clearData(JNIEnv * env, jobject obj, jlong jtexture);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeTexture_updateTextureParameters(JNIEnv * env, jobject obj,
                jlong jtexture, jintArray jtexture_parameters);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeTexture_getId(JNIEnv * env, jobject obj, jlong jtexture);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeTexture_setImage(JNIEnv * env, jobject obj,
                jlong jtexture, jlong nativeImage);
}
;

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeTexture_constructor(JNIEnv * env, jobject obj)
{
    Texture* tex = Renderer::getInstance()->createTexture(0);
    return reinterpret_cast<jlong>(tex);
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeTexture_getId(JNIEnv * env, jobject obj, jlong jtexture)
{
    Texture* texture = reinterpret_cast<Texture*>(jtexture);
    return texture->getId();
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeTexture_isReady(JNIEnv * env, jobject obj, jlong jtexture)
{
    Texture* texture = reinterpret_cast<Texture*>(jtexture);
    return texture->isReady();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeTexture_clearData(JNIEnv* env, jobject obj, jlong jtexture)
{
    Texture* texture = reinterpret_cast<Texture*>(jtexture);
    texture->clearData(env);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeTexture_updateTextureParameters(JNIEnv * env, jobject obj,
        jlong jtexture, jintArray jtexture_parameters) {
    Texture* texture = reinterpret_cast<Texture*>(jtexture);

    jint* texture_parameters = env->GetIntArrayElements(jtexture_parameters, 0);
    texture->updateTextureParameters(texture_parameters, env->GetArrayLength(jtexture_parameters));
    env->ReleaseIntArrayElements(jtexture_parameters, texture_parameters, 0);

}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeTexture_setImage(JNIEnv* env, jobject obj, jlong jtexture,
                                        jlong nativeImage)
{
    Texture* texture = reinterpret_cast<Texture*>(jtexture);
    Image* image = reinterpret_cast<Image*>(nativeImage);
    texture->setImage(env, image);
}
}
