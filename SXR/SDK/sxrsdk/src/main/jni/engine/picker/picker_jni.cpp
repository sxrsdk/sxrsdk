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

#include <jni.h>
#include <objects/components/mesh_collider.h>
#include "picker.h"
#include "objects/scene.h"
#include "glm/gtc/type_ptr.hpp"

#include "util/sxr_log.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlongArray JNICALL
    Java_com_samsungxr_NativePicker_pickScene(JNIEnv * env,
                                            jobject obj, jlong jscene, jfloat ox, jfloat oy, jfloat z, jfloat dx,
                                            jfloat dy, jfloat dz);
    JNIEXPORT jobject JNICALL
    Java_com_samsungxr_NativePicker_pickClosest(JNIEnv * env,
                                            jobject obj, jlong jscene,
                                            jlong jtransform,
                                            jfloat ox, jfloat oy, jfloat oz,
                                            jfloat dx, jfloat dy, jfloat dz);
    JNIEXPORT jobjectArray JNICALL
    Java_com_samsungxr_NativePicker_pickBounds(JNIEnv * env,
                                          jobject obj, jlong jscene,
                                          jobject collidables);

    JNIEXPORT jobjectArray JNICALL
    Java_com_samsungxr_NativePicker_pickObjects(JNIEnv * env,
            jobject obj, jlong jscene, jlong jtransform, jfloat ox, jfloat oy, jfloat oz, jfloat dx,
            jfloat dy, jfloat dz);
    JNIEXPORT jobject JNICALL
    Java_com_samsungxr_NativePicker_pickNode(JNIEnv * env,
            jobject obj, jlong jnode, jfloat ox, jfloat oy, jfloat oz,
            jfloat dx, jfloat dy, jfloat dz);
    JNIEXPORT bool JNICALL
    Java_com_samsungxr_NativePicker_pickNodeAgainstBoundingBox(JNIEnv * env,
            jobject obj, jlong jnode, jfloat ox, jfloat oy, jfloat oz, jfloat dx,
            jfloat dy, jfloat dz, jobject jreadback_buffer);
    JNIEXPORT jobjectArray JNICALL
    Java_com_samsungxr_NativePicker_pickVisible(JNIEnv * env,
            jobject obj, jlong jscene);
}

JNIEXPORT jlongArray JNICALL
Java_com_samsungxr_NativePicker_pickScene(JNIEnv * env,
        jobject obj, jlong jscene, jfloat ox, jfloat oy, jfloat oz, jfloat dx,
        jfloat dy, jfloat dz) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    std::vector<ColliderData> colliders;
    Transform* t = scene->main_camera_rig()->getHeadTransform();
    if (nullptr == t) {
        return nullptr;
    }

    Picker::pickScene(scene, colliders, t, ox, oy, oz, dx, dy, dz);
    jlongArray jcolliders = env->NewLongArray(colliders.size());
    jlong* ptrArray = env->GetLongArrayElements(jcolliders, 0);
    jlong* ptrs = ptrArray;
    for (auto it = colliders.begin(); it != colliders.end(); ++it) {
        const ColliderData& data = *it;
        jlong collider = reinterpret_cast<jlong>(data.ColliderHit);
        *ptrs++ = collider;
    }
    env->ReleaseLongArrayElements(jcolliders, ptrArray, 0);
    return jcolliders;
}


