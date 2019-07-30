/*
 * Copyright 2017 Samsung Electronics Co., LTD
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

#include "ovr_gear_controller.h"

#include "glm/gtx/quaternion.hpp"
#include "util/sxr_log.h"


namespace sxr {

    bool GearController::findConnectedGearController() {
        bool foundRemote = false;

        for (uint32_t deviceIndex = 0;; deviceIndex++) {
            ovrInputCapabilityHeader curCaps;

            if (vrapi_EnumerateInputDevices(mOvrMobile, deviceIndex, &curCaps) < 0) {
                break;
            }
            switch (curCaps.Type) {
                case ovrControllerType_TrackedRemote:
                    if (!foundRemote) {
                        foundRemote = true;
                    }
                    if (mRemoteDeviceId[deviceIndex] != curCaps.DeviceID) {
                        mRemoteDeviceId[deviceIndex] = curCaps.DeviceID;
                        onControllerConnected(deviceIndex);
                    }
                    break;
                default:
                    break;
            }
        }

        if (!foundRemote && mRemoteDeviceId[0] != ovrDeviceIdType_Invalid) {
            mRemoteDeviceId[0] = ovrDeviceIdType_Invalid;
            return false;
        }

        if (!foundRemote && mRemoteDeviceId[1] != ovrDeviceIdType_Invalid) {
            mRemoteDeviceId[1] = ovrDeviceIdType_Invalid;
            return false;
        }

        return true;
    }

    void GearController::onControllerConnected(const ovrDeviceID deviceID) {
        ovrInputTrackedRemoteCapabilities remoteCapabilities;
        remoteCapabilities.Header.Type = ovrControllerType_TrackedRemote;
        remoteCapabilities.Header.DeviceID = mRemoteDeviceId[deviceID];
        ovrResult result = vrapi_GetInputDeviceCapabilities(mOvrMobile, &remoteCapabilities.Header);
        if (result == ovrSuccess) {
            handedness = (remoteCapabilities.ControllerCapabilities & ovrControllerCaps_LeftHand) ? 0 : 1;

            ovrInputStateTrackedRemote remoteInputState;
            remoteInputState.Header.ControllerType = ovrControllerType_TrackedRemote;
            result = vrapi_GetCurrentInputState(mOvrMobile, mRemoteDeviceId[deviceID], &remoteInputState.Header);
        }
        vrapi_SetRemoteEmulation(mOvrMobile, false);
    }

    void GearController::onFrame(double predictedDisplayTime) {
        ovrTracking tracking;
        for(int i=0; i<MAX_CONTROLLERS; i++) {
            if (mRemoteDeviceId[i] != ovrDeviceIdType_Invalid) {
                mOrientationTrackingReadbackBuffer[i][0] = CONNECTED;

                mOrientationTrackingReadbackBuffer[i][1] = handedness;
                ovrResult result = vrapi_GetInputTrackingState(mOvrMobile, mRemoteDeviceId[i], predictedDisplayTime, &tracking);
                if (ovrSuccess != result) {
                    LOGW("GearController::onFrame: vrapi_GetInputTrackingState failed with %d", result);
                    return;
                }
                ovrQuatf orientation = tracking.HeadPose.Pose.Orientation;
                const glm::quat tmp(orientation.w, orientation.x, orientation.y, orientation.z);
                const glm::quat quat = glm::conjugate(glm::inverse(tmp));
                mOrientationTrackingReadbackBuffer[i][6] = quat.w;
                mOrientationTrackingReadbackBuffer[i][7] = quat.x;
                mOrientationTrackingReadbackBuffer[i][8] = quat.y;
                mOrientationTrackingReadbackBuffer[i][9] = quat.z;

                ovrInputStateTrackedRemote state;
                state.Header.ControllerType = ovrControllerType_TrackedRemote;
                vrapi_GetCurrentInputState(mOvrMobile, mRemoteDeviceId[i], &state.Header);

                mOrientationTrackingReadbackBuffer[i][2] = state.TrackpadStatus;

                if (0x20000001 == state.Buttons) {
                    state.Buttons = ovrButton_Enter;
                }
                mOrientationTrackingReadbackBuffer[i][10] = state.Buttons;
                mOrientationTrackingReadbackBuffer[i][11] = state.TrackpadPosition.x;
                mOrientationTrackingReadbackBuffer[i][12] = state.TrackpadPosition.y;

                ovrVector3f position = tracking.HeadPose.Pose.Position;
                mOrientationTrackingReadbackBuffer[i][3] = position.x;
                mOrientationTrackingReadbackBuffer[i][4] = position.y;
                mOrientationTrackingReadbackBuffer[i][5] = position.z;

            } else {
                // set disconnected
                mOrientationTrackingReadbackBuffer[i][0] = DISCONNECTED;
            }
        }
    }

    void GearController::reset() {
        mRemoteDeviceId[0] = ovrDeviceIdType_Invalid;
        mRemoteDeviceId[1] = ovrDeviceIdType_Invalid;
    }
}
