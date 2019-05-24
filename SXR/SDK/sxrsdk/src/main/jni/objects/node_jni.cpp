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

#include "node.h"

#include "util/sxr_log.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeNode_ctor(JNIEnv * env,
            jobject obj);
    JNIEXPORT jstring JNICALL
    Java_com_samsungxr_NativeNode_getName(JNIEnv * env,
            jobject obj, jlong jnode);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeNode_setName(JNIEnv * env,
            jobject obj, jlong jnode, jstring name);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_attachComponent(JNIEnv * env,
            jobject obj, jlong jnode, jlong jcomponent);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_detachComponent(JNIEnv * env,
            jobject obj, jlong jnode, jlong type);

    JNIEXPORT long JNICALL
    Java_com_samsungxr_NativeNode_findComponent(JNIEnv * env,
            jobject obj, jlong jnode, jlong type);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeNode_addChildObject(JNIEnv * env,
            jobject obj, jlong jnode, jlong jchild);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeNode_removeChildObject(
            JNIEnv * env, jobject obj, jlong jnode, jlong jchild);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_isColliding(
            JNIEnv * env, jobject obj, jlong jnode, jlong jother_object);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_isEnabled(
            JNIEnv * env, jobject obj, jlong jnode);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeNode_setEnable(
            JNIEnv * env, jobject obj, jlong jnode, bool flag);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_rayIntersectsBoundingVolume(JNIEnv * env,
            jobject obj, jlong jnode, jfloat rox, jfloat roy, jfloat roz,
            jfloat rdx, jfloat rdy, jfloat rdz);

    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativeNode_objectIntersectsBoundingVolume(
            JNIEnv * env, jobject obj, jlong jnode, jlong jother_object);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_NativeNode_getBoundingVolume(JNIEnv * env,
            jobject obj, jlong jNode);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_NativeNode_expandBoundingVolumeByPoint(JNIEnv * env,
            jobject obj, jlong jNode, jfloat pointX, jfloat pointY, jfloat pointZ);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_NativeNode_expandBoundingVolumeByCenterAndRadius(JNIEnv * env,
            jobject obj, jlong jNode, jfloat centerX, jfloat centerY, jfloat centerZ, jfloat radius);
} // extern "C"

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeNode_ctor(JNIEnv * env,
        jobject obj) {
    return reinterpret_cast<jlong>(new Node());
}

JNIEXPORT jstring JNICALL
Java_com_samsungxr_NativeNode_getName(JNIEnv * env,
        jobject obj, jlong jnode) {
    Node* node = reinterpret_cast<Node*>(jnode);
    std::string name = node->name();
    jstring jname = env->NewStringUTF(name.c_str());
    return jname;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeNode_setName(JNIEnv * env,
        jobject obj, jlong jnode, jstring name) {
    Node* node = reinterpret_cast<Node*>(jnode);
    const char* native_name = env->GetStringUTFChars(name, 0);
    node->set_name(std::string(native_name));
    env->ReleaseStringUTFChars(name, native_name);
}


JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_attachComponent(JNIEnv * env,
        jobject obj, jlong jnode, jlong jcomponent) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Component* component = reinterpret_cast<Component*>(jcomponent);
    return node->attachComponent(component);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_detachComponent(JNIEnv * env,
        jobject obj, jlong jnode, jlong type) {
    Node* node = reinterpret_cast<Node*>(jnode);
    return node->detachComponent(type) != NULL;
}


JNIEXPORT long JNICALL
Java_com_samsungxr_NativeNode_findComponent(JNIEnv * env,
        jobject obj, jlong jnode, jlong type) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Component* component = node->getComponent(type);
    return (long) component;
}


JNIEXPORT void JNICALL
Java_com_samsungxr_NativeNode_addChildObject(JNIEnv * env,
        jobject obj, jlong jnode, jlong jchild) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Node* child = reinterpret_cast<Node*>(jchild);
    node->addChildObject(node, child);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeNode_removeChildObject(
        JNIEnv * env, jobject obj, jlong jnode, jlong jchild) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Node* child = reinterpret_cast<Node*>(jchild);
    node->removeChildObject(child);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_isColliding(
        JNIEnv * env, jobject obj, jlong jnode, jlong jother_object) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Node* other_object = reinterpret_cast<Node*>(jother_object);
    return node->isColliding(other_object);
}


JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_isEnabled(
        JNIEnv * env, jobject obj, jlong jnode) {
    Node* node = reinterpret_cast<Node*>(jnode);
    return node->enabled();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeNode_setEnable(
        JNIEnv * env, jobject obj, jlong jnode, bool flag) {
    Node* node = reinterpret_cast<Node*>(jnode);
    node->set_enable(flag);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_rayIntersectsBoundingVolume(JNIEnv * env,
        jobject obj, jlong jnode, jfloat rox, jfloat roy, jfloat roz,
        jfloat rdx, jfloat rdy, jfloat rdz) {
    Node* node = reinterpret_cast<Node*>(jnode);
    return node->intersectsBoundingVolume(rox, roy, roz, rdx, rdy, rdz);
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativeNode_objectIntersectsBoundingVolume(
        JNIEnv * env, jobject obj, jlong jnode, jlong jother_object) {
    Node* node = reinterpret_cast<Node*>(jnode);
    Node* other_object = reinterpret_cast<Node*>(jother_object);
    return node->intersectsBoundingVolume(other_object);
}

jfloatArray boundingVolumeToArray(JNIEnv* env, const BoundingVolume& bvol) {
    jfloat temp[10];
    temp[0] = bvol.center().x;
    temp[1] = bvol.center().y;
    temp[2] = bvol.center().z;
    temp[3] = bvol.radius();
    temp[4] = bvol.min_corner().x;
    temp[5] = bvol.min_corner().y;
    temp[6] = bvol.min_corner().z;
    temp[7] = bvol.max_corner().x;
    temp[8] = bvol.max_corner().y;
    temp[9] = bvol.max_corner().z;

    jfloatArray result = env->NewFloatArray(10);
    env->SetFloatArrayRegion(result, 0, 10, temp);
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_samsungxr_NativeNode_getBoundingVolume(JNIEnv * env,
        jobject obj, jlong jNode) {
    Node* sceneObject = reinterpret_cast<Node*>(jNode);
    const BoundingVolume& bvol = sceneObject->getBoundingVolume();
    return boundingVolumeToArray(env, bvol);
}

JNIEXPORT jfloatArray JNICALL
Java_com_samsungxr_NativeNode_expandBoundingVolumeByPoint(JNIEnv * env,
        jobject obj, jlong jNode, jfloat pointX, jfloat pointY, jfloat pointZ) {

    Node* sceneObject = reinterpret_cast<Node*>(jNode);
    BoundingVolume& bvol = sceneObject->getBoundingVolume();
    bvol.expand(glm::vec3(pointX, pointY, pointZ));

    return boundingVolumeToArray(env, bvol);
}

JNIEXPORT jfloatArray JNICALL
Java_com_samsungxr_NativeNode_expandBoundingVolumeByCenterAndRadius(JNIEnv * env,
        jobject obj, jlong jNode, jfloat centerX, jfloat centerY, jfloat centerZ, jfloat radius) {

    Node* sceneObject = reinterpret_cast<Node*>(jNode);
    BoundingVolume& bvol = sceneObject->getBoundingVolume();
    bvol.expand(glm::vec3(centerX, centerY, centerZ), radius);

    return boundingVolumeToArray(env, bvol);
}

} // namespace sxr