JNIEXPORT jobjectArray JNICALL
Java_com_samsungxr_NativePicker_pickObjects(JNIEnv * env,
        jobject obj, jlong jscene, jlong jtransform, jfloat ox, jfloat oy, jfloat oz, jfloat dx,
        jfloat dy, jfloat dz)
{
    jclass pickerClass = env->FindClass("com/samsungxr/SXRPicker");
    jclass hitClass = env->FindClass("com/samsungxr/SXRPicker$SXRPickedObject");
    jmethodID makeHitMesh = env->GetStaticMethodID(pickerClass, "makeHitMesh", "(JFFFFIFFFFFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");
    jmethodID makeHit = env->GetStaticMethodID(pickerClass, "makeHit", "(JFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");

    Scene* scene = reinterpret_cast<Scene*>(jscene);
    std::vector<ColliderData> colliders;
    Transform* t = reinterpret_cast<Transform*>(jtransform);

    if (t == NULL) {
        t = scene->main_camera_rig()->getHeadTransform();
        if (nullptr == t) {
            return nullptr;
        }
    }
    Picker::pickScene(scene, colliders, t, ox, oy, oz, dx, dy, dz);

    int i = 0;
    int size = colliders.size();
    jobjectArray pickList = env->NewObjectArray(size, hitClass, NULL);

    for (auto it = colliders.begin(); it != colliders.end(); ++it)
    {
        const ColliderData& data = *it;
        jlong pointerCollider = reinterpret_cast<jlong>(data.ColliderHit);
        jobject hitObject;
        MeshCollider* meshCollider = (MeshCollider *) data.ColliderHit;
        if(meshCollider && meshCollider->shape_type() == COLLIDER_SHAPE_MESH && meshCollider->pickCoordinatesEnabled()) {
            hitObject = env->CallStaticObjectMethod(pickerClass, makeHitMesh, pointerCollider,
                                                    data.Distance,
                                                    data.HitPosition.x, data.HitPosition.y, data.HitPosition.z,
                                                    data.FaceIndex,
                                                    data.BarycentricCoordinates.x, data.BarycentricCoordinates.y, data.BarycentricCoordinates.z,
                                                    data.TextureCoordinates.x, data.TextureCoordinates.y,
                                                    data.NormalCoordinates.x, data.NormalCoordinates.y, data.NormalCoordinates.z);
        }
        else {
            hitObject = env->CallStaticObjectMethod(pickerClass, makeHit, pointerCollider,
                                                    data.Distance,
                                                    data.HitPosition.x, data.HitPosition.y, data.HitPosition.z);
        }
        if (hitObject != 0)
        {
            env->SetObjectArrayElement(pickList, i++, hitObject);
            env->DeleteLocalRef(hitObject);
        }
    }
    env->DeleteLocalRef(pickerClass);
    env->DeleteLocalRef(hitClass);
    return pickList;
}

JNIEXPORT jobject JNICALL
Java_com_samsungxr_NativePicker_pickClosest(JNIEnv * env,
                                          jobject obj, jlong jscene, jlong jtransform,
                                          jfloat ox, jfloat oy, jfloat oz,
                                          jfloat dx,  jfloat dy, jfloat dz)
{
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    Transform* t = reinterpret_cast<Transform*>(jtransform);
    ColliderData data;

    if (t == NULL)
    {
        t = scene->main_camera_rig()->getHeadTransform();
        if (nullptr == t) {
            return nullptr;
        }
    }
    Picker::pickClosest(scene, data, t, ox, oy, oz, dx, dy, dz);
    if (!data.IsHit)
    {
        return 0L;
    }
    jlong pointerCollider = reinterpret_cast<jlong>(data.ColliderHit);
    jobject hitObject;
    MeshCollider* meshCollider = (MeshCollider *) data.ColliderHit;
    jclass pickerClass = env->FindClass("com/samsungxr/SXRPicker");

    if (meshCollider &&
        (meshCollider->shape_type() == COLLIDER_SHAPE_MESH) &&
        meshCollider->pickCoordinatesEnabled())
    {
        jmethodID makeHitMesh = env->GetStaticMethodID(pickerClass, "makeHitMesh", "(JFFFFIFFFFFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");

        hitObject = env->CallStaticObjectMethod(pickerClass, makeHitMesh, pointerCollider,
                                                data.Distance,
                                                data.HitPosition.x, data.HitPosition.y, data.HitPosition.z,
                                                data.FaceIndex,
                                                data.BarycentricCoordinates.x, data.BarycentricCoordinates.y, data.BarycentricCoordinates.z,
                                                data.TextureCoordinates.x, data.TextureCoordinates.y,
                                                data.NormalCoordinates.x, data.NormalCoordinates.y, data.NormalCoordinates.z);
    }
    else
    {
        jmethodID makeHit = env->GetStaticMethodID(pickerClass, "makeHit", "(JFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");
        hitObject = env->CallStaticObjectMethod(pickerClass, makeHit, pointerCollider,
                                                data.Distance,
                                                data.HitPosition.x, data.HitPosition.y, data.HitPosition.z);
    }
    env->DeleteLocalRef(pickerClass);
    return hitObject;
}

