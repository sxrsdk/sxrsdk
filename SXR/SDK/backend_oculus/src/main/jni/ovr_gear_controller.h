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

#ifndef FRAMEWORK_OVR_GEAR_CONTROLLER_H
#define FRAMEWORK_OVR_GEAR_CONTROLLER_H

#include "VrApi_Input.h"

namespace sxr {
    class GearController {

    private:
        ovrDeviceID mRemoteDeviceId = ovrDeviceIdType_Invalid;
        ovrMobile *mOvrMobile;
        float *mOrientationTrackingReadbackBuffer;
        static const int CONNECTED = 1;
        static const int DISCONNECTED = 0;

    public :

        GearController(float *orientationTrackingReadbackBuffer) {
            this->mOrientationTrackingReadbackBuffer = orientationTrackingReadbackBuffer;
        }


        void setOvrMobile(ovrMobile *ovrMobile) {
            this->mOvrMobile = ovrMobile;
        }

        bool findConnectedGearController();

        void onControllerConnected(ovrDeviceID const deviceID);

        void onFrame(double predictedDisplayTime);

        int handedness;

        void reset();
    };
}
#endif //FRAMEWORK_OVR_GEAR_CONTROLLER_H
