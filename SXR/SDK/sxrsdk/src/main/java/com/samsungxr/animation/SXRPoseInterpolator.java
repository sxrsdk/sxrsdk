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
import com.samsungxr.animation.keyframe.SXRFloatAnimation;
import com.samsungxr.animation.keyframe.SXRQuatAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static com.samsungxr.animation.SXRPose.Bone;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

/**
 * Interpolate the poses or blend the skeleton animations.
 * <p>
 * The blending is performed by interpolating values between two poses
 * which contain translations, rotations and scaling to apply to the bone
 * over time.
 * <p>
 * Each frame the animation compute the matrix for animating
 * that bone. This matrix is used to update the current pose
 * of the skeleton and the nodes associated with the bone.
 * After all channels have been evaluated, the skinning pose is
 * computed to drive skinned meshes.
 * @see SXRSkeleton
 * @see com.samsungxr.animation.SXRSkin
 * @see SXRPose
 */
public class SXRPoseInterpolator extends SXRAnimation
{
    private SXRPose initialPose;
    private SXRPose finalPose;
    private Bone[] mBones;
    private SXRSkeleton mSkeleton;
    private SXRNode modelTarget;

    private Vector3f poseOnePos =  new Vector3f(0,0,0);
    private Vector3f poseTwoPos =  new Vector3f(0,0,0);
    private Vector3f poseOneScl =  new Vector3f(0,0,0);
    private Vector3f poseTwoScl =  new Vector3f(0,0,0);
    private Quaternionf poseOneRot =  new Quaternionf(0,0,0,1);
    private Quaternionf poseTwoRot =  new Quaternionf(0,0,0,1);

    private Vector3f tempVec = new Vector3f(0,0,0);
    private Quaternionf tempQuat = new Quaternionf(0,0,0,1);

    private SXRQuatAnimation mRotInterpolator;
    private SXRFloatAnimation mPosInterpolator;
    private SXRFloatAnimation mSclInterpolator;

    private float[] rotData = new float[10];
    private float[] posData = new float[8];
    private float[] sclData = new float[8];
    private float[] poseData;
    private float[] posInterpolatedData = new float[3];
    private float[] sclInterpolatedData = new float[3];
    private float[] rotInterpolatedData = new float[4];

    private int poseDataSize = 20;
    private int initialPosIndex=0; //initial position index for bone 0 in the poseData array
    private int finalPosIndex=10;  //final position index for bone 0 in the poseData array
    private int initialRotIndex=3; //initial rotation index for bone 0 in the poseData array
    private int finalRotIndex=13;  //final rotation index for bone 0 in the poseData array
    private int initialSclIndex=7; //initial scale index for bone 0 in the poseData array
    private int finalSclIndex=17;  //final scale index for bone 0 in the poseData array

    private float startTime;
    private float endTime;
    private int startTimeIndex = 0;
    private int endTimeIndex =  4;
    private int offset = 0;
    private Matrix4f mat;
    private float mDuration;

    //dynamic update
    private SXRSkeletonAnimation skelAnimSrc;
    private SXRSkeletonAnimation skelAnimDest;
    private float[][] posBlend;
    private float[][] rotBlend;
    private float[][] sclBlend;
    private boolean poseBlend = false;
    private boolean mReverse = false;

    /**
     * Create a interpolation animation between two given poses.
     *
     * @param target The target hierachy containing nodes for bones.
     * @param duration Duration of the animation in seconds.
     * @param poseOne start pose.
     * @param poseTwo end pose.
     * @param skeleton The skeleton being animated.
     */
    public SXRPoseInterpolator(SXRNode target, float duration, SXRPose poseOne, SXRPose poseTwo, SXRSkeleton skeleton)
    {
        super(target, duration);

        modelTarget = target;

        initialPose = poseOne;
        finalPose = poseTwo;
        mSkeleton = skeleton;

        startTime = 0;
        endTime =  duration;

        mBones = new Bone[mSkeleton.getNumBones()];
        poseData = new float[poseDataSize*mSkeleton.getNumBones()];
        mDuration = duration;

        for (int i = 0; i < mSkeleton.getNumBones(); i++)
        {
            setAnimationData(i);
        }
        mat = new Matrix4f();
        poseBlend = true;
    }