JNIEXPORT jobjectArray JNICALL
Java_com_samsungxr_NativePicker_pickBounds(JNIEnv * env, jobject obj,
                                         jlong jscene,
                                         jobject jcollidables)
{
    jclass listClass = env->FindClass("java/util/List");
    jmethodID sizeMethod = env->GetMethodID(listClass, "size", "()I");
    int n = env->CallIntMethod(jcollidables, sizeMethod, jcollidables);
    int i = 0;

    if (n == 0)
    {
        return NULL;
    }
    jclass hybridClass = env->FindClass("com/samsungxr/SXRHybridObject");
    jmethodID getMethod = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
    jmethodID nativeMethod = env->GetMethodID(hybridClass, "getNative", "()J");
    Scene *scene = reinterpret_cast<Scene *>(jscene);
    std::vector<ColliderData> colliders;
    std::vector<Node *> collidables;

    for (i = 0; i < n; ++i)
    {
        jobject sceneObj = env->CallObjectMethod(jcollidables, getMethod, i);
        if (sceneObj != NULL)
        {
            Node* nativePtr = reinterpret_cast<Node*>
                    (env->CallLongMethod(sceneObj, nativeMethod));
            collidables.push_back(nativePtr);
        }
        else
        {
            LOGE("PICKER: ERROR: no collidable node index = %d", i);
            collidables.push_back(0L);
        }
    }
    env->DeleteLocalRef(listClass);
    env->DeleteLocalRef(hybridClass);
    Picker::pickBounds(scene, colliders, collidables);

    if (colliders.size() == 0)
    {
        return NULL;
    }
    jclass hitClass = env->FindClass("com/samsungxr/SXRPicker$SXRPickedObject");
    jclass pickerClass = env->FindClass("com/samsungxr/SXRBoundsPicker");
    jmethodID makeHit = env->GetStaticMethodID(pickerClass, "makeObjectHit", "(JIFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");
    jobjectArray pickList = env->NewObjectArray(colliders.size(), hitClass, NULL);

    i = 0;
    for (auto it = colliders.begin(); it != colliders.end(); ++it)
    {
        const ColliderData& data = *it;
        jlong pointerCollider = reinterpret_cast<jlong>(data.ColliderHit);
        if (pointerCollider)
        {
            jobject hitObject = env->CallStaticObjectMethod(pickerClass, makeHit, pointerCollider,
                                                            data.CollidableIndex, data.Distance,
                                                            data.HitPosition.x, data.HitPosition.y,
                                                            data.HitPosition.z);
            if (hitObject != 0)
            {
                env->SetObjectArrayElement(pickList, i, hitObject);
                env->DeleteLocalRef(hitObject);
            } else
            {
                LOGE("PICKER: ERROR: failed to make SXRPickedObject for collidable #%d",
                     data.CollidableIndex);
            }
        }
        ++i;
    }
    env->DeleteLocalRef(hitClass);
    env->DeleteLocalRef(pickerClass);
    return pickList;
}

JNIEXPORT jobject JNICALL
Java_com_samsungxr_NativePicker_pickNode(JNIEnv * env,
                                              jobject obj, jlong jnode,
                                              jfloat ox, jfloat oy, jfloat oz,
                                              jfloat dx, jfloat dy, jfloat dz) {
    jclass pickerClass = env->FindClass("com/samsungxr/SXRPicker");
    jmethodID makeHitMesh = env->GetStaticMethodID(pickerClass, "makeHitMesh", "(JFFFFIFFFFFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");
    jmethodID makeHit = env->GetStaticMethodID(pickerClass, "makeHit", "(JFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");

    Node* node = reinterpret_cast<Node*>(jnode);

    ColliderData data;
    Picker::pickNode(node, ox, oy, oz, dx, dy, dz, data);
    jlong pointerCollider = reinterpret_cast<jlong>(data.ColliderHit);
    jobject hitObject;
    MeshCollider* meshCollider = (MeshCollider *) data.ColliderHit;
    if(meshCollider && meshCollider->shape_type() == COLLIDER_SHAPE_MESH && meshCollider->pickCoordinatesEnabled()) {
        hitObject = env->CallStaticObjectMethod(pickerClass, makeHitMesh, pointerCollider,
                                                data.Distance,
                                                data.HitPosition.x, data.HitPosition.y, data.HitPosition.z,
                                                data.FaceIndex,
                                                data.BarycentricCoordinates.x, data.BarycentricCoordinates.y, data.BarycentricCoordinates.z,
                                                data.TextureCoordinates.x, data.TextureCoordinates.y,
                                                data.NormalCoordinates.x, data.NormalCoordinates.y, data.NormalCoordinates.z);
    }
    else {
        hitObject = env->CallStaticObjectMethod(pickerClass, makeHit, pointerCollider,
                                                data.Distance,
                                                data.HitPosition.x, data.HitPosition.y, data.HitPosition.z);
    }

    env->DeleteLocalRef(pickerClass);
    return hitObject;
}

