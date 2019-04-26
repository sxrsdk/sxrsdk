/* Copyright 2016 Samsung Electronics Co., LTD
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

#include "jni_utils.h"
#include "util/sxr_log.h"

namespace sxr {

JNIEnv* getCurrentEnv(JavaVM *javaVm) {
    JNIEnv* result;
    if (JNI_OK != javaVm->GetEnv(reinterpret_cast<void**>(&result), JNI_VERSION_1_6)) {
        FAIL("GetEnv failed");
    }
    return result;
}

jclass GetGlobalClassReference(JNIEnv &env, const char *className) {
    jclass lc = env.FindClass(className);
    if (0 == lc) {
        FAIL("unable to find class %s", className);
    }
    // Turn it into a global ref, so we can safely use it in the VR thread
    jclass gc = static_cast<jclass>(env.NewGlobalRef(lc));
    env.DeleteLocalRef(lc);

    return gc;
}

jmethodID GetMethodId(JNIEnv &env, const jclass clazz, const char *name, const char *signature) {
    const jmethodID mid = env.GetMethodID(clazz, name, signature);
    if (nullptr == mid) {
        FAIL("unable to find method %s", name);
    }
    return mid;
}

jmethodID GetStaticMethodID(JNIEnv &env, jclass clazz, const char *name, const char *signature) {
    jmethodID mid = env.GetStaticMethodID(clazz, name, signature);
    if (!mid) {
        FAIL("unable to find static method %s", name);
    }
    return mid;
}

jint throwOutOfMemoryError(JNIEnv* env, const char *message)
{
    jclass exClass = env->FindClass("java/lang/OutOfMemoryError");

    if (exClass != NULL)
    {
        return env->ThrowNew(exClass, message);
    }
    return -1;
}

}