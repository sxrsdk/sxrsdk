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

package com.samsungxr.animation;

import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderData;

/** Animate a {@link SXRShaderData}. */
public abstract class SXRPostEffectAnimation extends SXRAnimation {

    protected final SXRShaderData mPostEffectData;

    /**
     * Sets the {@code protected final SXRPostEffectData mPostEffectData} field.
     * 
     * @param target
     *            {@link SXRShaderData} to animate
     * @param duration
     *            The animation duration, in seconds.
     */
    protected SXRPostEffectAnimation(SXRShaderData target, float duration) {
        super(target, duration);
        mPostEffectData = target;
    }
}
