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

package com.samsungxr;

import com.samsungxr.utility.Log;

class SplashScreen extends SXRNode {

    private static final String TAG = Log.tag(SplashScreen.class);

    private boolean mCloseRequested;
    /**
     * Earliest time to close splash screen. Unit is nanos, as returned by
     * {@link SXRTime#getCurrentTime()}
     */
    final long mTimeout;

    SplashScreen(SXRContext gvrContext, SXRMesh mesh, SXRTexture texture,
            SXRShaderId shaderId, SXRMain script) {
        super(gvrContext, mesh, texture, shaderId);
        mCloseRequested = false; // unnecessary, but ...

        float splashDisplayTime = script.getSplashDisplayTime();

        // check if we have a splash display time, else request for
        // an immediate timeout
        if (splashDisplayTime < 0f) {
            mTimeout = Long.MAX_VALUE;
        } else {
            long currentTime = SXRTime.getCurrentTime();
            mTimeout = currentTime + (long) (splashDisplayTime * 1e9f);
            Log.d(TAG, "currentTime = %,d, timeout = %,d", currentTime, mTimeout);
        }
    }

    void closeSplashScreen() {
        mCloseRequested = true;
        Log.d(TAG, "close()");
    }

    boolean closeRequested() {
        return mCloseRequested;
    }

}
