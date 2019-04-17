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
#include <objects/textures/render_texture.h>
#include "ovr_activity.h"

namespace sxr {

extern "C" {

    JNIEXPORT long JNICALL Java_com_samsungxr_OvrActivityNative_onCreate(JNIEnv *jni, jclass,
                                                                       jobject activity,
                                                                       jobject vrAppSettings) {
        SXRActivity *sxrActivity = new SXRActivity(*jni, activity, vrAppSettings);
        return reinterpret_cast<long>(sxrActivity);
    }

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_OvrViewManager_getRenderTextureInfo(JNIEnv *, jobject, jlong jactivity,
                                                           jint index, jint eye) {
        SXRActivity *sxrActivity = reinterpret_cast<SXRActivity *>(jactivity);
        return reinterpret_cast<long>(sxrActivity->getRenderTextureInfo(eye, index));
    }

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrActivityNative_onDestroy(JNIEnv * jni, jclass clazz, jlong appPtr) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        delete activity;
    }

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrActivityNative_setCameraRig(JNIEnv * jni, jclass clazz, jlong appPtr,
                                                                           jlong cameraRig) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        activity->setCameraRig(cameraRig);
    }

// -------------------- //
// VrapiActivityHandler //
// -------------------- //

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeLeaveVrMode(JNIEnv * jni, jclass clazz,
                                                                                      jlong appPtr) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        activity->leaveVrMode();
    }

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeOnSurfaceCreated(JNIEnv * jni, jclass clazz,
                                                                                           jlong appPtr) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        activity->onSurfaceCreated(*jni);
    }

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeOnSurfaceChanged(JNIEnv * jni, jclass,
                                                                                           jlong appPtr, jobject jsurface) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        activity->onSurfaceChanged(*jni, jsurface);
    }

JNIEXPORT void JNICALL
Java_com_samsungxr_OvrViewManager_drawEyes(JNIEnv* env, jobject jViewManager, jlong appPtr, jobject mainScene) {
    SXRActivity *activity = reinterpret_cast<SXRActivity *>(appPtr);
    activity->onDrawFrame(env, jViewManager, mainScene);
}
    
    JNIEXPORT void JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeShowConfirmQuit(JNIEnv * jni, jclass clazz, jlong appPtr) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        activity->showConfirmQuit();
    }
    
    JNIEXPORT jint JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeInitializeVrApi(JNIEnv * jni, jclass clazz, jlong appPtr) {
        SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        return activity->initializeVrApi();
    }

    JNIEXPORT void JNICALL Java_com_samsungxr_OvrVrapiActivityHandler_nativeUninitializeVrApi(JNIEnv *, jclass) {
        SXRActivity::uninitializeVrApi();
    }
    
    JNIEXPORT jboolean JNICALL Java_com_samsungxr_OvrConfigurationManager_nativeIsHmtConnected(JNIEnv* jni, jclass clazz, jlong appPtr) {
        const SXRActivity *activity = reinterpret_cast<SXRActivity*>(appPtr);
        return activity->isHmtConnected();
    }
    
    extern "C"
    JNIEXPORT void JNICALL
    Java_com_samsungxr_OvrViewManager_recenterPose__J(JNIEnv*, jobject, jlong ptr) {
        const SXRActivity *activity = reinterpret_cast<SXRActivity*>(ptr);
        activity->recenterPose();
    }

JNIEXPORT void JNICALL
Java_com_samsungxr_OvrViewManager_initialize(JNIEnv *env, jobject instance, jlong aNative,
                                             jlong materialShaderManager,
                                             jlong postEffectRenderTextureA,
                                             jlong postEffectRenderTextureB) {
    SXRActivity *activity = reinterpret_cast<SXRActivity*>(aNative);
    activity->initialize(reinterpret_cast<ShaderManager *>(materialShaderManager),
                         reinterpret_cast<RenderTexture *>(postEffectRenderTextureA),
                         reinterpret_cast<RenderTexture *>(postEffectRenderTextureB));
}

} //extern "C" {
    
} //namespace sxr
