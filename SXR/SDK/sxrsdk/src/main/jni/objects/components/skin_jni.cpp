/***************************************************************************
 * JNI
 ***************************************************************************/

#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "skin.h"
#include "skeleton.h"
#include "util/sxr_jni.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_animation_NativeSkin_ctor(JNIEnv* env, jobject obj, jobject jskeleton);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_animation_NativeSkin_getComponentType(JNIEnv* env, jobject clz);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkin_setBoneMap(JNIEnv* env, jobject clz,
                                            jlong jskin, jintArray jboneMap);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkin_setSkeleton(JNIEnv* env, jobject clz,
                                                       jlong jskin, jlong jskel);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkin_setInverseBindPose(JNIEnv* env, jobject clz,
                                                       jlong jskin, jfloatArray jmatrices);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkin_getInverseBindPose(JNIEnv* env, jobject clz,
                                                       jlong jskin, jfloatArray jmatrices);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkin_scalePositions(JNIEnv* env, jobject clz,
                                                          jlong jskin, float sf);

} // extern "C"


JNIEXPORT jlong JNICALL
Java_com_samsungxr_animation_NativeSkin_ctor(JNIEnv * env, jobject clz, jobject jskeleton)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskeleton);
    return reinterpret_cast<jlong>(new Skin(*skel));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_animation_NativeSkin_getComponentType(JNIEnv * env, jobject clz)
{
    return Skin::getComponentType();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkin_setBoneMap(JNIEnv* env, jobject clz,
                                        jlong jskin, jintArray jboneMap)
{
    Skin* skin = reinterpret_cast<Skin*>(jskin);
    int n = env->GetArrayLength(jboneMap);
    jint* boneMap = env->GetIntArrayElements(jboneMap, JNI_FALSE);

    skin->setBoneMap(boneMap, n);
    env->ReleaseIntArrayElements(jboneMap, boneMap, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkin_setSkeleton(JNIEnv* env, jobject clz,
                                                    jlong jskin, jlong jskel)
{
    Skin* skin = reinterpret_cast<Skin*>(jskin);
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    skin->setSkeleton(skel);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkin_setInverseBindPose(JNIEnv* env, jobject clz,
                                                           jlong jskin, jfloatArray jmatrices)
{
    Skin* skin = reinterpret_cast<Skin*>(jskin);
    int n = env->GetArrayLength(jmatrices) * sizeof(float) / sizeof(glm::mat4);
    jfloat* matrices = env->GetFloatArrayElements(jmatrices, JNI_FALSE);
    skin->setInverseBindPose(matrices, n);
    env->ReleaseFloatArrayElements(jmatrices, matrices, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkin_getInverseBindPose(JNIEnv* env, jobject clz,
                                                           jlong jskin, jfloatArray jmatrices)
{
    Skin* skin = reinterpret_cast<Skin*>(jskin);
    int n = env->GetArrayLength(jmatrices) * sizeof(float) / sizeof(glm::mat4);
    jfloat* matrices = env->GetFloatArrayElements(jmatrices, JNI_FALSE);
    skin->setInverseBindPose(matrices, n);
    env->ReleaseFloatArrayElements(jmatrices, matrices, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkin_scalePositions(JNIEnv* env, jobject clz,
                                                  jlong jskin, float sf)
{
    Skin* skin = reinterpret_cast<Skin*>(jskin);
    skin->scalePositions(sf);
}

} // namespace sxr
