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

/**
 * Implement this interface to create a callback that's called once per frame.
 * 
 * Activate your callbacks with
 * {@link SXRContext#registerDrawFrameListener(SXRDrawFrameListener)};
 * deactivate a callback with
 * {@link SXRContext#unregisterDrawFrameListener(SXRDrawFrameListener)}.
 * Per-frame callbacks are called after the
 * {@linkplain SXRContext#runOnGlThread(Runnable) 'one shot' queue} has been
 * emptied and before {@link SXRMain#onStep()}.
 */
public interface SXRDrawFrameListener {
    /**
     * Called each time a frame is drawn. Callbacks are called in subscription-order, after any
     * {@linkplain SXRContext#runOnGlThread(Runnable) 'one shot' callbacks} and before
     * {@link SXRMain#onStep()}.
     * 
     * @param frameTime
     *            Seconds since the previous frame - see
     *            {@link SXRContext#getFrameTime()}
     */
    public void onDrawFrame(float frameTime);
}
