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
#include "objects/components/render_target.h"

namespace sxr {

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctorMultiview(JNIEnv *env, jobject obj, jlong jtexture, jboolean isMultiview);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctor(JNIEnv *env, jobject obj, jlong jtexture,  jlong ptr);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_getComponentType(JNIEnv *env, jobject obj);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setTexture(JNIEnv *env, jobject obj, jlong ptr, jlong texture);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setMainScene(JNIEnv *env, jobject obj, jlong ptr, jlong Sceneptr);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setCamera(JNIEnv *env, jobject obj, jlong ptr, jlong camera);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_beginRendering(JNIEnv *env, jobject obj, jlong ptr, jlong camera);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_endRendering(JNIEnv *env, jobject obj, jlong ptr);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_defaultCtor(JNIEnv *env, jobject obj, jlong jscene);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctorViewport(JNIEnv *env, jobject obj, jlong jscene,
                                                     jint defaultViewportW, jint defaultViewportH);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_cullFromCamera(JNIEnv *env, jobject obj, jlong jscene, jobject javaNode, jlong ptr, jlong jcamera, jlong jshaderManager);
    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_render(JNIEnv *env, jobject obj, jlong renderTarget, jlong camera,
                                               jlong shader_manager, jlong posteffectrenderTextureA, jlong posteffectRenderTextureB, jlong jscene,
                                               jobject javaNode);
};

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_render(JNIEnv *env, jobject obj, jlong renderTarget,
                                           jlong camera,
                                           jlong shader_manager, jlong posteffectrenderTextureA,
                                           jlong posteffectRenderTextureB, jlong jscene, jobject javaNode) {
    RenderTarget* target = reinterpret_cast<RenderTarget*>(renderTarget);
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    // Do not remote this: need it for screenshot capturer, center camera rendering
    target->setCamera(reinterpret_cast<Camera*>(camera));
    javaNode = env->NewLocalRef(javaNode);
    gRenderer->getInstance()->renderRenderTarget(scene, javaNode, target, reinterpret_cast<ShaderManager*>(shader_manager),
                                                 reinterpret_cast<RenderTexture*>(posteffectrenderTextureA),
                                                 reinterpret_cast<RenderTexture*>(posteffectRenderTextureB),
                                                 target->getRenderDataVector());
    env->DeleteLocalRef(javaNode);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_defaultCtor(JNIEnv *env, jobject obj, jlong jscene){
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(scene));

}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctorViewport(JNIEnv *env, jobject obj, jlong jscene,
                                                 jint defaultViewportW, jint defaultViewportH){
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(scene, defaultViewportW,
                                                                               defaultViewportH));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctorMultiview(JNIEnv *env, jobject obj, jlong jtexture, jboolean isMultiview)
{

    RenderTexture* texture = reinterpret_cast<RenderTexture*>(jtexture);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(texture, isMultiview));
}
JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctor(JNIEnv *env, jobject obj, jlong jtexture, jlong ptr)
{
    RenderTexture* texture = reinterpret_cast<RenderTexture*>(jtexture);
    RenderTarget* sourceRenderTarget = reinterpret_cast<RenderTarget*>(ptr);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(texture, sourceRenderTarget));
}
JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_setMainScene(JNIEnv *env, jobject obj, jlong ptr, jlong Sceneptr){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    Scene* scene = reinterpret_cast<Scene*>(Sceneptr);
    target->setMainScene(scene);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_beginRendering(JNIEnv *env, jobject obj, jlong ptr, jlong jcamera){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    target->setCamera(camera);
    target->beginRendering(gRenderer->getInstance());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_endRendering(JNIEnv *env, jobject obj, jlong ptr){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    target->endRendering(gRenderer->getInstance());
}


JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_cullFromCamera(JNIEnv *env, jobject obj, jlong jscene, jobject javaNode, jlong ptr, jlong jcamera, jlong jshaderManager){

    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    ShaderManager* shaderManager = reinterpret_cast<ShaderManager*> (jshaderManager);
    javaNode = env->NewLocalRef(javaNode);
    target->cullFromCamera(scene, javaNode, camera,gRenderer->getInstance(), shaderManager);
    env->DeleteLocalRef(javaNode);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_setTexture(JNIEnv *env, jobject obj, jlong ptr, jlong jtexture)
{
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    RenderTexture* texture = reinterpret_cast<RenderTexture*>(jtexture);
    target->setTexture(texture);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_setCamera(JNIEnv *env, jobject obj, jlong ptr, jlong jcamera)
{
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    target->setCamera(camera);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_getComponentType(JNIEnv * env, jobject obj)
{
    return RenderTarget::getComponentType();
}

}