    /**
     * Create a blending animation between two given skeleton animations.
     *
     * @param target The target hierachy containing nodes for bones.
     * @param duration Duration of the animation in seconds.
     * @param skelOne first skeleton animation.
     * @param skelTwo second skeleton animation.
     * @param skeleton The skeleton being animated.
     * @param reverse determines whether the blending animation is to be played reverse.
     */
    public SXRPoseInterpolator(SXRNode target, float duration, SXRSkeletonAnimation skelOne, SXRSkeletonAnimation skelTwo, SXRSkeleton skeleton, boolean reverse)
    {
        super(target, duration);
        modelTarget = target;

        skelAnimSrc = skelOne;
        skelAnimDest = skelTwo;
        mSkeleton = skeleton;

        posBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][6];
        rotBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][8];
        sclBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][6];

        startTime = 0;
        endTime =  duration;

        mBones = new Bone[mSkeleton.getNumBones()];
        poseData = new float[poseDataSize * mSkeleton.getNumBones()];
        mDuration = duration;
        mReverse = reverse;

        if(!reverse)
        {
            initialPose = skelAnimSrc.computePose(skelAnimSrc.getDuration()-mDuration, skelAnimSrc.getSkeleton().getPose());
            finalPose = skelAnimDest.computePose(0, skelAnimDest.getSkeleton().getPose());
        }
        else
        {
            initialPose = skelAnimSrc.computePose(0+mDuration, skelAnimSrc.getSkeleton().getPose());
            finalPose = skelAnimDest.computePose(skelAnimSrc.getDuration(), skelAnimDest.getSkeleton().getPose());
        }

        for (int i = 0; i < mSkeleton.getNumBones(); i++)
        {
            setAnimationData(i);
        }
        mat = new Matrix4f();
    }

    public void setAnimationData(int index)
    {
        setPosePositions(index, initialPose, finalPose);
        setPoseRotations(index, initialPose, finalPose);
        setPoseScale(index, initialPose, finalPose);
    }

    /**
     * Retrive and set the animation data with position values from the initial and final pose.
     *
     * @param index bone id number.
     * @param initialPose start pose of the skeleton.
     * @param finalPose end pose of the skeleton.
     */
    private void setPosePositions(int index, SXRPose initialPose, SXRPose finalPose)
    {
        initialPose.getLocalPosition(index,poseOnePos);
        finalPose.getLocalPosition(index,poseTwoPos);
        offset = index*poseDataSize;

        setPosePositions(offset+initialPosIndex,poseOnePos);
        setPosePositions(offset+finalPosIndex,poseTwoPos);
    }

    public void setPosePositions(int posOffset, Vector3f posePos)
    {
        poseData[posOffset]=posePos.x();
        poseData[posOffset+1]=posePos.y();
        poseData[posOffset+2]=posePos.z();
    }

    /**
     * Retrive and set the animation data with rotation values from the initial and final pose.
     *
     * @param index bone id number.
     * @param initialPose start pose of the skeleton.
     * @param finalPose end pose of the skeleton.
     */
    private void setPoseRotations(int index, SXRPose initialPose, SXRPose finalPose)
    {
        initialPose.getLocalRotation(index,poseOneRot);
        finalPose.getLocalRotation(index,poseTwoRot);
        offset = index*poseDataSize;

        setPoseRotations(offset+initialRotIndex,poseOneRot);
        setPoseRotations(offset+finalRotIndex,poseTwoRot);
    }

    public void setPoseRotations(int rotOffset, Quaternionf poseRot)
    {
        poseData[rotOffset]=poseRot.x();
        poseData[rotOffset+1]=poseRot.y();
        poseData[rotOffset+2]=poseRot.z();
        poseData[rotOffset+3]=poseRot.w();
    }

    /**
     * Retrive and set the animation data with scale values from the initial and final pose.
     *
     * @param index bone id number.
     * @param initialPose start pose of the skeleton.
     * @param finalPose end pose of the skeleton.
     */
    private void setPoseScale(int index, SXRPose initialPose, SXRPose finalPose)
    {
        initialPose.getLocalScale(index,poseOneScl);
        finalPose.getLocalScale(index,poseTwoScl);
        offset = index*poseDataSize;

        setPoseScale(offset+initialSclIndex,poseOneScl);
        setPoseScale(offset+finalSclIndex,poseTwoScl);
    }
    public void setPoseScale(int sclOffset, Vector3f poseScl)
    {
        poseData[sclOffset]=poseScl.x();
        poseData[sclOffset+1]=poseScl.y();
        poseData[sclOffset+2]=poseScl.z();
    }


    /**
     * Update the position values to send through the linear interpolator.
     *
     * @param offset position index in animation data.
     * @param startTime previous time in animation, time in sec.
     * @param endTime current time in animation, time in sec.
     */
    public void updatePos(int offset, float startTime, float endTime)
    {
        posData[startTimeIndex] = startTime;
        posData[endTimeIndex] = endTime;
        updatePos(1, offset+initialPosIndex);
        updatePos(5, offset+finalPosIndex);
    }

    public void updatePos(int pos, int posOffset)
    {
        posData[pos] = poseData[posOffset];
        posData[pos+1] = poseData[posOffset+1];
        posData[pos+2] = poseData[posOffset+2];
    }

    /**
     * Update the rotation values to send through the spherical interpolator.
     *
     * @param offset rotation index in animation data.
     * @param startTime previous time in animation, time in sec.
     * @param endTime current time in animation, time in sec.
     */
    public void updateRot(int offset, float startTime, float endTime)
    {
        rotData[startTimeIndex] = startTime;
        rotData[endTimeIndex+1] = endTime;
        updateRot(1, offset+initialRotIndex);
        updateRot(6, offset+finalRotIndex);
    }

    public void updateRot(int rot, int rotOffset)
    {
        rotData[rot] = poseData[rotOffset];
        rotData[rot+1] = poseData[rotOffset+1];
        rotData[rot+2] = poseData[rotOffset+2];
        rotData[rot+3] = poseData[rotOffset+3];
    }

    /**
     * Update the scale values to send through the linear interpolator.
     *
     * @param offset scale index in animation data.
     * @param startTime previous time in animation, time in sec.
     * @param endTime current time in animation, time in sec.
     */
    public void updateScl(int offset, float startTime, float endTime)
    {
        sclData[startTimeIndex] = startTime;
        sclData[endTimeIndex] = endTime;
        updateScl(1, offset+initialSclIndex);
        updateScl(5, offset+finalSclIndex);
    }
    public void updateScl(int scl, int sclOffset)
    {
        sclData[scl] = poseData[sclOffset];
        sclData[scl+1] = poseData[sclOffset+1];
        sclData[scl+2] = poseData[sclOffset+2];
    }

    /**
     * Calculate the blend pose from two skeleton animations.
     * The blend is from mDuration to 0 for first skeleton animation that is first skeleton animation smoothly disappears
     * The blend is from 0 to mDuration for second skeleton animation that is second skeleton animation smoothly appears
     *
     * @param timer animation time in seconds.
     */
    public void getFinalPose(float timer)
    {
        Log.i("printskelAnimSrc","Interp "+timer+this.getNameAll());

        SXRPose firstPose = null;
        SXRPose secondPose = null;

        if(!mReverse)
        {
            firstPose = skelAnimSrc.computePose(skelAnimSrc.getDuration()-mDuration+timer,skelAnimSrc.getSkeleton().getPose());
            secondPose = skelAnimDest.computePose(0+timer,skelAnimDest.getSkeleton().getPose());
        }
        else
        {
            firstPose = skelAnimSrc.computePose(mDuration-timer,skelAnimSrc.getSkeleton().getPose());
            secondPose = skelAnimDest.computePose(skelAnimDest.getDuration()-timer,skelAnimDest.getSkeleton().getPose());
        }

        float mul = 1/mDuration;

        for(int k =0; k<skelAnimSrc.getSkeleton().getNumBones();k++)
        {
            Vector3f posI = new Vector3f(0,0,0);
            firstPose.getLocalPosition(k,posI);
            Vector3f posF = new Vector3f(0,0,0);
            secondPose.getLocalPosition(k,posF);

            posBlend[k][3] = ((mDuration-timer)*mul*posI.x)+(posF.x*(timer*mul));
            posBlend[k][4] = ((mDuration-timer)*mul*posI.y)+(posF.y*timer*mul);
            posBlend[k][5] = ((mDuration-timer)*mul*posI.z)+(posF.z*timer*mul);

            finalPose.setLocalPosition(k,posBlend[k][3],  posBlend[k][4],  posBlend[k][5]);

            Quaternionf q1 = new Quaternionf(0,0,0,1);
            firstPose.getLocalRotation(k,q1);
            Quaternionf q2 = new Quaternionf(0,0,0,1);
            secondPose.getLocalRotation(k,q2);

            rotBlend[k][4] = ((mDuration-timer)*mul*q1.x)+(q2.x*timer*mul);
            rotBlend[k][5] = ((mDuration-timer)*mul*q1.y)+(q2.y*timer*mul);
            rotBlend[k][6] = ((mDuration-timer)*mul*q1.z)+(q2.z*timer*mul);
            rotBlend[k][7] = ((mDuration-timer)*mul*q1.w)+(q2.w*timer*mul);

            finalPose.setLocalRotation(k,rotBlend[k][4],  rotBlend[k][5],  rotBlend[k][6],  rotBlend[k][7]);

        }
    }

    @Override
    protected void animate(SXRHybridObject target, float ratio)
    {
        animate(mDuration * ratio);
    }

    /**
     * Compute pose of skeleton at the given time from the animation data.
     * @param timer animation time in seconds.
     */
    public void animate(float timer) {

        initialPose = mSkeleton.getPose();
        if(!poseBlend)
        {
            endTime=timer;
            getFinalPose(timer);
        }

        for(int  i= 0;i < mSkeleton.getNumBones();i++)
        {
            offset = i*poseDataSize;

            setPosePositions(i, initialPose, finalPose);
            setPoseRotations(i,initialPose, finalPose);
            setPoseScale(i,initialPose, finalPose);

            updatePos(offset, startTime, endTime);
            updateRot(offset, startTime, endTime);
            updateScl(offset, startTime, endTime);

            mPosInterpolator = new SXRFloatAnimation(posData, 4);
            mRotInterpolator = new SXRQuatAnimation(rotData);
            mSclInterpolator = new SXRFloatAnimation(sclData, 4);

            mPosInterpolator.animate(timer,posInterpolatedData);
            mRotInterpolator.animate(timer,rotInterpolatedData);
            mSclInterpolator.animate(timer,sclInterpolatedData);

            mat.translationRotateScale(posInterpolatedData[0], posInterpolatedData[1], posInterpolatedData[2],rotInterpolatedData[0], rotInterpolatedData[1], rotInterpolatedData[2], rotInterpolatedData[3],sclInterpolatedData[0], sclInterpolatedData[1], sclInterpolatedData[2]);
            initialPose.setLocalMatrix(i, mat);
        }

        mSkeleton.poseToBones();
        mSkeleton.updateBonePose();
        mSkeleton.updateSkinPose();

        if(!poseBlend)
        {
            startTime=timer;
        }
    }
}