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

/**
 * Animates the translation component of a transform.
 * <p>
 * The rotation and scale components are unchanged and
 * may be simultaneously updated by another animation.
 * @see SXRTransformAnimation
 * @see SXRRotationByAxisAnimation
 */
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
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
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
        String name = target.getName();
        if (name != null)
        {
            setName(name + ".position");
        }
    }

    /**
     * Construct one position animation based on another.
     * <p>
     * Both animations will affect the same target node.
     * @param src {@link SXRPositionAnimation} to copy.
     */
    public SXRPositionAnimation(final SXRPositionAnimation src)
    {
        super(src.mTransform, src.mDuration);
        mStartX = src.mStartX;
        mStartY = src.mStartY;
        mStartZ = src.mStartZ;
        mDeltaX = src.mDeltaX;
        mDeltaY = src.mDeltaY;
        mDeltaZ = src.mDeltaZ;
    }

    @Override
    public SXRAnimation copy()
    {
        return new SXRPositionAnimation(this);
    }

    @Override
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        mTransform.setPosition(mStartX + ratio * mDeltaX, mStartY + ratio
                * mDeltaY, mStartZ + ratio * mDeltaZ);
    }
}
