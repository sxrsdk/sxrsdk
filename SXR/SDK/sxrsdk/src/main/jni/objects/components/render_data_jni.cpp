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

#include "render_data.h"
#include "engine/renderer/renderer.h"

namespace sxr {

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderData_ctor(JNIEnv * env, jobject obj);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_NativeRenderData_getComponentType(JNIEnv * env, jobject obj);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setMesh(JNIEnv * env,
                                              jobject obj, jlong jrender_data, jlong jmesh);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_addPass(JNIEnv* env,
                                              jobject obj, jlong jrender_data, jlong jrender_pass);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_removePass(JNIEnv *env, jclass type, jlong renderData,
                                                 jint renderPass);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_enableLight(JNIEnv * env,
                                                  jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_disableLight(JNIEnv * env,
                                                   jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_enableLightMap(JNIEnv * env,
                                                     jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_disableLightMap(JNIEnv * env,
                                                      jobject obj, jlong jrender_data);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeRenderData_getRenderMask(JNIEnv * env,
                                                    jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setRenderMask(JNIEnv * env,
                                                    jobject obj, jlong jrender_data, jint render_mask);
    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeRenderData_getRenderingOrder(
            JNIEnv * env, jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setRenderingOrder(
            JNIEnv * env, jobject obj, jlong jrender_data, jint rendering_order);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getOffset(JNIEnv * env,
                                                jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setOffset(JNIEnv * env,
                                                jobject obj, jlong jrender_data, jboolean offset);
    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_NativeRenderData_getOffsetFactor(JNIEnv * env,
                                                      jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setOffsetFactor(JNIEnv * env,
                                                      jobject obj, jlong jrender_data, jfloat offset_factor);
    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_NativeRenderData_getOffsetUnits(JNIEnv * env,
                                                     jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setOffsetUnits(JNIEnv * env,
                                                     jobject obj, jlong jrender_data, jfloat offset_units);
    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getDepthTest(JNIEnv * env,
                                                   jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setDepthTest(JNIEnv * env,
                                                   jobject obj, jlong jrender_data, jboolean depth_test);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setDepthMask(JNIEnv * env,
                                                   jobject obj, jlong jrender_data, jboolean depth_mask);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getAlphaBlend(JNIEnv * env,
                                                    jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setAlphaBlend(JNIEnv * env,
                                                    jobject obj, jlong jrender_data, jboolean alpha_blend);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getAlphaToCoverage(JNIEnv * env,
                                                         jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setAlphaToCoverage(JNIEnv * env,
                                                         jobject obj, jlong jrender_data, jboolean alphaToCoverage);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setSampleCoverage(JNIEnv * env,
                                                        jobject obj, jlong jrender_data, jfloat sampleCoverage);

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_NativeRenderData_getSampleCoverage(JNIEnv * env,
                                                        jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setInvertCoverageMask(JNIEnv * env,
                                                            jobject obj, jlong jrender_data, jboolean invertCoverageMask);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getInvertCoverageMask(JNIEnv * env,
                                                            jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setCastShadows(JNIEnv * env,
                                                     jobject obj, jlong jrender_data, jboolean castShadows);

    JNIEXPORT jboolean JNICALL
    Java_com_samsungxr_NativeRenderData_getCastShadows(JNIEnv * env,
                                                     jobject obj, jlong jrender_data);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeRenderData_getDrawMode(
            JNIEnv * env, jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setDrawMode(
            JNIEnv * env, jobject obj, jlong jrender_data, jint draw_mode);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setTextureCapturer(JNIEnv * env, jobject obj,
                                                         jlong jrender_data, jlong jtexture_capturer);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setStencilFunc(JNIEnv *env, jclass type, jlong renderData,
                                                     jint func, jint ref, jint mask);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setStencilOp(JNIEnv *env, jclass type, jlong renderData,
                                                   jint fail, jint zfail, jint zpass);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setStencilMask(JNIEnv *env, jclass type, jlong renderData,
                                                     jint mask);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setStencilTest(JNIEnv *env, jclass type, jlong renderData, jboolean flag);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setAlphaBlendFunc(JNIEnv * env,
                                                        jobject obj, jlong jrender_data, jint sourceBlend, jint destBlend);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeRenderData_getSourceAlphaBlendFunc(JNIEnv * env,
                                                              jobject obj, jlong jrender_data);

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_NativeRenderData_getDestAlphaBlendFunc(JNIEnv * env,
                                                            jobject obj, jlong jrender_data);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setBindShaderObject(JNIEnv *env, jclass type, jlong jRenderData,
                                                          jobject bindShaderObject);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_NativeRenderData_setLayer(JNIEnv *env, jclass type, jlong aNative, jint layer);
}


JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderData_ctor(JNIEnv * env, jobject obj)
{
    Renderer* renderer = Renderer::getInstance();
    return reinterpret_cast<jlong>(renderer->createRenderData());
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_NativeRenderData_getComponentType(JNIEnv * env, jobject obj) {
    return RenderData::getComponentType();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setMesh(JNIEnv * env,
                                          jobject obj, jlong jrender_data, jlong jmesh) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    Mesh* mesh = reinterpret_cast<Mesh*>(jmesh);
    render_data->set_mesh(mesh);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_addPass(JNIEnv* env,
                                          jobject obj, jlong jrender_data, jlong jrender_pass) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    RenderPass* render_pass = reinterpret_cast<RenderPass*>(jrender_pass);
    render_data->add_pass(render_pass);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_removePass(JNIEnv *, jclass, jlong renderData, jint renderPass)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(renderData);
    render_data->remove_pass(renderPass);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_enableLight(JNIEnv * env,
                                              jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->enable_light();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_disableLight(JNIEnv * env,
                                               jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->disable_light();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_enableLightMap(JNIEnv * env,
                                                 jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->enable_lightmap();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_disableLightMap(JNIEnv * env,
                                                  jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->disable_lightmap();
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeRenderData_getRenderMask(JNIEnv * env,
                                                jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->render_mask();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setRenderMask(JNIEnv * env,
                                                jobject obj, jlong jrender_data, jint render_mask) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_render_mask(render_mask);
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeRenderData_getRenderingOrder(
        JNIEnv * env, jobject obj, jlong jrender_data)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->rendering_order();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setRenderingOrder(
        JNIEnv * env, jobject obj, jlong jrender_data, jint rendering_order) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_rendering_order(rendering_order);
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getOffset(JNIEnv * env,
                                            jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jboolean>(render_data->offset());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setOffset(JNIEnv * env,
                                            jobject obj, jlong jrender_data, jboolean offset) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_offset(static_cast<bool>(offset));
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_NativeRenderData_getOffsetFactor(JNIEnv * env,
                                                  jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->offset_factor();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setOffsetFactor(JNIEnv * env,
                                                  jobject obj, jlong jrender_data, jfloat offset_factor) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_offset_factor(offset_factor);
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_NativeRenderData_getOffsetUnits(JNIEnv * env,
                                                 jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->offset_units();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setOffsetUnits(JNIEnv * env,
                                                 jobject obj, jlong jrender_data, jfloat offset_units) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_offset_units(offset_units);
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getDepthTest(JNIEnv * env,
                                               jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jboolean>(render_data->depth_test());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setDepthTest(JNIEnv * env,
                                               jobject obj, jlong jrender_data, jboolean depth_test) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_depth_test(static_cast<bool>(depth_test));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setDepthMask(JNIEnv * env,
                                               jobject obj, jlong jrender_data, jboolean depth_mask) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_depth_mask(static_cast<bool>(depth_mask));
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getAlphaBlend(JNIEnv * env,
                                                jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jboolean>(render_data->alpha_blend());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setAlphaBlend(JNIEnv * env,
                                                jobject obj, jlong jrender_data, jboolean alpha_blend) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_alpha_blend(static_cast<bool>(alpha_blend));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setAlphaBlendFunc(JNIEnv * env,
                                                    jobject obj, jlong jrender_data, jint sourceBlend, jint destBlend)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_alpha_blend_func(sourceBlend, destBlend);
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeRenderData_getSourceAlphaBlendFunc(JNIEnv * env,
                                                          jobject obj, jlong jrender_data)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->source_alpha_blend_func();
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeRenderData_getDestAlphaBlendFunc(JNIEnv * env,
                                                        jobject obj, jlong jrender_data)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->dest_alpha_blend_func();
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getAlphaToCoverage(JNIEnv * env,
                                                     jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jboolean>(render_data->alpha_to_coverage());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setAlphaToCoverage(JNIEnv * env,
                                                     jobject obj, jlong jrender_data, jboolean alphaToCoverage) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_alpha_to_coverage(static_cast<bool>(alphaToCoverage));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setSampleCoverage(JNIEnv * env,
                                                    jobject obj, jlong jrender_data, jfloat sampleCoverage) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_sample_coverage(static_cast<float>(sampleCoverage));
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_NativeRenderData_getSampleCoverage(JNIEnv * env,
                                                    jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jfloat>(render_data->sample_coverage());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setInvertCoverageMask(JNIEnv * env,
                                                        jobject obj, jlong jrender_data, jboolean invertCoverageMask) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_invert_coverage_mask(static_cast<bool>(invertCoverageMask));
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getInvertCoverageMask(JNIEnv * env,
                                                        jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return static_cast<jboolean>(render_data->invert_coverage_mask());
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setDrawMode(
        JNIEnv * env, jobject obj, jlong jrender_data, jint draw_mode) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_draw_mode(draw_mode);
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_NativeRenderData_getDrawMode(
        JNIEnv * env, jobject obj, jlong jrender_data) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->draw_mode();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setTextureCapturer(JNIEnv * env, jobject obj,
                                                     jlong jrender_data, jlong jtexture_capturer) {
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_texture_capturer(
            reinterpret_cast<TextureCapturer*>(jtexture_capturer));
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setCastShadows(JNIEnv * env,
                                                 jobject obj, jlong jrender_data, jboolean castShadows)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    render_data->set_cast_shadows(castShadows);
}

JNIEXPORT jboolean JNICALL
Java_com_samsungxr_NativeRenderData_getCastShadows(JNIEnv * env,
                                                 jobject obj, jlong jrender_data)
{
    RenderData* render_data = reinterpret_cast<RenderData*>(jrender_data);
    return render_data->cast_shadows();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setStencilFunc(JNIEnv *env, jclass type, jlong renderData,
                                                 jint func, jint ref, jint mask) {
    RenderData* rd = reinterpret_cast<RenderData*>(renderData);
    rd->setStencilFunc(func, ref, mask);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setStencilOp(JNIEnv *env, jclass type, jlong renderData,
                                               jint fail, jint zfail, jint zpass) {
    RenderData* rd = reinterpret_cast<RenderData*>(renderData);
    rd->setStencilOp(fail, zfail, zpass);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setStencilMask(JNIEnv *env, jclass type, jlong renderData,
                                                 jint mask) {
    RenderData* rd = reinterpret_cast<RenderData*>(renderData);
    rd->setStencilMask(mask);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setStencilTest(JNIEnv *env, jclass type, jlong renderData, jboolean flag) {
    RenderData* rd = reinterpret_cast<RenderData*>(renderData);
    rd->setStencilTest(flag);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setBindShaderObject(JNIEnv* env, jclass, jlong jRenderData, jobject bindShaderObject) {
    RenderData* rd = reinterpret_cast<RenderData*>(jRenderData);
    rd->setBindShaderObject(env, bindShaderObject);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_NativeRenderData_setLayer(JNIEnv *env, jclass type, jlong aNative, jint layer) {
    RenderData* rd = reinterpret_cast<RenderData*>(aNative);
    rd->setLayer(layer);
}

}
