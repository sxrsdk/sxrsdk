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
import com.samsungxr.utility.Log;
import org.joml.Quaternionf;

/**
 * Animates the rotation about a specified axis.
 * <p>
 * Only the rotation component of the transform
 * are affected. he position and scale components are
 * unchanged and may be simultaneously updated by another animation.
 * @see SXRTransformAnimation
 * @see SXRRotationByAxisWithPivotAnimation
 */
public class SXRRotationByAxisAnimation extends SXRTransformAnimation
{
    private final float mAngle, mAxisX, mAxisY, mAxisZ;
    private final Quaternionf mStartRotation;

    /**
     * Use {@link SXRTransform#rotateByAxis(float, float, float, float)} to do
     * an animated rotation about a specific axis.
     *
     * @param target
     *            {@link SXRTransform} to animate.
     * @param duration
     *            The animation duration, in seconds.
     * @param angle
     *            the rotation angle, in degrees
     * @param x
     *            the normalized axis x component
     * @param y
     *            the normalized axis y component
     * @param z
     *            the normalized axis z component
     */
    public SXRRotationByAxisAnimation(SXRTransform target, float duration,
                                      float angle, float x, float y, float z)
    {
        super(target, duration);
        mAngle = angle;
        mAxisX = x;
        mAxisY = y;
        mAxisZ = z;
        mStartRotation = new Quaternionf();
        mStartRotation.set(mRotation);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    /**
     * Construct a rotation animation to rotate about the given axis.
     * <p>
     * This will rotate about (0, 0, 0) in the local coordinate
     * system of the object. To rotate about a different point,
     * use {@link SXRRotationByAxisWithPivotAnimation}.
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param angle
     *            the rotation angle, in degrees
     * @param x
     *            the normalized axis x component
     * @param y
     *            the normalized axis y component
     * @param z
     *            the normalized axis z component
     */
    public SXRRotationByAxisAnimation(SXRNode target, float duration,
                                      float angle, float x, float y, float z)
    {
        this(target.getTransform(), duration, angle, x, y, z);
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".rotation");
        }
    }

    /**
     * Construct a copy of another rotation animation.
     * @param src rotation animation to copy.
     */
    public SXRRotationByAxisAnimation(final SXRRotationByAxisAnimation src)
    {
        super(src.mTransform, src.mDuration);
        mAngle = src.mAngle;
        mAxisX = src.mAxisX;
        mAxisY = src.mAxisY;
        mAxisZ = src.mAxisZ;
        mStartRotation = src.mStartRotation;
    }

    @Override
    public SXRAnimation copy()
    {
        return new SXRRotationByAxisAnimation(this);
    }

    @Override
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        float angle = ratio * mAngle;

        mRotation.fromAxisAngleDeg(mAxisX, mAxisY, mAxisZ, angle);
        mRotation.mul(mStartRotation);
        mTransform.setRotation(mRotation.w, mRotation.x, mRotation.y, mRotation.z);
    }
}