JNIEXPORT bool JNICALL
Java_com_samsungxr_NativePicker_pickNodeAgainstBoundingBox(JNIEnv * env,
        jobject obj, jlong jnode,  jfloat ox, jfloat oy, jfloat oz, jfloat dx,
        jfloat dy, jfloat dz, jobject jreadback_buffer) {
    Node* node =
            reinterpret_cast<Node*>(jnode);
    float *data = (float *) env->GetDirectBufferAddress(jreadback_buffer);
    glm::vec3 hit =  Picker::pickNodeAgainstBoundingBox(node,
            ox, oy, oz,  dx, dy, dz);

    if (hit == glm::vec3(std::numeric_limits<float>::infinity())){
        return false;
    }
    jsize size = sizeof(hit) / sizeof(jfloat);
    if (size != 3) {
        LOGE("sizeof(hit) / sizeof(jfloat) != 3");
        throw "sizeof(hit) / sizeof(jfloat) != 3";
    }

    data[0]=hit.x;
    data[1]=hit.y;
    data[2]=hit.z;
    return true;
}

JNIEXPORT jobjectArray JNICALL
Java_com_samsungxr_NativePicker_pickVisible(JNIEnv * env,
        jobject obj, jlong jscene)
{
    jclass pickerClass = env->FindClass("com/samsungxr/SXRPicker");
    jclass hitClass = env->FindClass("com/samsungxr/SXRPicker$SXRPickedObject");
    jmethodID makeHitMesh = env->GetStaticMethodID(pickerClass, "makeHitMesh", "(JFFFFIFFFFFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");
    jmethodID makeHit = env->GetStaticMethodID(pickerClass, "makeHit", "(JFFFF)Lcom/samsungxr/SXRPicker$SXRPickedObject;");

    Scene* scene = reinterpret_cast<Scene*>(jscene);
    std::vector<ColliderData> colliders;
    Transform* t = scene->main_camera_rig()->getHeadTransform();

    Picker::pickVisible(scene, t, colliders);

    int i = 0;
    int size = colliders.size();
    jobjectArray pickList = env->NewObjectArray(size, hitClass, NULL);

    for (auto it = colliders.begin(); it != colliders.end(); ++it)
    {
        const ColliderData& data = *it;
        jlong pointerCollider = reinterpret_cast<jlong>(data.ColliderHit);
        jobject hitObject;
        MeshCollider* meshCollider = (MeshCollider *) data.ColliderHit;
        if(meshCollider && meshCollider->shape_type() == COLLIDER_SHAPE_MESH && meshCollider->pickCoordinatesEnabled()) {
            hitObject = env->CallStaticObjectMethod(pickerClass, makeHitMesh, pointerCollider,
                                                    data.Distance,
                                                    data.HitPosition.x, data.HitPosition.y, data.HitPosition.z,
                                                    data.FaceIndex,
                                                    data.BarycentricCoordinates.x, data.BarycentricCoordinates.y, data.BarycentricCoordinates.z,
                                                    data.TextureCoordinates.x, data.TextureCoordinates.y,
                                                    data.NormalCoordinates.x, data.NormalCoordinates.y, data.NormalCoordinates.z);
        }
        else {
            hitObject = env->CallStaticObjectMethod(pickerClass, makeHit, pointerCollider,
                                                    data.Distance,
                                                    data.HitPosition.x, data.HitPosition.y, data.HitPosition.z);
        }
        if (hitObject != 0)
        {
            env->SetObjectArrayElement(pickList, i++, hitObject);
            env->DeleteLocalRef(hitObject);
        }
    }
    env->DeleteLocalRef(pickerClass);
    env->DeleteLocalRef(hitClass);
    return pickList;
}

}
