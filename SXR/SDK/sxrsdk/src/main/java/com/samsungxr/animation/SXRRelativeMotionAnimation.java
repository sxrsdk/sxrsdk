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

import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTransform;

/** Animate an object's position. */
public class SXRRelativeMotionAnimation extends SXRTransformAnimation
{

    private final float mStartX, mDeltaX, mStartY, mDeltaY, mStartZ, mDeltaZ;

    /**
     * Animate a move by delta x/y/z.
     * 
     * @param target
     *            {@link SXRTransform} to animate
     * @param duration
     *            The animation duration, in seconds.
     * @param deltaX
     *            The value to add to x
     * @param deltaY
     *            The value to add to y
     * @param deltaZ
     *            The value to add to z
     */
    public SXRRelativeMotionAnimation(SXRTransform target, float duration,
            float deltaX, float deltaY, float deltaZ) {
        super(target, duration);

        mStartX = mPosition.x;
        mStartY = mPosition.y;
        mStartZ = mPosition.z;

        mDeltaX = deltaX;
        mDeltaY = deltaY;
        mDeltaZ = deltaZ;
    }

    /**
     * Animate a move by delta x/y/z.
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param deltaX
     *            The value to add to x
     * @param deltaY
     *            The value to add to y
     * @param deltaZ
     *            The value to add to z
     */
    public SXRRelativeMotionAnimation(SXRNode target, float duration,
            float deltaX, float deltaY, float deltaZ) {
        this(target.getTransform(), duration, deltaX, deltaY, deltaZ);
    }

    @Override
    protected void animate(SXRHybridObject target, float ratio) {
        mTransform.setPosition(mStartX + mDeltaX * ratio, //
                mStartY + mDeltaY * ratio, //
                mStartZ + mDeltaZ * ratio);
    }
}
