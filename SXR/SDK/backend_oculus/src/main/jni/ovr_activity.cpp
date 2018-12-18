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

#include "ovr_activity.h"
#include "../util/jni_utils.h"
#include "../eglextension/msaa/msaa.h"
#include "VrApi_Helpers.h"
#include "VrApi.h"
#include "VrApi_SystemUtils.h"
#include <cstring>
#include <unistd.h>
#include "engine/renderer/renderer.h"
#include <VrApi_Types.h>
#include <engine/renderer/vulkan_renderer.h>
#include <objects/textures/render_texture.h>

static const char* activityClassName = "android/app/Activity";
static const char* applicationClassName = "com/samsungxr/SXRApplication";
static const char* viewManagerClassName = "com/samsungxr/OvrViewManager";

namespace sxr {

//=============================================================================
//                             SXRActivity
//=============================================================================

SXRActivity::SXRActivity(JNIEnv &env, jobject activity, jobject vrAppSettings) : envMainThread_(
        &env), configurationHelper_(env, vrAppSettings)
    {
        activity_ = env.NewGlobalRef(activity);
        activityClass_ = GetGlobalClassReference(env, activityClassName);
        applicationClass_ = GetGlobalClassReference(env, applicationClassName);

        jclass viewManagerClass = env.FindClass(viewManagerClassName);
        onDrawEyeMethodId = GetMethodId(env, viewManagerClass, "onDrawEye", "(IIZ)V");
        onBeforeDrawEyesMethodId = GetMethodId(env, viewManagerClass, "beforeDrawEyes", "()V");
        updateSensoredSceneMethodId = GetMethodId(env, viewManagerClass, "updateSensoredScene", "()Z");

        mainThreadId_ = gettid();
    }

    SXRActivity::~SXRActivity() {
        LOGV("SXRActivity::~SXRActivity");
        envMainThread_->DeleteGlobalRef(activityClass_);
        envMainThread_->DeleteGlobalRef(activity_);
        envMainThread_->DeleteGlobalRef(jsurface_);
    }

    int SXRActivity::initializeVrApi() {
        initializeOculusJava(*envMainThread_, oculusJavaMainThread_);

        const ovrInitParms initParms = vrapi_DefaultInitParms(&oculusJavaMainThread_);
        ovrInitializeStatus vrapiInitResult = vrapi_Initialize(&initParms);
        if (VRAPI_INITIALIZE_UNKNOWN_ERROR == vrapiInitResult) {
            LOGE("Oculus is probably not present on this device");
            return vrapiInitResult;
        }

        if (VRAPI_INITIALIZE_PERMISSIONS_ERROR == vrapiInitResult) {
            char const * msg = "Thread priority security exception. Make sure the APK is signed.";
            vrapi_ShowFatalError(&oculusJavaMainThread_, nullptr, msg, __FILE__, __LINE__);
        }

        return vrapiInitResult;
    }

    /**
     * Do not call unless vrapi has been successfully initialized prior to that.
     */
    void SXRActivity::uninitializeVrApi() {
        vrapi_Shutdown();
    }

    void SXRActivity::showConfirmQuit() {
        LOGV("SXRActivity::showConfirmuit");

        ovrFrameParms parms = vrapi_DefaultFrameParms(&oculusJavaGlThread_, VRAPI_FRAME_INIT_BLACK_FINAL, vrapi_GetTimeInSeconds(), nullptr);
        parms.FrameIndex = ++frameIndex;
        parms.SwapInterval = 1;
        parms.PerformanceParms = oculusPerformanceParms_;
        vrapi_SubmitFrame(oculusMobile_, &parms);

        vrapi_ShowSystemUI(&oculusJavaGlThread_, VRAPI_SYS_UI_CONFIRM_QUIT_MENU);
    }

    bool SXRActivity::updateSensoredScene(jobject jViewManager) {
        return oculusJavaGlThread_.Env->CallBooleanMethod(jViewManager, updateSensoredSceneMethodId);
    }

    void SXRActivity::setCameraRig(jlong cameraRig) {
        cameraRig_ = reinterpret_cast<CameraRig*>(cameraRig);
        sensoredSceneUpdated_ = false;
    }

