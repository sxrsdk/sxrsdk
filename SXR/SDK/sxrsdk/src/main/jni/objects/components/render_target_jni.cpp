/***************************************************************************
 * JNI
 ***************************************************************************/

#include "objects/components/render_target.h"
#include "util/sxr_jni.h"

namespace sxr {

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctorMultiview(JNIEnv *env, jobject obj, jlong jtexture, jboolean isMultiview, jboolean isStereo);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctor(JNIEnv *env, jobject obj, jlong jtexture, jlong jsrcTarget);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_getComponentType(JNIEnv *env, jobject obj);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setTexture(JNIEnv *env, jobject obj, jlong ptr, jlong texture);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setCamera(JNIEnv *env, jobject obj, jlong ptr, jlong camera);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_setStereo(JNIEnv *env, jobject obj, jlong ptr, jboolean stereo);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_beginRendering(JNIEnv *env, jobject obj, jlong ptr, jlong camera);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_endRendering(JNIEnv *env, jobject obj, jlong ptr);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderTarget_ctorViewport(JNIEnv *env, jobject obj, jlong jscene, jint w, jint h);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_attachRenderTarget(JNIEnv *env, jobject obj, jlong jrendertarget, jlong jnextrendertarget);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_cullFromCamera(JNIEnv *env, jobject obj, jlong jrendertarget, jlong jscene, jobject javaSceneObject, jlong jcamera, jlong jshaderManager);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderTarget_render(JNIEnv *env, jobject obj, jlong renderTarget, jlong camera,
                                               jlong shader_manager, jlong posteffectrenderTextureA, jlong posteffectRenderTextureB, jlong jscene,
                                               jobject javaSceneObject);
};

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_render(JNIEnv *env, jobject obj, jlong renderTarget,
                                           jlong camera,
                                           jlong shader_manager, jlong posteffectrenderTextureA,
                                           jlong posteffectRenderTextureB, jlong jscene, jobject javaSceneObject) {
    RenderTarget* target = reinterpret_cast<RenderTarget*>(renderTarget);
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    Renderer* r = Renderer::getInstance();

    // Do not remote this: need it for screenshot capturer, center camera rendering
    target->setCamera(reinterpret_cast<Camera*>(camera));
    javaSceneObject = env->NewLocalRef(javaSceneObject);
    r->renderRenderTarget(scene, javaSceneObject, target, reinterpret_cast<ShaderManager*>(shader_manager),
                                                 reinterpret_cast<RenderTexture*>(posteffectrenderTextureA), reinterpret_cast<RenderTexture*>(posteffectRenderTextureB));
    env->DeleteLocalRef(javaSceneObject);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctorViewport(JNIEnv* env, jobject obj, jlong jscene, jint w, jint h)
{
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(scene, w, h));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctorMultiview(JNIEnv *env, jobject obj, jlong jtexture, jboolean isMultiview, jboolean isStereo)
{
    RenderTexture* texture = reinterpret_cast<RenderTexture*>(jtexture);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(texture, isMultiview, isStereo));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_ctor(JNIEnv *env, jobject obj, jlong jtexture, jlong jsrcTarget)
{
    RenderTexture* texture = reinterpret_cast<RenderTexture*>(jtexture);
    RenderTarget* sourceRenderTarget = reinterpret_cast<RenderTarget*>(jsrcTarget);
    return reinterpret_cast<jlong>(Renderer::getInstance()->createRenderTarget(texture, sourceRenderTarget));
}


JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_attachRenderTarget(JNIEnv *env, jobject obj, jlong jrendertarget, jlong jnextrendertarget){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(jrendertarget);
    RenderTarget* nextrendertarget = reinterpret_cast<RenderTarget*>(jnextrendertarget);
    target->attachNextRenderTarget(nextrendertarget);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_beginRendering(JNIEnv *env, jobject obj, jlong ptr, jlong jcamera){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    target->setCamera(camera);
    target->beginRendering(*Renderer::getInstance());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_endRendering(JNIEnv *env, jobject obj, jlong ptr){
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    target->endRendering(*Renderer::getInstance());
}


JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_cullFromCamera(JNIEnv *env, jobject obj, jlong rtarget, jlong jscene, jobject javaSceneObject, jlong jcamera, jlong jshaderManager)
{
    RenderTarget* target = reinterpret_cast<RenderTarget*>(rtarget);
    Camera* camera = reinterpret_cast<Camera*>(jcamera);
    Scene* scene = reinterpret_cast<Scene*>(jscene);
    ShaderManager* shaderManager = reinterpret_cast<ShaderManager*> (jshaderManager);
    jobject jsceneref = env->NewLocalRef(javaSceneObject);
    target->cullFromCamera(scene, jsceneref, camera, shaderManager);
    env->DeleteLocalRef(jsceneref);
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

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderTarget_setStereo(JNIEnv *env, jobject obj, jlong ptr, jboolean stereo)
{
    RenderTarget* target = reinterpret_cast<RenderTarget*>(ptr);
    target->setStereo(stereo);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderTarget_getComponentType(JNIEnv * env, jobject obj)
{
    return RenderTarget::getComponentType();
}

}