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
#include "objects/components/shadow_map.h"

namespace sxr
{
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeShadowMap_ctor(JNIEnv *env, jobject obj, jlong jmaterial);
    };

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeShadowMap_ctor(JNIEnv *env, jobject obj, jlong jmaterial)
    {
        ShaderData* material = reinterpret_cast<ShaderData*>(jmaterial);
        return reinterpret_cast<jlong>(new ShadowMap(material));
    }

}