    void SXRActivity::onSurfaceCreated(JNIEnv& env) {
        LOGV("SXRActivity::onSurfaceCreated");
        initializeOculusJava(env, oculusJavaGlThread_);

        //must happen as soon as possible as it updates the java side wherever it has default values; e.g.
        //resolutionWidth -1 becomes whatever VRAPI_SYS_PROP_SUGGESTED_EYE_TEXTURE_WIDTH is.
        configurationHelper_.getFramebufferConfiguration(env, mWidthConfiguration, mHeightConfiguration,
                                                         vrapi_GetSystemPropertyInt(&oculusJavaGlThread_, VRAPI_SYS_PROP_SUGGESTED_EYE_TEXTURE_WIDTH),
                                                         vrapi_GetSystemPropertyInt(&oculusJavaGlThread_, VRAPI_SYS_PROP_SUGGESTED_EYE_TEXTURE_HEIGHT),
                                                         mMultisamplesConfiguration, mColorTextureFormatConfiguration,
                                                         mResolveDepthConfiguration, mDepthTextureFormatConfiguration);
    }

RenderTextureInfo *SXRActivity::getRenderTextureInfo(int eye, int index) {
    // for multiview, eye index would be 2
    eye = eye % 2;
    FrameBufferObject fbo = frameBuffer_[eye];

    RenderTextureInfo *renderTextureInfo = new RenderTextureInfo();
    renderTextureInfo->fboId = fbo.getRenderBufferFBOId(index);
    renderTextureInfo->fboHeight = fbo.getHeight();
    renderTextureInfo->fboWidth = fbo.getWidth();
    renderTextureInfo->multisamples = mMultisamplesConfiguration;
    renderTextureInfo->useMultiview = use_multiview;
    renderTextureInfo->texId = fbo.getColorTexId(index);
    renderTextureInfo->viewport[0] = x;
    renderTextureInfo->viewport[1] = y;
    renderTextureInfo->viewport[2] = width;
    renderTextureInfo->viewport[3] = height;

    return renderTextureInfo;
}

void SXRActivity::onSurfaceChanged(JNIEnv &env, jobject jsurface) {
    int maxSamples = MSAA::getMaxSampleCount();
    LOGV("SXRActivity::onSurfaceChanged");
    initializeOculusJava(env, oculusJavaGlThread_);
    jsurface_ = env.NewGlobalRef(jsurface);

    if (nullptr != oculusMobile_) {
        return;
    }

    ovrModeParms parms = vrapi_DefaultModeParms(&oculusJavaGlThread_);
    {
        bool allowPowerSave, resetWindowFullscreen;
        configurationHelper_.getModeConfiguration(env, allowPowerSave, resetWindowFullscreen);
        if (allowPowerSave) {
            parms.Flags |= VRAPI_MODE_FLAG_ALLOW_POWER_SAVE;
        }
        if (resetWindowFullscreen) {
            parms.Flags |= VRAPI_MODE_FLAG_RESET_WINDOW_FULLSCREEN;
        }
        parms.Flags |= VRAPI_MODE_FLAG_NATIVE_WINDOW;
        //@todo consider VRAPI_MODE_FLAG_CREATE_CONTEXT_NO_ERROR as a release-build optimization

        ANativeWindow *nativeWindow = ANativeWindow_fromSurface(&env, jsurface_);
        if (nullptr == nativeWindow) {
            FAIL("No native window!");
        }
        parms.WindowSurface = reinterpret_cast<unsigned long long>(nativeWindow);
        EGLDisplay display = eglGetCurrentDisplay();
        if (EGL_NO_DISPLAY == display) {
            FAIL("No egl display!");
        }
        parms.Display = reinterpret_cast<unsigned long long>(display);
        EGLContext context = eglGetCurrentContext();
        if (EGL_NO_CONTEXT == context) {
            FAIL("No egl context!");
        }
        parms.ShareContext = reinterpret_cast<unsigned long long>(context);
    }

    //@todo backend specific fix, generalize; ensures there is a renderer instance after pause/resume
    gRenderer = Renderer::getInstance();

    oculusMobile_ = vrapi_EnterVrMode(&parms);
    if (nullptr == oculusMobile_) {
        FAIL("vrapi_EnterVrMode failed!");
    }

    if (gearController != nullptr) {
        gearController->setOvrMobile(oculusMobile_);
    }

    oculusPerformanceParms_ = vrapi_DefaultPerformanceParms();
    env.ExceptionClear(); //clear a weird GearVrRemoteForBatteryWorkAround raised by Oculus
    configurationHelper_.getPerformanceConfiguration(env, oculusPerformanceParms_);
    oculusPerformanceParms_.MainThreadTid = mainThreadId_;
    oculusPerformanceParms_.RenderThreadTid = gettid();

    if (mMultisamplesConfiguration > maxSamples) {
        mMultisamplesConfiguration = maxSamples;
    }

    bool multiview;
    configurationHelper_.getMultiviewConfiguration(env, multiview);

    const char *extensions = (const char *) glGetString(GL_EXTENSIONS);
    if (multiview && std::strstr(extensions, "GL_OVR_multiview2") != NULL) {
        use_multiview = true;
    }
    if (multiview && !use_multiview) {
        std::string error = "Multiview is not supported by your device";
        LOGE(" Multiview is not supported by your device");
        throw error;
    }

    clampToBorderSupported_ = nullptr != std::strstr(extensions, "GL_EXT_texture_border_clamp");

    for (int eye = 0; eye < (use_multiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX); eye++) {
        frameBuffer_[eye].create(mColorTextureFormatConfiguration, mWidthConfiguration,
                                 mHeightConfiguration, mMultisamplesConfiguration,
                                 mResolveDepthConfiguration,
                                 mDepthTextureFormatConfiguration);
    }

    // default viewport same as window size
    x = 0;
    y = 0;
    width = mWidthConfiguration;
    height = mHeightConfiguration;
    configurationHelper_.getSceneViewport(env, x, y, width, height);

    projectionMatrix_ = ovrMatrix4f_CreateProjectionFov(
            vrapi_GetSystemPropertyFloat(&oculusJavaGlThread_,
                                         VRAPI_SYS_PROP_SUGGESTED_EYE_FOV_DEGREES_X),
            vrapi_GetSystemPropertyFloat(&oculusJavaGlThread_,
                                         VRAPI_SYS_PROP_SUGGESTED_EYE_FOV_DEGREES_Y), 0.0f,
            0.0f, 1.0f,
            0.0f);
    texCoordsTanAnglesMatrix_ = ovrMatrix4f_TanAngleMatrixFromProjection(&projectionMatrix_);

    //so that android events get generated for the back key; bear in mind that with controller
    //connected this emulation will be turned off in ovr_gear_controller.cpp
    ovrResult r = vrapi_SetRemoteEmulation(oculusMobile_, true);
    if (ovrSuccess != r) {
        FAIL("vrapi_SetRemoteEmulation failed");
    }
}

void SXRActivity::copyVulkanTexture(int texSwapChainIndex, int eye){
    RenderTarget* renderTarget = gRenderer->getRenderTarget(texSwapChainIndex, use_multiview ? 2 : eye);
    reinterpret_cast<VulkanRenderer*>(gRenderer)->renderToOculus(renderTarget);

    glBindTexture(GL_TEXTURE_2D,vrapi_GetTextureSwapChainHandle(frameBuffer_[eye].mColorTextureSwapChain, texSwapChainIndex));
    glTexSubImage2D(   GL_TEXTURE_2D,
                       0,
                       0,
                       0,
                       mWidthConfiguration,
                       mHeightConfiguration,
                       GL_RGBA,
                       GL_UNSIGNED_BYTE,
                       oculusTexData);
    glFlush();
    frameBuffer_[eye].advance();
    reinterpret_cast<VulkanRenderer*>(gRenderer)->unmapRenderToOculus(renderTarget);
}

void SXRActivity::onDrawFrame(jobject jViewManager) {
    ovrFrameParms parms = vrapi_DefaultFrameParms(&oculusJavaGlThread_, VRAPI_FRAME_INIT_DEFAULT,
                                                  vrapi_GetTimeInSeconds(),
                                                  NULL);
    parms.FrameIndex = ++frameIndex;
    parms.SwapInterval = 1;
    parms.PerformanceParms = oculusPerformanceParms_;

    const double predictedDisplayTime = vrapi_GetPredictedDisplayTime(oculusMobile_, frameIndex);
    const ovrTracking tracking = vrapi_GetPredictedTracking(oculusMobile_, predictedDisplayTime);

    ovrTracking updatedTracking = vrapi_GetPredictedTracking(oculusMobile_,
                                                             tracking.HeadPose.TimeInSeconds);
    updatedTracking.HeadPose.Pose.Position = tracking.HeadPose.Pose.Position;

    for (int eye = 0; eye < VRAPI_FRAME_LAYER_EYE_MAX; eye++) {
        ovrFrameLayerTexture &eyeTexture = parms.Layers[0].Textures[eye];

        eyeTexture.ColorTextureSwapChain = frameBuffer_[use_multiview ? 0
                                                                      : eye].mColorTextureSwapChain;
        eyeTexture.DepthTextureSwapChain = frameBuffer_[use_multiview ? 0
                                                                      : eye].mDepthTextureSwapChain;
        eyeTexture.TextureSwapChainIndex = frameBuffer_[use_multiview ? 0
                                                                      : eye].mTextureSwapChainIndex;
        eyeTexture.TexCoordsFromTanAngles = texCoordsTanAnglesMatrix_;
        if (CameraRig::CameraRigType::FREEZE != cameraRig_->camera_rig_type()) {
            eyeTexture.HeadPose = updatedTracking.HeadPose;
        }
    }

    parms.Layers[0].Flags |= VRAPI_FRAME_LAYER_FLAG_CHROMATIC_ABERRATION_CORRECTION;
    if (CameraRig::CameraRigType::FREEZE == cameraRig_->camera_rig_type()) {
        parms.Layers[0].Flags |= VRAPI_FRAME_LAYER_FLAG_FIXED_TO_VIEW;
    } else {
        const ovrQuatf &orientation = updatedTracking.HeadPose.Pose.Orientation;
        const glm::quat tmp(orientation.w, orientation.x, orientation.y, orientation.z);
        const glm::quat quat = glm::conjugate(glm::inverse(tmp));

        cameraRig_->setRotationSensorData(0, quat.w, quat.x, quat.y, quat.z, 0, 0, 0);
    }

    cameraRig_->updateRotation();

    if (!sensoredSceneUpdated_) {
        sensoredSceneUpdated_ = updateSensoredScene(jViewManager);
    }
    oculusJavaGlThread_.Env->CallVoidMethod(jViewManager, onBeforeDrawEyesMethodId);

    // Render the eye images.
    for (int eye = 0; eye < (use_multiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX); eye++) {
        int textureSwapChainIndex = frameBuffer_[eye].mTextureSwapChainIndex;
        oculusJavaGlThread_.Env->CallVoidMethod(jViewManager, onDrawEyeMethodId, eye,
                                                textureSwapChainIndex, use_multiview);

        if (gRenderer->isVulkanInstance()) {
            copyVulkanTexture(textureSwapChainIndex, eye);
        } else {
            endRenderingEye(eye);
            FrameBufferObject::unbind();
        }
    }

    // check if the controller is available
    if (gearController != nullptr && gearController->findConnectedGearController()) {
        // collect the controller input if available
        gearController->onFrame(predictedDisplayTime);
    }

    vrapi_SubmitFrame(oculusMobile_, &parms);
}

