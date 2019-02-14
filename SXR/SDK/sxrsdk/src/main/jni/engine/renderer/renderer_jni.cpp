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
#include "vulkan/vulkan_headers.h"

namespace sxr {
extern "C" {
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderer_initialize(JNIEnv* env, jobject obj, jint token);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderer_reset(JNIEnv* env, jobject obj, jint token);

};

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderer_initialize(JNIEnv * env, jobject obj, jint token)
{
    Java_com_samsungxr_NativeRenderer_reset(env, obj, token);
    Renderer* r = Renderer::getInstance();
    JavaVM* jvm;
    if (r == nullptr)
    {
        r = Renderer::getInstance(token);
    }
    else if (r->getToken() != token)
    {
        Renderer::resetInstance();
        r = Renderer::getInstance(token);
    }
    env->GetJavaVM(&jvm);
    r->setJavaVM(jvm);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderer_reset(JNIEnv* env, jobject obj, jint token)
{
    Renderer* r = Renderer::getInstance();
    if (r && (r->getToken() == token))
    {
        Renderer::resetInstance();
    }
}

}
