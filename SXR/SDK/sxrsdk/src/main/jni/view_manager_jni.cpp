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
#include "engine/renderer/renderer.h"
#include "objects/textures/render_texture.h"
#include "objects/components/render_target.h"
//#include "objects/components/camera.h"

namespace sxr {

class Camera;
class Scene;

extern "C" {

    void Java_com_samsungxr_SXRViewManager_makeShadowMaps(JNIEnv *jni, jclass clazz,
                                                        jlong jscene, jobject javaNode,
                                                        jlong jshader_manager,
                                                        jint width, jint height) {
        Scene *scene = reinterpret_cast<Scene *>(jscene);

        ShaderManager *shader_manager = reinterpret_cast<ShaderManager *>(jshader_manager);
        gRenderer = Renderer::getInstance();
        javaNode = jni->NewLocalRef(javaNode);
        gRenderer->makeShadowMaps(scene, javaNode, shader_manager);
        jni->DeleteLocalRef(javaNode);
    }

    void Java_com_samsungxr_SXRViewManager_cullAndRender(JNIEnv *jni, jclass clazz,
                                                      jlong jrenderTarget, jlong jscene,
                                                      jobject javaNode,
                                                      jlong jshader_manager,
                                                      jlong jpost_effect_render_texture_a,
                                                      jlong jpost_effect_render_texture_b)
    {
        Scene *scene = reinterpret_cast<Scene *>(jscene);
        RenderTarget *renderTarget = reinterpret_cast<RenderTarget *>(jrenderTarget);
        ShaderManager *shader_manager =
                reinterpret_cast<ShaderManager *>(jshader_manager);
        RenderTexture *post_effect_render_texture_a =
                reinterpret_cast<RenderTexture *>(jpost_effect_render_texture_a);
        RenderTexture *post_effect_render_texture_b =
                reinterpret_cast<RenderTexture *>(jpost_effect_render_texture_b);

        javaNode = jni->NewLocalRef(javaNode);
        renderTarget->cullFromCamera(scene, javaNode, renderTarget->getCamera(),gRenderer,shader_manager);

        gRenderer->renderRenderTarget(scene, javaNode, renderTarget,shader_manager,post_effect_render_texture_a,post_effect_render_texture_b, renderTarget->getRenderDataVector());

        //unbind the fbo on which the drawing was done
        if(renderTarget->getTexture())
            renderTarget->getTexture()->unbind();

        jni->DeleteLocalRef(javaNode);
    }

    void
    Java_com_samsungxr_SXRRenderBundle_addRenderTarget(JNIEnv *jni, jclass clazz, jlong jrenderTarget,
                                                     jint eye, jint index) {
        RenderTarget *renderTarget = reinterpret_cast<RenderTarget *>(jrenderTarget);
        Renderer::getInstance()->addRenderTarget(renderTarget, EYE(eye), index);
    }

    long Java_com_samsungxr_SXRRenderBundle_getRenderTextureNative(JNIEnv *jni, jclass clazz, jlong jrenderTextureInfo)
    {
        RenderTextureInfo* renderTextureInfo = reinterpret_cast<RenderTextureInfo*>(jrenderTextureInfo);
        RenderTexture* renderTexture = (Renderer::getInstance()->createRenderTexture(*renderTextureInfo));
        delete renderTextureInfo; // free up the resource as it is no longer needed
        return reinterpret_cast<long>(renderTexture);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_SXRViewManager_readRenderResultNative(JNIEnv *env, jclass clazz,
                                                           jobject jreadback_buffer, jlong jrenderTarget, jint eye, jboolean useMultiview);

} // extern "C"


JNIEXPORT void JNICALL Java_com_samsungxr_SXRViewManager_readRenderResultNative(JNIEnv *env, jclass clazz,
                                                                              jobject jreadback_buffer, jlong jrenderTarget, jint eye, jboolean useMultiview){
    uint8_t *readback_buffer = (uint8_t*) env->GetDirectBufferAddress(jreadback_buffer);
    RenderTarget* renderTarget = reinterpret_cast<RenderTarget*>(jrenderTarget);
    RenderTexture* renderTexture = renderTarget->getTexture();
    if(useMultiview){
            renderTexture->setLayerIndex(eye);
    }
    renderTexture->readRenderResult(readback_buffer);
}
}