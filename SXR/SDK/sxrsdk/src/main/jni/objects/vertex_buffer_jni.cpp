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
#include <glm/gtc/type_ptr.hpp>
#include "objects/vertex_buffer.h"

#include "util/sxr_log.h"

namespace sxr {
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeVertexBuffer_ctor(JNIEnv* env, jobject obj,
                                             jstring descriptor, int vertexCount);
    JNIEXPORT jintArray JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getIntArray(JNIEnv* env, jobject obj,
                                                    jlong jvbuf, jstring attribName);
    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_setIntArray(JNIEnv* env, jobject obj,
                                                    jlong jvbuf, jstring attribName,
                                                    jintArray data, jint stride, jint ofs);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_setIntVec(JNIEnv* env, jobject obj,
                                                  jlong jvbuf, jstring attribName,
                                                  jintArray data, jint stride, jint ofs);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getIntVec(JNIEnv* env, jobject obj,
                                                  jlong jvbuf, jstring attribName,
                                                  jobject jintbuf, jint stride, jint ofs);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getFloatVec(JNIEnv* env, jobject obj,
                                                    jlong jvbuf, jstring attribName,
                                                    jobject jfloatbuf, jint stride, jint ofs);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getFloatArray(JNIEnv * env, jobject obj,
                                                      jlong jvbuf, jstring attribName);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_setFloatArray(JNIEnv* env, jobject obj,
                                                    jlong jvbuf, jstring attribName,
                                                    jfloatArray data, jint stride, jint ofs);
    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_setFloatVec(JNIEnv* env, jobject obj,
                                                    jlong jvbuf, jstring attribName,
                                                    jobject jfloatbuf, jint stride, jint ofs);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeVertexBuffer_isSet(JNIEnv* env, jobject obj,
                                              jlong jvbuf, jstring attribName);
    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getVertexCount(JNIEnv* env, jobject obj,
                                                      jlong jvbuf);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getAttributeSize(JNIEnv* env, jobject obj,
                                                        jlong jvbuf, jstring attribName);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeVertexBuffer_getBoundingVolume(JNIEnv* env, jobject obj,
                                                         jlong jvbuf, jfloatArray outputArray);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeVertexBuffer_dump(JNIEnv* env, jobject obj,
                                                          jlong jvbuf, jstring attrName);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeVertexBuffer_transform(JNIEnv* env, jobject obj,
                                               jlong jvbuf, jfloatArray trans, bool doNormals);
    };

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeVertexBuffer_ctor(JNIEnv* env, jobject obj, jstring descriptor, int vertexCount)
{
    const char* char_desc = env->GetStringUTFChars(descriptor, 0);
    VertexBuffer* vbuf = Renderer::getInstance()->createVertexBuffer(char_desc, vertexCount);
    env->ReleaseStringUTFChars(descriptor, char_desc);
    if (vertexCount != vbuf->getVertexCount())
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
    }
    return reinterpret_cast<jlong>(vbuf);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_getFloatVec(JNIEnv* env, jobject obj,
                                                jlong jvbuf, jstring attribName,
                                                jobject jfloatbuf, jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    float* bufptr = (float*) env->GetDirectBufferAddress(jfloatbuf);
    int rc = 0;
    if (bufptr)
    {
        int capacity = env->GetDirectBufferCapacity(jfloatbuf);
        if (capacity > 0)
        {
            rc = vbuf->getFloatVec(char_key, bufptr + ofs, capacity - ofs, stride);
        }
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    return rc;
}

JNIEXPORT jfloatArray JNICALL
Java_com_samsungxr_NativeVertexBuffer_getFloatArray(JNIEnv * env, jobject obj,
                                                jlong jvbuf, jstring attribName)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    VertexBuffer::DataEntry* entry = vbuf->find(char_key);
    jfloatArray jdata = NULL;

    if (entry != NULL)
    {
        int n = (vbuf->getVertexCount() * entry->Size) / sizeof(float);
        jdata = env->NewFloatArray(n);
        if (jdata)
        {
            float* data = env->GetFloatArrayElements(jdata, 0);
            vbuf->getFloatVec(char_key, data, n, 0);
            env->ReleaseFloatArrayElements(jdata, data, 0);
        }
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    return jdata;
}

JNIEXPORT jintArray JNICALL
Java_com_samsungxr_NativeVertexBuffer_getIntArray(JNIEnv* env, jobject obj,
                                                jlong jvbuf, jstring attribName)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    VertexBuffer::DataEntry* entry = vbuf->find(char_key);
    jintArray jdata = NULL;

    if (entry != NULL)
    {
        int n = (vbuf->getVertexCount() * entry->Size) / sizeof(float);
        jdata = env->NewIntArray(n);
        if (jdata)
        {
            int* data = env->GetIntArrayElements(jdata, 0);
            vbuf->getIntVec(char_key, data, n, 0);
            env->ReleaseIntArrayElements(jdata, data, 0);
        }
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    return jdata;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_setIntArray(JNIEnv * env, jobject obj,
                                                jlong jvbuf, jstring attribName,
                                                jintArray jdata, jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    jint* attribData = env->GetIntArrayElements(jdata, 0);
    int n = static_cast<int>(env->GetArrayLength(jdata));
    int rc = 0;

    if (attribData && (n > 0))
    {
        rc = vbuf->setIntVec(char_key, attribData + ofs, n - ofs, stride);
    }
    env->ReleaseIntArrayElements(jdata, attribData, 0);
    env->ReleaseStringUTFChars(attribName, char_key);
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
        return -1;
    }
    return rc > 0;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_getIntVec(JNIEnv* env, jobject obj,
                                              jlong jvbuf, jstring attribName, jobject jintbuf,
                                              jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    int* bufptr = (int*) env->GetDirectBufferAddress(jintbuf);
    int capacity = env->GetDirectBufferCapacity(jintbuf) - ofs;
    int rc = 0;

    if (bufptr && (capacity > 0))
    {
        rc = vbuf->getIntVec(char_key, bufptr + ofs, capacity, stride);
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    return rc;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_setFloatArray(JNIEnv * env, jobject obj,
                                                jlong jvbuf, jstring attribName,
                                                jfloatArray jdata, jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    jfloat* attribData = env->GetFloatArrayElements(jdata, 0);
    int rc = 0;
    if (attribData)
    {
        rc = vbuf->setFloatVec(char_key, attribData + ofs,
                                   static_cast<int>(env->GetArrayLength(jdata)), stride);
        env->ReleaseFloatArrayElements(jdata, attribData, 0);
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
        return -1;
    }
    return rc > 0;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_setFloatVec(JNIEnv* env, jobject obj,
                                                jlong jvbuf, jstring attribName,
                                                jobject jfloatbuf, jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    float* bufptr = (float*) env->GetDirectBufferAddress(jfloatbuf);
    int rc = 0;
    if (bufptr)
    {
        int capacity = env->GetDirectBufferCapacity(jfloatbuf);
        if (capacity > 0)
        {
            rc = vbuf->setFloatVec(char_key, bufptr + ofs, capacity - ofs, stride);
        }
    }
    env->ReleaseStringUTFChars(attribName, char_key);
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
        return -1;
    }
    return rc;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_setIntVec(JNIEnv* env, jobject obj,
                                              jlong jvbuf, jstring attribName,
                                              jintArray jdata, jint stride, jint ofs)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    jint* attribData = env->GetIntArrayElements(jdata, 0);
    int n = static_cast<int>(env->GetArrayLength(jdata));
    int rc = 0;
    if (attribData && (n > 0))
    {
        rc = vbuf->setIntVec(char_key, attribData + ofs, n - ofs, stride);
        env->ReleaseStringUTFChars(attribName, char_key);
        env->ReleaseIntArrayElements(jdata, attribData, 0);
    }
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
        return -1;
    }
    return rc > 0;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeVertexBuffer_isSet(JNIEnv* env, jobject obj,
                                          jlong jvbuf, jstring attribName)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    bool rc = vbuf->isSet(char_key);
    env->ReleaseStringUTFChars(attribName, char_key);
    return rc;
}

JNIEXPORT int JNICALL
Java_com_samsungxr_NativeVertexBuffer_getVertexCount(JNIEnv* env, jobject obj, jlong jvbuf)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    return vbuf->getVertexCount();
}

JNIEXPORT int JNICALL
Java_com_samsungxr_NativeVertexBuffer_getAttributeSize(JNIEnv* env, jobject obj,
                                                     jlong jvbuf, jstring attribName)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attribName, 0);
    int size = vbuf->getByteSize(char_key) / sizeof(float);
    env->ReleaseStringUTFChars(attribName, char_key);
    return size;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeVertexBuffer_transform(JNIEnv* env, jobject obj,
                                                jlong jvbuf, jfloatArray jtrans, bool doNormals)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    jfloat* mtx = env->GetFloatArrayElements(jtrans, 0);
    glm::mat4 m = glm::make_mat4(mtx);
    vbuf->transform(m, doNormals);
    env->ReleaseFloatArrayElements(jtrans, mtx, JNI_ABORT);
}


