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

/** Size animation. */
public class SXRPositionAnimation extends SXRTransformAnimation
{
    private final float mStartX, mStartY, mStartZ;
    private final float mDeltaX, mDeltaY, mDeltaZ;

    /**
     * Position the transform, by potentially different amounts in each direction.
     * 
     * @param target
     *            {@link SXRTransform} to animate.
     * @param duration
     *            The animation duration, in seconds.
     * @param positionX
     *            Target x position
     * @param positionY
     *            Target y position
     * @param positionZ
     *            Target z position
     */
    public SXRPositionAnimation(SXRTransform target, float duration, float positionX,
            float positionY, float positionZ) {
        super(target, duration);

        mStartX = mPosition.x;
        mStartY = mPosition.y;
        mStartZ = mPosition.z;

        mDeltaX = positionX - mStartX;
        mDeltaY = positionY - mStartY;
        mDeltaZ = positionZ - mStartZ;
    }

    /**
     * Position the transform, by potentially different amounts in each direction.
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param positionX
     *            Target x position
     * @param positionY
     *            Target y position
     * @param positionZ
     *            Target z position
     */
    public SXRPositionAnimation(SXRNode target, float duration,
            float positionX, float positionY, float positionZ)
    {
        this(target.getTransform(), duration, positionX, positionY, positionZ);
    }

    @Override
    protected void animate(SXRHybridObject target, float ratio)
    {
        mTransform.setPosition(mStartX + ratio * mDeltaX, mStartY + ratio
                * mDeltaY, mStartZ + ratio * mDeltaZ);
    }
}
