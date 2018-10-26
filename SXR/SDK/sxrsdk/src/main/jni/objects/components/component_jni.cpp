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


#include "component.h"
#include "component.inl"
#include "util/sxr_jni.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeComponent_getType(JNIEnv * env,
            jobject obj, jlong jcomponent);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeComponent_setOwnerObject(JNIEnv * env,
            jobject obj, jlong jcomponent, jlong jowner);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeComponent_isEnabled(JNIEnv * env, jobject obj, jlong jcomponent);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeComponent_setEnable(JNIEnv * env, jobject obj, jlong jlight, jboolean flag);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeComponent_addChildComponent(JNIEnv * env, jobject obj,
                                                       jlong jgroup, jlong jcomponent);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeComponent_removeChildComponent(JNIEnv * env, jobject obj,
                                                          jlong jgroup, jlong jcomponent);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeComponent_getType(JNIEnv * env,
        jobject obj, jlong jcomponent)
{
    Component* component = reinterpret_cast<Component*>(jcomponent);
    long long type = component->getType();
    return type;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeComponent_setOwnerObject(JNIEnv * env,
        jobject obj, jlong jcomponent, jlong jowner)
{
    Component* component = reinterpret_cast<Component*>(jcomponent);
    Node* owner = reinterpret_cast<Node*>(jowner);
    component->set_owner_object(owner);
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeComponent_isEnabled(JNIEnv * env, jobject obj, jlong jcomponent)
{
    Component* component = reinterpret_cast<Component*>(jcomponent);
    return component->enabled();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeComponent_setEnable(JNIEnv * env, jobject obj, jlong jcomponent, jboolean flag)
{
    Component* component = reinterpret_cast<Component*>(jcomponent);
    component->set_enable((bool) flag);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeComponent_addChildComponent(JNIEnv * env, jobject obj,
                                                   jlong jgroup, jlong jcomponent)
{
    Component* group = reinterpret_cast<Component*>(jgroup);
    Component* component = reinterpret_cast<Component*>(jcomponent);
    group->addChildComponent(component);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeComponent_removeChildComponent(JNIEnv * env, jobject obj,
                                                      jlong jgroup, jlong jcomponent)
{
    Component* group = reinterpret_cast<Component*>(jgroup);
    Component* component = reinterpret_cast<Component*>(jcomponent);
    group->removeChildComponent(component);
}
}

