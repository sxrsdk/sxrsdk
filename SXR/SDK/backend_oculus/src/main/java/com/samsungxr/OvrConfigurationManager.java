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

package com.samsungxr;

final class OvrConfigurationManager extends SXRConfigurationManager {

    OvrConfigurationManager(SXRApplication application) {
        super(application);
    }

    @Override
    public boolean isHmtConnected() {

        final SXRApplication application = (SXRApplication) mApplication.get();
        if (null == application) {
            return false;
        }
        return nativeIsHmtConnected(application.getNative());
    }

    private static native boolean nativeIsHmtConnected(long ptr);
}
