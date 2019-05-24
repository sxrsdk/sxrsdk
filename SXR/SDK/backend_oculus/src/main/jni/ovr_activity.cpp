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
#include <objects/scene.h>
#include <objects/components/perspective_camera.h>
#include <objects/components/render_target.h>

#include "util/sxr_log.h"

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
        onBeforeDrawEyesMethodId = GetMethodId(env, viewManagerClass, "beforeDrawEyes", "()V");
        updateSensoredSceneMethodId = GetMethodId(env, viewManagerClass, "updateSensoredScene", "()Z");

        mCaptureCenterEyeMethod = GetMethodId(env, viewManagerClass, "captureCenterEye", "(IIZ)V");
        mCaptureLeftEyeMethod = GetMethodId(env, viewManagerClass, "captureLeftEye", "(IIZ)V");
        mCaptureRightEyeMethod = GetMethodId(env, viewManagerClass, "captureRightEye", "(IIZ)V");
        mCaptureFinishMethod = GetMethodId(env, viewManagerClass, "captureFinish", "()V");
        mCapture3DScreenShot = GetMethodId(env, viewManagerClass, "capture3DScreenShot", "(IIZ)V");

        mGetCaptureTargets = GetMethodId(env, viewManagerClass, "getCaptureTargets", "()I");

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
    renderTextureInfo->useMultiview = gUseMultiview;
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
#ifdef NDEBUG
        //apply first EGL_CONTEXT_OPENGL_NO_ERROR_KHR
        //parms.Flags |= VRAPI_MODE_FLAG_CREATE_CONTEXT_NO_ERROR;
#endif

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
        gUseMultiview = true;
    }
    if (multiview && !gUseMultiview) {
        std::string error = "Multiview is not supported by your device";
        LOGE(" Multiview is not supported by your device");
        throw error;
    }

    clampToBorderSupported_ = nullptr != std::strstr(extensions, "GL_EXT_texture_border_clamp");

    mUseCursorLayer = configurationHelper_.getUseCursorLayer(env);
    for (int eye = 0; eye < (gUseMultiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX); eye++) {
        frameBuffer_[eye].create(mColorTextureFormatConfiguration, mWidthConfiguration,
                                 mHeightConfiguration, mMultisamplesConfiguration,
                                 mResolveDepthConfiguration,
                                 mDepthTextureFormatConfiguration);

        if (mUseCursorLayer) {
            cursorBuffer_[eye].create(mColorTextureFormatConfiguration, mWidthConfiguration / 2,
                                      mHeightConfiguration / 2, mMultisamplesConfiguration,
                                      mResolveDepthConfiguration,
                                      mDepthTextureFormatConfiguration);
        }
    }

    // default viewport same as window size
    x = 0;
    y = 0;
    width = mWidthConfiguration;
    height = mHeightConfiguration;
    configurationHelper_.getSceneViewport(env, x, y, width, height);

    if (mUseCursorLayer) {
        const int cnt = vrapi_GetTextureSwapChainLength(cursorBuffer_->mColorTextureSwapChain);
        const int eyeCount = gUseMultiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX;
        for (int i = 0; i < cnt; ++i) {
            for (int j = 0; j < eyeCount; ++j) {
                FrameBufferObject fbo = cursorBuffer_[j];

                RenderTextureInfo renderTextureInfo;
                renderTextureInfo.fboId = fbo.getRenderBufferFBOId(i);
                renderTextureInfo.fboHeight = fbo.getHeight();
                renderTextureInfo.fboWidth = fbo.getWidth();
                renderTextureInfo.multisamples = mMultisamplesConfiguration;
                renderTextureInfo.useMultiview = gUseMultiview;
                renderTextureInfo.texId = fbo.getColorTexId(i);
                renderTextureInfo.viewport[0] = x;
                renderTextureInfo.viewport[1] = y;
                renderTextureInfo.viewport[2] = fbo.getHeight();
                renderTextureInfo.viewport[3] = fbo.getWidth();

                mCursorRenderTextures[j][i] = Renderer::getInstance()->createRenderTexture(
                        renderTextureInfo);
                mCursorRenderTarget[j][i] = Renderer::getInstance()->createRenderTarget(
                        mCursorRenderTextures[j][i], gUseMultiview);
            }
        }
    }

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
    RenderTarget* renderTarget = gRenderer->getRenderTarget(texSwapChainIndex, gUseMultiview ? 2 : eye);
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

