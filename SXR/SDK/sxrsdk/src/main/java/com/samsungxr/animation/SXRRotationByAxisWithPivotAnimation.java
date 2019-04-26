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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * This animation uses
 * {@link SXRTransform#rotateByAxisWithPivot(float, float, float, float, float, float, float) }
 * to do an animated rotation about a specific axis, with a specific pivot
 * point.
 */
public class SXRRotationByAxisWithPivotAnimation extends SXRTransformAnimation {

    private final float mAngle, //
        mAxisX, mAxisY, mAxisZ, //
        mPivotX, mPivotY, mPivotZ;
    private final Quaternionf mStartRotation = new Quaternionf();
    private final Vector3f mStartPosition = new Vector3f();

    /**
     * Use
     * {@link SXRTransform#rotateByAxisWithPivot(float, float, float, float, float, float, float)}
     * to do an animated rotation about a specific axis with a specific pivot.
     *
     * @param target
     *            {@link SXRTransform} to animate.
     * @param duration
     *            The animation duration, in seconds.
     * @param angle
     *            the rotation angle, in degrees
     * @param axisX
     *            the normalized axis x component
     * @param axisY
     *            the normalized axis y component
     * @param axisZ
     *            the normalized axis z component
     * @param pivotX
     *            The x-coordinate of the pivot point
     * @param pivotY
     *            The y-coordinate of the pivot point
     * @param pivotZ
     *            The z-coordinate of the pivot point
     */
    public SXRRotationByAxisWithPivotAnimation(SXRTransform target,
                                               float duration, float angle, float axisX, float axisY, float axisZ,
                                               float pivotX, float pivotY, float pivotZ) {
        super(target, duration);
        mAngle = angle;
        mAxisX = axisX;
        mAxisY = axisY;
        mAxisZ = axisZ;
        mPivotX = pivotX;
        mPivotY = pivotY;
        mPivotZ = pivotZ;
        mStartRotation.set(mRotation);
        mStartPosition.set(mPosition);
        if (duration < 0)
        {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }

    /**
     * Use
     * {@link SXRTransform#rotateByAxisWithPivot(float, float, float, float, float, float, float)}
     * to do an animated rotation about a specific axis with a specific pivot.
     *
     * @param target
     *            {@link SXRNode} containing a {@link SXRTransform}
     * @param duration
     *            The animation duration, in seconds.
     * @param angle
     *            the rotation angle, in degrees
     * @param axisX
     *            the normalized axis x component
     * @param axisY
     *            the normalized axis y component
     * @param axisZ
     *            the normalized axis z component
     * @param pivotX
     *            The x-coordinate of the pivot point
     * @param pivotY
     *            The y-coordinate of the pivot point
     * @param pivotZ
     *            The z-coordinate of the pivot point
     */
    public SXRRotationByAxisWithPivotAnimation(SXRNode target,
                                               float duration, float angle, float axisX, float axisY, float axisZ,
                                               float pivotX, float pivotY, float pivotZ) {
        this(target.getTransform(), duration, angle, axisX, axisY, axisZ,
             pivotX, pivotY, pivotZ);
        String name = target.getName();
        if ((name != null) && (mName == null))
        {
            setName(name + ".rotation");
        }
    }

    @Override
    public void animate(float timeInSec)
    {
        float ratio = timeInSec / mDuration;
        // Reset rotation and position (this is pretty cheap - SXRF uses a 'lazy
        // update' policy on the matrix, so three changes don't cost all that
        // much more than one)
        mTransform.setRotation(mStartRotation.w, mStartRotation.x, mStartRotation.y, mStartRotation.z);
        mTransform.setPosition(mStartPosition.x, mStartPosition.y, mStartPosition.z);

        // Rotate with pivot, from start orientation & position
        float angle = ratio * mAngle;
        mTransform.rotateByAxisWithPivot(angle, mAxisX, mAxisY, mAxisZ,
                                         mPivotX, mPivotY, mPivotZ);
    }
}