    void SXRActivity::endRenderingEye(const int eye) {
        if (!clampToBorderSupported_) {
            // quote off VrApi_Types.h:
            // <quote>
            // Because OpenGL ES does not support clampToBorder, it is the
            // application's responsibility to make sure that all mip levels
            // of the primary eye texture have a black border that will show
            // up when time warp pushes the texture partially off screen.
            // </quote>
            // also see EyePostRender::FillEdgeColor in VrAppFramework
            GL(glClearColor(0, 0, 0, 1));
            GL(glEnable(GL_SCISSOR_TEST));

            GL(glScissor(0, 0, mWidthConfiguration, 1));
            GL(glClear(GL_COLOR_BUFFER_BIT));
            GL(glScissor(0, mHeightConfiguration - 1, mWidthConfiguration, 1));
            GL(glClear(GL_COLOR_BUFFER_BIT));
            GL(glScissor(0, 0, 1, mHeightConfiguration));
            GL(glClear(GL_COLOR_BUFFER_BIT));
            GL(glScissor(mWidthConfiguration - 1, 0, 1, mHeightConfiguration));
            GL(glClear(GL_COLOR_BUFFER_BIT));

            GL(glDisable(GL_SCISSOR_TEST));
        }

        //per vrAppFw
        frameBuffer_[eye].resolve();
        frameBuffer_[eye].advance();
    }

    void SXRActivity::initializeOculusJava(JNIEnv& env, ovrJava& oculusJava) {
        oculusJava.Env = &env;
        env.GetJavaVM(&oculusJava.Vm);
        oculusJava.ActivityObject = activity_;
    }

    void SXRActivity::leaveVrMode() {
        LOGV("SXRActivity::leaveVrMode");
        Renderer::resetInstance();
        if (nullptr != oculusMobile_) {
            for (int eye = 0; eye < (use_multiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX); eye++) {
                frameBuffer_[eye].destroy();
            }

            if (nullptr != gearController) {
                gearController->reset();
            }
            vrapi_LeaveVrMode(oculusMobile_);
            oculusMobile_ = nullptr;
        } else {
            LOGW("SXRActivity::leaveVrMode: ignored, have not entered vrMode");
        }
    }

/**
 * Must be called on the GL thread
 */
    bool SXRActivity::isHmtConnected() const {
        return vrapi_GetSystemStatusInt(&oculusJavaMainThread_, VRAPI_SYS_STATUS_DOCKED);
    }

    bool SXRActivity::usingMultiview() const {
        LOGD("Activity: usingMultview = %d", use_multiview);
        return use_multiview;
    }
}
