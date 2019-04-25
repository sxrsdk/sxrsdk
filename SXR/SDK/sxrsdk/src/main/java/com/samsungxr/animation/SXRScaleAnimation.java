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
public class SXRScaleAnimation extends SXRTransformAnimation
{
    private final float mStartX, mStartY, mStartZ;
    private final float mDeltaX, mDeltaY, mDeltaZ;

    /**
     * Scale the transform, by potentially different amounts in each dimension.
     * 
     * Note that the scale parameters are the target scale, not a multiplier
     * applied to the current scale. For example, if
     * {@link SXRTransform#getScaleX() getScaleX()} returns 4, animating to 2
     * will <em>shrink</em> the object, not grow it.
     * 
     * @param target
     *            {@link SXRTransform} to animate.
     * @param duration
     *            The animation duration, in seconds.
     * @param scaleX
     *            Target x scale
     * @param scaleY
     *            Target y scale
     * @param scaleZ
     *            Target z scale
     */
    public SXRScaleAnimation(SXRTransform target, float duration, float scaleX,
            float scaleY, float scaleZ)
    {
        super(target, duration);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        mStartX = target.getScaleX();
        mStartY = target.getScaleY();
        mStartZ = target.getScaleZ();
        mDeltaX = scaleX - mStartX;
        mDeltaY = scaleY - mStartY;
        mDeltaZ = scaleZ - mStartZ;
    }

    /**
     * Scale the transform, by the same amount in each dimension.
     * 
     * Note that the scale parameter is the target scale, not a multiplier
     * applied to the current scale. For example, if
     * {@link SXRTransform#getScaleX() getScaleX()} returns 4, animating to 2
     * will <em>shrink</em> the object, not grow it.
     * 
     * @param target
     *            {@link SXRTransform} to animate.
     * @param duration
     *            The animation duration, in seconds.
     * @param scale
     *            Target scale
     */
    public SXRScaleAnimation(SXRTransform target, float duration, float scale)
    {
        this(target, duration, scale, scale, scale);
    }

    /**
     * Scale the transform, by potentially different amounts in each dimension.
     * 
     * Note that the scale parameters are the target scale, not a multiplier
     * applied to the current scale. For example, if
     * {@link SXRTransform#getScaleX() getScaleX()} returns 4, animating to 2
     * will <em>shrink</em> the object, not grow it.
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param scaleX
     *            Target x scale
     * @param scaleY
     *            Target y scale
     * @param scaleZ
     *            Target z scale
     */
    public SXRScaleAnimation(SXRNode target, float duration,
            float scaleX, float scaleY, float scaleZ)
    {
        this(target.getTransform(), duration, scaleX, scaleY, scaleZ);
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".scale");
        }
    }

    /**
     * Scale the transform, by the same amount in each dimension.
     * 
     * Note that the scale parameter is the target scale, not a multiplier
     * applied to the current scale. For example, if
     * {@link SXRTransform#getScaleX() getScaleX()} returns 4, animating to 2
     * will <em>shrink</em> the object, not grow it.
     * 
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param scale
     *            Target scale
     */
    public SXRScaleAnimation(SXRNode target, float duration, float scale)
    {
        this(target, duration, scale, scale, scale);
    }

    @Override
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        mTransform.setScale(mStartX + ratio * mDeltaX, mStartY + ratio
                * mDeltaY, mStartZ + ratio * mDeltaZ);
    }
}
