/* Copyright 2018 Samsung Electronics Co., LTD
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

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.samsungxr.utility.Log;


/**
 * Interpolate the poses or blend the skeleton animations.
 * <p>
 * The blending is performed by interpolating values between two poses
 * which contain translations, rotations and scaling to apply to the bone
 * over time.
 * <p>
 * The source poses may be animated over time independently.
 * The pose of the destination skeleton is replaced by
 * the blended pose.

 * @see SXRSkeleton
 * @see SXRPose
 */
public class SXRPoseInterpolator extends SXRAnimation
{
    private SXRSkeleton mDestSkeleton;
    private SXRPose mInputPose;
    Vector3f mTempVec1 = new Vector3f();
    Vector3f mTempVec2 = new Vector3f();
    Quaternionf mTempQuat1 = new Quaternionf();
    Quaternionf mTempQuat2 = new Quaternionf();
    Matrix4f mTempMtx = new Matrix4f();

    /**
     * Create a blending animation between two given skeletons
     *
     * @param duration Duration of the animation in seconds.
     * @param target target skeleton to get blended pose
     * @param source source skeleton
     */
    public SXRPoseInterpolator(SXRSkeleton target, SXRSkeleton source, float duration)
    {
        super(target, duration);
        mDestSkeleton = target;
        mInputPose = source.getPose();
    }

    /**
     * Create a blending animation between a static pose and a skeleton.
     *
     * @param duration Duration of the animation in seconds.
     * @param target target skeleton to get blended pose
     * @param pose source pose
     */
    public SXRPoseInterpolator(SXRSkeleton target, SXRPose pose, float duration)
    {
        super(target, duration);
        mDestSkeleton = target;
        mInputPose = pose;
    }

    /**
     * Calculate the blend pose from two input poses.
     * The blend is from mDuration to 0 for first skeleton animation that is first skeleton animation smoothly disappears
     * The blend is from 0 to mDuration for second skeleton animation that is second skeleton animation smoothly appears
     *
     * @param timer animation time in seconds.
     */
    public void animate(float timer)
    {
        SXRPose pose2 = mDestSkeleton.getPose();
        for (int k = 0; k < mDestSkeleton.getNumBones(); k++)
        {
            float t = timer / getDuration();

            mInputPose.getLocalMatrix(k, mTempMtx);
            mInputPose.getLocalRotation(k, mTempQuat1);
            pose2.getLocalRotation(k, mTempQuat2);
            mTempQuat1.slerp(mTempQuat2, t, mTempQuat2);
            mTempMtx.set(mTempQuat2);
            mInputPose.getLocalScale(k, mTempVec2);
            mTempMtx.scale(mTempVec2);
            mInputPose.getLocalPosition(k, mTempVec1);
            pose2.getLocalPosition(k, mTempVec2);
            mTempVec1.lerp(mTempVec2, t, mTempVec2);
            mTempMtx.setTranslation(mTempVec2.x,  mTempVec2.y, mTempVec2.z);
            pose2.setLocalMatrix(k, mTempMtx);
        }
        pose2.sync();
    }
}