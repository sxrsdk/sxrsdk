/* Copyright 2016 Samsung Electronics Co., LTD
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

#include "ovr_configuration_helper.h"
#include "jni_utils.h"
#include "util/sxr_log.h"

static const char* app_settings_name = "com/samsungxr/OvrVrAppSettings";

namespace sxr {

ConfigurationHelper::ConfigurationHelper(JNIEnv& env, jobject vrAppSettings)
    : env_(env), vrAppSettings_(vrAppSettings)
{
    vrAppSettings_ = env.NewGlobalRef(vrAppSettings);
    vrAppSettingsClass_ = GetGlobalClassReference(env, app_settings_name);
}

ConfigurationHelper::~ConfigurationHelper() {
    env_.DeleteGlobalRef(vrAppSettingsClass_);
    env_.DeleteGlobalRef(vrAppSettings_);
}

void ConfigurationHelper::getFramebufferConfiguration(JNIEnv& env, int& fbWidthOut, int& fbHeightOut,
        const int fbWidthDefault, const int fbHeightDefault, int& multiSamplesOut,
        ovrTextureFormat& colorTextureFormatOut, bool& resolveDepthOut, ovrTextureFormat& depthTextureFormatOut)
{
    LOGV("ConfigurationHelper: --- framebuffer configuration ---");

    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "eyeBufferParams", "Lcom/samsungxr/utility/VrAppSettings$EyeBufferParams;");
    const jobject parms = env.GetObjectField(vrAppSettings_, fid);
    const jclass parmsClass = env.GetObjectClass(parms);

    fid = env.GetFieldID(parmsClass, "resolutionWidth", "I");
    fbWidthOut = env.GetIntField(parms, fid);
    if (-1 == fbWidthOut) {
        env.SetIntField(parms, fid, fbWidthDefault);
        fbWidthOut = fbWidthDefault;
    }
    LOGV("ConfigurationHelper: --- width %d", fbWidthOut);

    fid = env.GetFieldID(parmsClass, "resolutionHeight", "I");
    fbHeightOut = env.GetIntField(parms, fid);
    if (-1 == fbHeightOut) {
        env.SetIntField(parms, fid, fbHeightDefault);
        fbHeightOut = fbHeightDefault;
    }
    LOGV("ConfigurationHelper: --- height: %d", fbHeightOut);

    fid = env.GetFieldID(parmsClass, "multiSamples", "I");
    multiSamplesOut = env.GetIntField(parms, fid);
    LOGV("ConfigurationHelper: --- multisamples: %d", multiSamplesOut);

    fid = env.GetFieldID(parmsClass, "colorFormat", "Lcom/samsungxr/utility/VrAppSettings$EyeBufferParams$ColorFormat;");
    jobject textureFormat = env.GetObjectField(parms, fid);
    jmethodID mid = env.GetMethodID(env.GetObjectClass(textureFormat),"getValue","()I");
    int textureFormatValue = env.CallIntMethod(textureFormat, mid);
    switch (textureFormatValue){
    case 0:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_565;
        break;
    case 1:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_5551;
        break;
    case 2:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_4444;
        break;
    case 3:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_8888;
        break;
    case 4:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_8888_sRGB;
        break;
    case 5:
        colorTextureFormatOut = VRAPI_TEXTURE_FORMAT_RGBA16F;
        break;
    default:
        LOGE("fatal error: unknown color texture format");
        std::terminate();
    }
    LOGV("ConfigurationHelper: --- color texture format: %d", colorTextureFormatOut);

    fid = env.GetFieldID(parmsClass, "resolveDepth", "Z");
    resolveDepthOut = env.GetBooleanField(parms, fid);
    LOGV("ConfigurationHelper: --- resolve depth: %d", resolveDepthOut);

    fid = env.GetFieldID(parmsClass, "depthFormat",
            "Lcom/samsungxr/utility/VrAppSettings$EyeBufferParams$DepthFormat;");
    jobject depthFormat = env.GetObjectField(parms, fid);
    mid = env.GetMethodID(env.GetObjectClass(depthFormat), "getValue", "()I");
    int depthFormatValue = env.CallIntMethod(depthFormat, mid);
    switch (depthFormatValue) {
    case 0:
        depthTextureFormatOut = VRAPI_TEXTURE_FORMAT_NONE;
        break;
    case 1:
        depthTextureFormatOut = VRAPI_TEXTURE_FORMAT_DEPTH_16;
        break;
    case 2:
        depthTextureFormatOut = VRAPI_TEXTURE_FORMAT_DEPTH_24;
        break;
    case 3:
        depthTextureFormatOut = VRAPI_TEXTURE_FORMAT_DEPTH_24_STENCIL_8;
        break;
    default:
        LOGE("fatal error: unknown depth texture format");
        std::terminate();
    }

    LOGV("ConfigurationHelper: --- depth texture format: %d", depthTextureFormatOut);
    LOGV("ConfigurationHelper: ---------------------------------");
}
void ConfigurationHelper::getMultiviewConfiguration(JNIEnv& env, bool& useMultiview){

    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "useMultiview", "Z");
    useMultiview = env.GetBooleanField(vrAppSettings_, fid);
}
void ConfigurationHelper::getModeConfiguration(JNIEnv& env, bool& allowPowerSaveOut, bool& resetWindowFullscreenOut) {
    LOGV("ConfigurationHelper: --- mode configuration ---");

    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "modeParams", "Lcom/samsungxr/utility/VrAppSettings$ModeParams;");
    jobject modeParms = env.GetObjectField(vrAppSettings_, fid);
    jclass modeParmsClass = env.GetObjectClass(modeParms);

    allowPowerSaveOut = env.GetBooleanField(modeParms, env.GetFieldID(modeParmsClass, "allowPowerSave", "Z"));
    LOGV("ConfigurationHelper: --- allowPowerSave: %d", allowPowerSaveOut);
    resetWindowFullscreenOut = env.GetBooleanField(modeParms, env.GetFieldID(modeParmsClass, "resetWindowFullScreen","Z"));
    LOGV("ConfigurationHelper: --- resetWindowFullscreen: %d", resetWindowFullscreenOut);

    LOGV("ConfigurationHelper: --------------------------");
}

void ConfigurationHelper::getPerformanceConfiguration(JNIEnv& env, ovrPerformanceParms& parmsOut) {
    LOGV("ConfigurationHelper: --- performance configuration ---");

    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "performanceParams", "Lcom/samsungxr/utility/VrAppSettings$PerformanceParams;");
    jobject parms = env.GetObjectField(vrAppSettings_, fid);
    jclass parmsClass = env.GetObjectClass(parms);

    parmsOut.GpuLevel = env.GetIntField(parms, env.GetFieldID(parmsClass, "gpuLevel", "I"));
    LOGV("ConfigurationHelper: --- gpuLevel: %d", parmsOut.GpuLevel);
    parmsOut.CpuLevel = env.GetIntField(parms, env.GetFieldID(parmsClass, "cpuLevel", "I"));
    LOGV("ConfigurationHelper: --- cpuLevel: %d", parmsOut.CpuLevel);

    LOGV("ConfigurationHelper: --------------------------");
}

void ConfigurationHelper::getSceneViewport(JNIEnv& env, int& viewport_x, int& viewport_y, int& viewport_width, int& viewport_height) {

    LOGV("ConfigurationHelper: --- viewport configuration ---");
    int x, y, width, height;

    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "sceneParams", "Lcom/samsungxr/OvrVrAppSettings$SceneParams;");
    jobject parms = env.GetObjectField(vrAppSettings_, fid);
    jclass parmsClass = env.GetObjectClass(parms);

    x = env.GetIntField(parms, env.GetFieldID(parmsClass, "viewportX", "I"));
    y = env.GetIntField(parms, env.GetFieldID(parmsClass, "viewportY", "I"));
    width = env.GetIntField(parms, env.GetFieldID(parmsClass, "viewportWidth", "I"));
    height = env.GetIntField(parms, env.GetFieldID(parmsClass, "viewportHeight", "I"));

    if (width != 0 && height != 0) {

        // otherwise default viewport
        viewport_x = x;
        viewport_y = y;
        viewport_width = width;
        viewport_height = height;
    }
}

const bool ConfigurationHelper::getUseCursorLayer(JNIEnv &env) {
    jfieldID fid = env.GetFieldID(vrAppSettingsClass_, "mUseCursorLayer", "Z");
    return env.GetBooleanField(vrAppSettings_, fid);
}

}
