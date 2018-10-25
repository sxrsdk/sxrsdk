/***************************************************************************
 * JNI
 ***************************************************************************/

#include <engine/renderer/renderer.h>
#include "external_image.h"

#include "util/sxr_jni.h"

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