JNIEXPORT int JNICALL
Java_com_samsungxr_NativeVertexBuffer_getBoundingVolume(JNIEnv* env, jobject,
                                                      jlong jvbuf, jfloatArray outputArray)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    int capacity = env->GetArrayLength(outputArray);

    if (capacity < 4)
    {
        LOGE("VertexBuffer::getBoundingVolume destination buffer must hold at least 4 floats");
        return -1;
    }
    BoundingVolume bv;
    jfloat* f = env->GetFloatArrayElements(outputArray, 0);
    vbuf->getBoundingVolume(bv);
    if (capacity == 4)
    {
        f[0] = bv.center().x;
        f[1] = bv.center().y;
        f[2] = bv.center().z;
        f[0] = bv.radius();
    }
    else if (capacity == 6)
    {
        f[0] = bv.min_corner().x;
        f[1] = bv.min_corner().y;
        f[2] = bv.min_corner().z;
        f[3] = bv.max_corner().x;
        f[4] = bv.max_corner().y;
        f[5] = bv.max_corner().z;
    }
    else if (capacity >= 10)
    {
        f[0] = bv.center().x;
        f[1] = bv.center().y;
        f[2] = bv.center().z;
        f[3] = bv.min_corner().x;
        f[4] = bv.min_corner().y;
        f[5] = bv.min_corner().z;
        f[6] = bv.max_corner().x;
        f[7] = bv.max_corner().y;
        f[8] = bv.max_corner().z;
        f[9] = bv.radius();
    }
    env->ReleaseFloatArrayElements(outputArray, f, 0);
    return (bv.radius() > 0) ? 1 : 0;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeVertexBuffer_dump(JNIEnv* env, jobject obj,
                                         jlong jvbuf, jstring attrName)
{
    VertexBuffer* vbuf = reinterpret_cast<VertexBuffer*>(jvbuf);
    const char* char_key = env->GetStringUTFChars(attrName, 0);
    vbuf->dump(char_key);
    env->ReleaseStringUTFChars(attrName, char_key);
}

}
