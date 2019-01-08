/***************************************************************************
 * JNI
 ***************************************************************************/

#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "objects/components/skeleton.h"
#include "util/sxr_jni.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_ctor(JNIEnv* env, jobject obj, jintArray boneparents);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_getComponentType(JNIEnv* env, jobject clz);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_setPose(JNIEnv* env, jobject clz,
                                                        jlong jskel, jfloatArray jmatrices);
    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_setWorldPose(JNIEnv* env, jobject clz,
                                                            jlong jskel, jfloatArray jmatrices);
    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_getPose(JNIEnv* env, jobject clz,
                                                      jlong jskel, jfloatArray jmatrices);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_setBoneName(JNIEnv* env, jobject clz,
                                                            jlong jskel, jint index, jstring name);
    JNIEXPORT jstring JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_getBoneName(JNIEnv* env, jobject clz,
                                                           jlong jskel, jint index);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_getNumBones(JNIEnv* env, jobject clz,
                                                            jlong jskel);
    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_getBoneParents(JNIEnv* env, jobject clz,
                                                                jlong jskel, jintArray parents);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_animation_NativeSkeleton_updateBones(JNIEnv* env, jobject obj,
                                                            jlong jskel, jintArray boneparents, jobjectArray bonenames);


} // extern "C"


JNIEXPORT jlong JNICALL
Java_com_samsungxr_animation_NativeSkeleton_ctor(JNIEnv * env, jobject clz, jintArray jboneparents)
{
    jint numbones = env->GetArrayLength(jboneparents);
    jint* boneParents = env->GetIntArrayElements(jboneparents, JNI_FALSE);
    Skeleton* skel = new Skeleton(boneParents, numbones);
    env->ReleaseIntArrayElements(jboneparents, boneParents, JNI_ABORT);
    return reinterpret_cast<jlong>(skel);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_animation_NativeSkeleton_getComponentType(JNIEnv * env, jobject clz)
{
    return Skeleton::getComponentType();
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_animation_NativeSkeleton_setPose(JNIEnv* env, jobject clz,
                                        jlong jskel, jfloatArray jmatrices)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    int n = env->GetArrayLength(jmatrices);

    if (skel->getNumBones() != (n / 16))
    {
        return false;
    }
    jfloat* inputMatrices = env->GetFloatArrayElements(jmatrices, JNI_FALSE);

    skel->setPose(inputMatrices);
    env->ReleaseFloatArrayElements(jmatrices, inputMatrices, JNI_ABORT);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_animation_NativeSkeleton_getPose(JNIEnv* env, jobject clz,
                                                  jlong jskel, jfloatArray jmatrices)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    int n = env->GetArrayLength(jmatrices);

    if (skel->getNumBones() != (n / 16))
    {
        return false;
    }
    jfloat* inputMatrices = env->GetFloatArrayElements(jmatrices, JNI_FALSE);

    skel->getPose(inputMatrices);
    env->ReleaseFloatArrayElements(jmatrices, inputMatrices, 0);
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_animation_NativeSkeleton_setWorldPose(JNIEnv* env, jobject clz,
                                                  jlong jskel, jfloatArray jmatrices)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    int n = env->GetArrayLength(jmatrices);

    if (skel->getNumBones() != (n / 16))
    {
        return false;
    }
    jfloat* inputMatrices = env->GetFloatArrayElements(jmatrices, JNI_FALSE);

    skel->setWorldPose(inputMatrices);
    env->ReleaseFloatArrayElements(jmatrices, inputMatrices, JNI_ABORT);
    return true;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkeleton_setBoneName(JNIEnv* env, jobject clz,
                                                      jlong jskel, jint index, jstring name)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    const char* boneName = env->GetStringUTFChars(name, 0);
    skel->setBoneName(index, boneName);
    env->ReleaseStringUTFChars(name, boneName);
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_animation_NativeSkeleton_getBoneParents(JNIEnv* env, jobject clz,
                                                         jlong jskel, jintArray jparentIds)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    int numBones = env->GetArrayLength(jparentIds);
    if (numBones != skel->getNumBones())
    {
        return false;
    }
    jint* parentIds = env->GetIntArrayElements(jparentIds, 0);
    const int* boneParents = skel->getBoneParents();
    memcpy(parentIds, boneParents, numBones * sizeof(int));
    env->ReleaseIntArrayElements(jparentIds, parentIds, 0);
    return true;
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_animation_NativeSkeleton_getNumBones(JNIEnv* env, jobject clz, jlong jskel)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    return skel->getNumBones();
}

JNIEXPORT jstring JNICALL
Java_com_samsungxr_animation_NativeSkeleton_getBoneName(JNIEnv* env, jobject clz,
                                                      jlong jskel, jint index)
{
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    const char* name = skel->getBoneName(index);
    return env->NewStringUTF(name);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_animation_NativeSkeleton_updateBones(JNIEnv* env, jobject obj,
                                                        jlong jskel, jintArray jboneparents, jobjectArray jbonenames)
{
    jint numbones = env->GetArrayLength(jboneparents);
    jint* boneParents = env->GetIntArrayElements(jboneparents, JNI_FALSE);
    Skeleton* skel = reinterpret_cast<Skeleton*>(jskel);
    const char* names[numbones];

    for (int i = 0; i < numbones; ++i)
    {
        jstring str = (jstring) env->GetObjectArrayElement(jbonenames, i);
        names[i] = env->GetStringUTFChars(str, JNI_FALSE);
    }
    skel->updateBones(boneParents, names, numbones);
    for (int i = 0; i < numbones; ++i)
    {
        jstring str = (jstring) env->GetObjectArrayElement(jbonenames, i);
        env->ReleaseStringUTFChars(str, names[i]);
    }
    env->ReleaseIntArrayElements(jboneparents, boneParents, JNI_ABORT);
}


}