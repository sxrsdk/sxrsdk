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


/***************************************************************************
 * JNI
 ***************************************************************************/

#include <engine/renderer/renderer.h>
#include "objects/index_buffer.h"

#include "util/jni_utils.h"
#include "util/sxr_log.h"

namespace sxr {
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeIndexBuffer_ctor(JNIEnv* env, jobject obj,
                                            int bytesPerIndex, int vertexCount);
    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getIntVec(JNIEnv* env, jobject obj, jlong jibuf, jobject data);

    JNIEXPORT jintArray JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getIntArray(JNIEnv* env, jobject obj, jlong jibuf);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_setIntArray(JNIEnv* env, jobject obj, jlong jibuf, jintArray data);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_setIntVec(JNIEnv* env, jobject obj, jlong jibuf, jobject jintbuf);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getShortVec(JNIEnv* env, jobject obj, jlong jibuf, jobject jshortbuf);

    JNIEXPORT jcharArray JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getShortArray(JNIEnv* env, jobject obj, jlong jibuf);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_setShortArray(JNIEnv* env, jobject obj, jlong jibuf, jcharArray data);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeIndexBuffer_setShortVec(JNIEnv* env, jobject obj, jlong jibuf, jobject jshortbuf);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getIndexSize(JNIEnv* env, jobject obj, jlong jibuf);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeIndexBuffer_getIndexCount(JNIEnv* env, jobject obj, jlong jibuf);

};

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeIndexBuffer_ctor(JNIEnv* env, jobject obj, int bytesPerVertex, int indexCount)
{
    IndexBuffer* ibuf = Renderer::getInstance()->createIndexBuffer(bytesPerVertex, indexCount);
    if (indexCount != ibuf->getIndexCount())
    {
        throwOutOfMemoryError(env, "Cannot allocate index buffer");
    }
    return reinterpret_cast<jlong>(ibuf);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_getShortVec(JNIEnv * env, jobject obj, jlong jibuf, jobject jshortbuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    void* bufptr = env->GetDirectBufferAddress(jshortbuf);
    int capacity = env->GetDirectBufferCapacity(jshortbuf);
    if (bufptr && (capacity > 0))
    {
        return ibuf->getShortVec((unsigned short*) bufptr, capacity);
    }
    return false;
}


JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_getIntVec(JNIEnv * env, jobject obj, jlong jibuf, jobject jdata)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    void* bufptr = env->GetDirectBufferAddress(jdata);
    int capacity = env->GetDirectBufferCapacity(jdata);
    if (bufptr && (capacity > 0))
    {
        return ibuf->getIntVec((unsigned int*) bufptr, capacity);
    }
    return false;
}

JNIEXPORT jintArray JNICALL
Java_com_samsungxr_NativeIndexBuffer_getIntArray(JNIEnv* env, jobject obj, jlong jibuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    int n = ibuf->getIndexCount();
    jintArray jdata = env->NewIntArray(n);
    unsigned int* data = reinterpret_cast<unsigned int*>(env->GetIntArrayElements(jdata, 0));
    if (data && (n > 0))
    {
        ibuf->getIntVec(data, n);
    }
    env->ReleaseIntArrayElements(jdata, reinterpret_cast<jint*>(data), 0);
    return jdata;
}

JNIEXPORT jcharArray JNICALL
Java_com_samsungxr_NativeIndexBuffer_getShortArray(JNIEnv* env, jobject obj, jlong jibuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    int n = ibuf->getIndexCount();
    jcharArray jdata = env->NewCharArray(n);
    unsigned short* data = reinterpret_cast<unsigned short*>(env->GetCharArrayElements(jdata, 0));
    if (data && (n > 0))
    {
        ibuf->getShortVec(data, n);
    }
    env->ReleaseCharArrayElements(jdata, reinterpret_cast<jchar*>(data), 0);
    return jdata;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_setShortArray(JNIEnv * env, jobject obj, jlong jibuf, jcharArray jdata)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    jchar* data = env->GetCharArrayElements(jdata, 0);
    int n = static_cast<int>(env->GetArrayLength(jdata));
    int rc = 0;
    if (data && (n > 0))
    {
        rc = ibuf->setShortVec(data, n);
    }
    env->ReleaseCharArrayElements(jdata, data, 0);
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate index buffer");
    }
    return rc;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_setShortVec(JNIEnv * env, jobject obj, jlong jibuf, jobject jshortbuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    void* bufptr = env->GetDirectBufferAddress(jshortbuf);
    int rc = 0;
    if (bufptr)
    {
        jlong capacity = env->GetDirectBufferCapacity(jshortbuf);
        rc = ibuf->setShortVec((unsigned short*) bufptr, capacity);
    }
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate index buffer");
    }
    return rc;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_setIntVec(JNIEnv* env, jobject obj, jlong jibuf, jobject jintbuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    void* bufptr = env->GetDirectBufferAddress(jintbuf);
    int rc = 0;
    if (bufptr)
    {
        jlong capacity = env->GetDirectBufferCapacity(jintbuf);
        rc = ibuf->setIntVec((unsigned int*) bufptr, capacity);
    }
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate vertex buffer");
        return -1;
    }
    return rc > 0;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeIndexBuffer_setIntArray(JNIEnv * env, jobject obj, jlong jibuf, jintArray jdata)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    unsigned int* data = reinterpret_cast<unsigned int*>(env->GetIntArrayElements(jdata, 0));
    int rc = 0;
    int n =  static_cast<int>(env->GetArrayLength(jdata));

    if (data && (n > 0))
    {
        rc = ibuf->setIntVec(data, n);
    }
    env->ReleaseIntArrayElements(jdata, reinterpret_cast<jint*>(data), 0);
    if (rc < 0)
    {
        throwOutOfMemoryError(env, "Cannot allocate index buffer");
    }
    return rc > 0;
}


JNIEXPORT int JNICALL
Java_com_samsungxr_NativeIndexBuffer_getIndexCount(JNIEnv* env, jobject obj, jlong jibuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    return ibuf->getIndexCount();
}

JNIEXPORT int JNICALL
Java_com_samsungxr_NativeIndexBuffer_getIndexSize(JNIEnv* env, jobject obj, jlong jibuf)
{
    IndexBuffer* ibuf = reinterpret_cast<IndexBuffer*>(jibuf);
    return ibuf->getIndexSize();
}

}
