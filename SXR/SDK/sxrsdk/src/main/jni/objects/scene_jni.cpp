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

#include "scene.h"
#include "objects/light.h"
#include "util/sxr_jni.h"

namespace sxr {
extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeScene_ctor(JNIEnv * env, jobject obj);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setJava(JNIEnv *env, jclass, jlong nativeScene, jobject javaScene);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_removeNode(JNIEnv * env,
            jobject obj, jlong jscene, jlong jscene_object);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_removeAllNodes(JNIEnv * env,
            jobject obj, jlong jscene);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setMainCameraRig(JNIEnv * env,
            jobject obj, jlong jscene, jlong jcamera_rig);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setFrustumCulling(JNIEnv * env,
            jobject obj, jlong jscene, jboolean flag);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setPickVisible(JNIEnv * env,
            jobject obj, jlong jscene, jboolean flag);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setOcclusionQuery(JNIEnv * env,
            jobject obj, jlong jscene, jboolean flag);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_resetStats(JNIEnv * env,
            jobject obj, jlong jscene);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeScene_getNumberDrawCalls(JNIEnv * env,
            jobject obj, jlong jscene);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_exportToFile(JNIEnv * env,
            jobject obj, jlong jscene, jstring file_path);

    JNIEXPORT int JNICALL
    Java_com_samsungxr_NativeScene_getNumberTriangles(JNIEnv * env,
            jobject obj, jlong jscene);

    JNIEXPORT jobjectArray JNICALL
    Java_com_samsungxr_NativeScene_getLightList(JNIEnv* env, jobject obj, jlong scene);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_addCollider(JNIEnv * env,
            jobject obj, jlong jscene, jlong jcollider);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setMainScene(JNIEnv * env, jobject obj, jlong jscene);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_deleteLightsAndDepthTextureOnRenderThread(JNIEnv * env,
                                                       jobject obj, jlong jscene) {
        Scene* scene = reinterpret_cast<Scene*>(jscene);
        scene->clearLights();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeScene_setSceneRoot(JNIEnv *env, jclass type, jlong scene, jlong sceneRoot) {
        Scene* aScene = reinterpret_cast<Scene*>(scene);
        Node* aSceneRoot = reinterpret_cast<Node*>(sceneRoot);
        aScene->setSceneRoot(aSceneRoot);
    }

};

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeScene_ctor(JNIEnv* env, jobject obj) {
    return reinterpret_cast<jlong>(new Scene());
}


JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_removeNode(JNIEnv * env,
        jobject obj, jlong jscene, jlong jscene_object) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    Node* scene_object = reinterpret_cast<Node*>(jscene_object);
    scene->removeNode(scene_object);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_removeAllNodes(JNIEnv * env,
        jobject obj, jlong jscene) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->removeAllNodes();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_setMainCameraRig(JNIEnv * env,
        jobject obj, jlong jscene, jlong jcamera_rig) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    CameraRig* camera_rig = reinterpret_cast<CameraRig*>(jcamera_rig);
    scene->set_main_camera_rig(camera_rig);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_setFrustumCulling(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->set_frustum_culling(static_cast<bool>(flag));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_setPickVisible(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->setPickVisible(static_cast<bool>(flag));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_setOcclusionQuery(JNIEnv * env,
        jobject obj, jlong jscene, jboolean flag) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->set_occlusion_culling(static_cast<bool>(flag));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_resetStats(JNIEnv * env,
        jobject obj, jlong jscene) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    scene->resetStats();
}


JNIEXPORT int JNICALL
Java_com_samsungxr_NativeScene_getNumberDrawCalls(JNIEnv * env,
        jobject obj, jlong jscene) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    return scene->getNumberDrawCalls();
}


JNIEXPORT int JNICALL
Java_com_samsungxr_NativeScene_getNumberTriangles(JNIEnv * env,
        jobject obj, jlong jscene) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    return scene->getNumberTriangles();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_exportToFile(JNIEnv * env,
        jobject obj, jlong jscene, jstring filepath) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    const char* native_filepath = env->GetStringUTFChars(filepath, 0);

    scene->exportToFile(std::string(native_filepath));

    env->ReleaseStringUTFChars(filepath, native_filepath);
}

JNIEXPORT jobjectArray JNICALL
Java_com_samsungxr_NativeScene_getLightList(JNIEnv* env, jobject obj, jlong jscene)
{
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    std::vector<Light*> lights;
    int nlights = scene->getLights().getLights(lights);
    jclass elemClass = env->FindClass("com/samsungxr/SXRLight");

    jobjectArray jlights = env->NewObjectArray(nlights, elemClass, NULL);
    int i = 0;
    for (auto it = lights.begin(); it != lights.end(); ++it)
    {
        jobject obj = (*it)->get_java(env);
        if (nullptr != obj)
        {
            env->SetObjectArrayElement(jlights, i++, obj);
        }
    }
    env->DeleteLocalRef(elemClass);
    return jlights;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_addCollider(JNIEnv * env,
        jobject obj, jlong jscene, jlong jcollider) {
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    Collider* collider = reinterpret_cast<Collider*>(jcollider);
    scene->addCollider(collider);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeScene_setMainScene(JNIEnv * env, jobject obj, jlong jscene) {
    Scene::set_main_scene(reinterpret_cast<Scene*>(jscene));
}

}