void SXRActivity::onDrawFrame(JNIEnv* env, jobject jViewManager, jobject javaMainScene)
{
    const double predictedDisplayTime = vrapi_GetPredictedDisplayTime(oculusMobile_, frameIndex);
    const ovrTracking tracking = vrapi_GetPredictedTracking(oculusMobile_, predictedDisplayTime);

    ovrTracking updatedTracking = vrapi_GetPredictedTracking(oculusMobile_,
                                                             tracking.HeadPose.TimeInSeconds);
    updatedTracking.HeadPose.Pose.Position = tracking.HeadPose.Pose.Position;

    ovrLayerProjection2 layers[2] = { vrapi_DefaultLayerProjection2(), vrapi_DefaultLayerProjection2() };

    const int eyeCount = gUseMultiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX;
    for (int eye = 0; eye < VRAPI_FRAME_LAYER_EYE_MAX; eye++) {
        auto& eyeLayer = layers[0].Textures[eye];

        eyeLayer.ColorSwapChain = frameBuffer_[gUseMultiview ? 0 : eye].mColorTextureSwapChain;
        eyeLayer.SwapChainIndex = frameBuffer_[gUseMultiview ? 0 : eye].mTextureSwapChainIndex;
        eyeLayer.TexCoordsFromTanAngles = texCoordsTanAnglesMatrix_;
        if (CameraRig::CameraRigType::FREEZE != cameraRig_->camera_rig_type()) {
            layers[0].HeadPose = updatedTracking.HeadPose;
        }

        if (mUseCursorLayer) {
            auto &cursorLayer = layers[1].Textures[eye];
            cursorLayer.ColorSwapChain = cursorBuffer_[gUseMultiview ? 0 : eye].mColorTextureSwapChain;
            cursorLayer.SwapChainIndex = cursorBuffer_[gUseMultiview ? 0 : eye].mTextureSwapChainIndex;
            cursorLayer.TexCoordsFromTanAngles = texCoordsTanAnglesMatrix_;
            layers[1].HeadPose = updatedTracking.HeadPose;
        }
    }

    layers[0].Header.Flags |= VRAPI_FRAME_LAYER_FLAG_CHROMATIC_ABERRATION_CORRECTION;
    layers[0].Header.SrcBlend = VRAPI_FRAME_LAYER_BLEND_ONE;
    layers[0].Header.DstBlend = VRAPI_FRAME_LAYER_BLEND_ZERO;

    if (mUseCursorLayer) {
        layers[1].Header.Flags |= VRAPI_FRAME_LAYER_FLAG_CHROMATIC_ABERRATION_CORRECTION;
        layers[1].Header.Flags |= VRAPI_FRAME_LAYER_FLAG_FIXED_TO_VIEW;
        layers[1].Header.SrcBlend = VRAPI_FRAME_LAYER_BLEND_SRC_ALPHA;
        layers[1].Header.DstBlend = VRAPI_FRAME_LAYER_BLEND_ONE_MINUS_SRC_ALPHA;
    }

    if (CameraRig::CameraRigType::FREEZE == cameraRig_->camera_rig_type()) {
        layers[0].Header.Flags |= VRAPI_FRAME_LAYER_FLAG_FIXED_TO_VIEW;
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

    Renderer* renderer = Renderer::getInstance();
    Scene* mainScene = Scene::main_scene();

    ovrSubmitFrameDescription2 parms = {0};
    parms.FrameIndex = ++frameIndex;
    parms.SwapInterval = 1;
    parms.DisplayTime = predictedDisplayTime;
    parms.LayerCount = 1;

    constexpr int SCREENSHOT_TARGET_CENTER = 0x01;
    constexpr int SCREENSHOT_TARGET_LEFT = 0x02;
    constexpr int SCREENSHOT_TARGET_RIGHT = 0x04;
    constexpr int SCREENSHOT_TARGET_3D = 0x08;

    const int captureTargets = env->CallIntMethod(jViewManager, mGetCaptureTargets);

    // Render the eye images; @todo fix this unwieldy loop
    for (int eye = 0; eye < eyeCount; eye++) {
        int textureSwapChainIndex = frameBuffer_[eye].mTextureSwapChainIndex;
        RenderTarget *renderTarget = renderer->getRenderTarget(textureSwapChainIndex, gUseMultiview ? EYE::MULTIVIEW : eye);
        Camera *centerCamera = static_cast<Camera *>(cameraRig_->center_camera());

        if (0 == eye) {
            if (SCREENSHOT_TARGET_3D & captureTargets) {
                env->CallVoidMethod(jViewManager, mCapture3DScreenShot, eye, textureSwapChainIndex, gUseMultiview);
            }

            renderer->cullFromCamera(mainScene, javaMainScene, centerCamera, mMaterialShaderManager, mRenderDataVector);
            if (!mUseCursorLayer) {
                auto& v = mRenderDataVector[Renderer::LAYER_NORMAL];
                auto& c = mRenderDataVector[Renderer::LAYER_CURSOR];
                v.insert(std::end(v), std::begin(c), std::end(c));
                c.clear();
            } else {
                renderer->state_sort(mRenderDataVector[Renderer::LAYER_CURSOR]);
            }
            renderer->state_sort(mRenderDataVector[Renderer::LAYER_NORMAL]);
            mainScene->getLights().shadersRebuilt();

            if (SCREENSHOT_TARGET_CENTER & captureTargets) {
                renderTarget->setCamera(centerCamera);
                renderer->renderRenderTarget(Scene::main_scene(), javaMainScene, renderTarget,
                                             mMaterialShaderManager,
                                             mPostEffectRenderTextureA, mPostEffectRenderTextureB,
                                             &mRenderDataVector[Renderer::LAYER_NORMAL]);

                env->CallVoidMethod(jViewManager, mCaptureCenterEyeMethod, eye, textureSwapChainIndex, gUseMultiview);
            }

            Camera *leftCamera = cameraRig_->left_camera();
            renderTarget->setCamera(leftCamera);
            renderer->renderRenderTarget(Scene::main_scene(), javaMainScene, renderTarget,
                                         mMaterialShaderManager,
                                         mPostEffectRenderTextureA, mPostEffectRenderTextureB,
                                         &mRenderDataVector[Renderer::LAYER_NORMAL]);
            if (SCREENSHOT_TARGET_LEFT & captureTargets) {
                env->CallVoidMethod(jViewManager, mCaptureLeftEyeMethod, eye, textureSwapChainIndex, gUseMultiview);
            }

        } else if (1 == eye) {
            Camera *rightCamera = cameraRig_->right_camera();
            renderTarget->setCamera(rightCamera);
            renderer->renderRenderTarget(Scene::main_scene(), javaMainScene, renderTarget,
                                         mMaterialShaderManager,
                                         mPostEffectRenderTextureA, mPostEffectRenderTextureB,
                                         &mRenderDataVector[Renderer::LAYER_NORMAL]);

            if (SCREENSHOT_TARGET_RIGHT & captureTargets) {
                env->CallVoidMethod(jViewManager, mCaptureRightEyeMethod, eye, textureSwapChainIndex, gUseMultiview);
            }
            if (0 != captureTargets) {
                env->CallVoidMethod(jViewManager, mCaptureFinishMethod);
            }
        }

        if (gRenderer->isVulkanInstance()) {
            copyVulkanTexture(textureSwapChainIndex, eye);
        } else {
            endRenderingEye(eye);
        }

        if (mUseCursorLayer && mRenderDataVector[Renderer::LAYER_CURSOR].size() > 0) {
            // cursor texture/layer; assumes dynamic texture - can be optimized for a cursor that never
            // changes
            textureSwapChainIndex = cursorBuffer_[eye].mTextureSwapChainIndex;
            renderTarget = mCursorRenderTarget[eye][textureSwapChainIndex];

            Camera *camera = eye ? cameraRig_->right_camera() : cameraRig_->left_camera();
            float alphaOld = camera->background_color_a();
            camera->set_background_color_a(0.0);
            renderTarget->setCamera(camera);
            renderer->renderRenderTarget(Scene::main_scene(), javaMainScene,
                                         renderTarget,
                                         mMaterialShaderManager,
                                         mPostEffectRenderTextureA, mPostEffectRenderTextureB,
                                         &mRenderDataVector[Renderer::LAYER_CURSOR]);
            camera->set_background_color_a(alphaOld);
            parms.LayerCount = 2;

            if (gRenderer->isVulkanInstance()) {
                //cursor layer not supported for vulkan
            } else {
                cursorBuffer_[eye].resolve();
                cursorBuffer_[eye].advance();
                cursorBuffer_[eye].unbind();
            }
        }
    }

    // check if the controller is available
    if (gearController != nullptr && gearController->findConnectedGearController()) {
        // collect the controller input if available
        gearController->onFrame(predictedDisplayTime);
    }

    const ovrLayerHeader2 *layersToSubmit[] =
            {
                    &layers[0].Header,
                    &layers[1].Header,
            };
    parms.Layers = layersToSubmit;

    ovrResult result = vrapi_SubmitFrame2(oculusMobile_, &parms);
    if (ovrSuccess != result) {
        FAIL("vrapi_SubmitFrame2 failed with 0x%X", result);
    }
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
        frameBuffer_[eye].unbind();
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
            for (int eye = 0; eye < (gUseMultiview ? 1 : VRAPI_FRAME_LAYER_EYE_MAX); eye++) {
                frameBuffer_[eye].destroy();
                cursorBuffer_[eye].destroy();
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

void SXRActivity::recenterPose() const {
    vrapi_RecenterPose(oculusMobile_);
}

void SXRActivity::initialize(sxr::ShaderManager* shaderManager, sxr::RenderTexture* textureA,
                             sxr::RenderTexture* textureB) {
    mMaterialShaderManager = shaderManager;
    mPostEffectRenderTextureA = textureA;
    mPostEffectRenderTextureB = textureB;
}

} // namespace sxr
