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
import com.samsungxr.SXRPicker;
import com.samsungxr.animation.keyframe.SXRFloatAnimation;
import com.samsungxr.animation.keyframe.SXRQuatAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static com.samsungxr.animation.SXRPose.Bone;

import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;

public class SXRPoseInterpolator extends SXRAnimation
{
    private SXRPose initialPose;
    private SXRPose finalPose;
    // private SXRSkeleton pSkeleton;
    private Bone[] mBones;

    private Vector3f poseOnePos;
    private Vector3f poseTwoPos;
    private Vector3f poseOneScl;
    private Vector3f poseTwoScl;
    private Quaternionf poseOneRot;
    private Quaternionf poseTwoRot;

    private Vector3f tempVec;
    private Quaternionf tempQuat;

    private SXRQuatAnimation mRotInterpolator;
    private SXRFloatAnimation mPosInterpolator;
    private SXRFloatAnimation mSclInterpolator;

    private float mDuration;
    private float[] rotData;
    private float[] posData;
    private float[] sclData;
    private float[] poseData;
    private float[] posIData;
    private float[] sclIData;
    private float[] rotIData;

    private int poseDataSize;
    private int initialPosIndex;
    private int finalPosIndex;
    private int initialRotIndex;
    private int finalRotIndex;
    private int initialSclIndex;
    private int finalSclIndex;
    private float startTime;
    private float endTime;
    private int startTimeIndex;
    private int endTimeIndex;
    private int offset;
    private Matrix4f mat;
    private SXRNode modelTarget;

    //dynamic update
    private SXRSkeletonAnimation skelAnimSrc;
    private SXRSkeletonAnimation skelAnimDest;
    private SXRSkeleton mSkeleton;
    private float[][] posBlend;
    private float[][] rotBlend;
    private float[][] sclBlend;
    static float frameTime =0;
    private boolean poseBlend = false;
    private boolean mReverse = false;
    public SXRPoseInterpolator(SXRNode target, float duration, SXRPose poseOne, SXRPose poseTwo, SXRSkeleton skeleton)
    {
        super(target, duration);

        modelTarget = target;

        initialPose = poseOne;
        finalPose = poseTwo;
        mSkeleton = skeleton;

        poseOnePos =  new Vector3f(0,0,0);
        poseTwoPos =  new Vector3f(0,0,0);
        poseOneScl =  new Vector3f(0,0,0);
        poseTwoScl =  new Vector3f(0,0,0);
        poseOneRot =  new Quaternionf(0,0,0,1);
        poseTwoRot =  new Quaternionf(0,0,0,1);

        rotData = new float[10];
        posData = new float[8];
        sclData = new float[8];

        posIData = new float[3];
        sclIData = new float[3];
        rotIData = new float[4];

        tempVec = new Vector3f(0,0,0);
        tempQuat = new Quaternionf(0,0,0,1);
        initialPosIndex=0; //initial position index
        finalPosIndex=10; //final position index
        initialRotIndex=3; //initial rotation index
        finalRotIndex=13;  //final rotation index
        initialSclIndex=7;
        finalSclIndex=17;
        startTime = 0;
        endTime =  duration;
        startTimeIndex = 0;
        endTimeIndex =  4;
        offset = 0;
        poseDataSize = 20;
        mBones = new Bone[mSkeleton.getNumBones()];
        poseData = new float[poseDataSize*mSkeleton.getNumBones()];
        mDuration = duration;

        for (int i = 0; i < mSkeleton.getNumBones(); i++)
        {
            poseInterpolate(i);
        }
        mat = new Matrix4f();
        poseBlend = true;
    }


    //dynamic update

    public SXRPoseInterpolator(SXRNode target, float duration, SXRSkeletonAnimation skelOne, SXRSkeletonAnimation skelTwo, SXRSkeleton skeleton, boolean reverse)
    {

        super(target, duration);

        modelTarget = target;

        skelAnimSrc = skelOne;
        skelAnimDest = skelTwo;
        mSkeleton = skeleton;

        poseOnePos =  new Vector3f(0,0,0);
        poseTwoPos =  new Vector3f(0,0,0);
        poseOneScl =  new Vector3f(0,0,0);
        poseTwoScl =  new Vector3f(0,0,0);
        poseOneRot =  new Quaternionf(0,0,0,1);
        poseTwoRot =  new Quaternionf(0,0,0,1);

        rotData = new float[10];
        posData = new float[8];
        sclData = new float[8];

        posIData = new float[3];
        sclIData = new float[3];
        rotIData = new float[4];

        posBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][6];
        rotBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][8];
        sclBlend = new float[skelAnimSrc.getSkeleton().getNumBones()][6];

        tempVec = new Vector3f(0,0,0);
        tempQuat = new Quaternionf(0,0,0,1);
        initialPosIndex=0; //initial position index
        finalPosIndex=10; //final position index
        initialRotIndex=3; //initial rotation index
        finalRotIndex=13;  //final rotation index
        initialSclIndex=7;
        finalSclIndex=17;
        startTime = 0;
        endTime =  duration;
        startTimeIndex = 0;
        endTimeIndex =  4;
        offset = 0;
        poseDataSize = 20;
        mBones = new Bone[mSkeleton.getNumBones()];
        poseData = new float[poseDataSize*mSkeleton.getNumBones()];
        mDuration = duration;
        mReverse = reverse;

        if(!reverse)
        {
            initialPose = skelAnimSrc.computePose(skelAnimSrc.getDuration()-mDuration,skelAnimSrc.getSkeleton().getPose());
            finalPose = skelAnimDest.computePose(0,skelAnimDest.getSkeleton().getPose());
        }
        else
        {
          //  finalPose = skelAnimSrc.computePose(0,skelAnimSrc.getSkeleton().getPose());
          //  initialPose = skelAnimDest.computePose(skelAnimSrc.getDuration()-mDuration,skelAnimDest.getSkeleton().getPose());
            initialPose = skelAnimSrc.computePose(0,skelAnimSrc.getSkeleton().getPose());
            finalPose = skelAnimDest.computePose(skelAnimSrc.getDuration()-mDuration,skelAnimDest.getSkeleton().getPose());
        }



        for (int i = 0; i < mSkeleton.getNumBones(); i++)
        {
            poseInterpolate(i);
        }

        mat = new Matrix4f();

    }

    public void poseInterpolate(int index)
    {
        setPosePositions(index, initialPose, finalPose);
        setPoseRotations(index, initialPose, finalPose);
        setPoseScale(index, initialPose, finalPose);
    }


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

    public void getFinalPose(float timer)
    {
        SXRPose firstPose = null;
        SXRPose secondPose = null;

        if(!mReverse)
        {
             firstPose = skelAnimSrc.computePose(skelAnimSrc.getDuration()-mDuration+timer+frameTime,skelAnimSrc.getSkeleton().getPose());
             secondPose = skelAnimDest.computePose(0+timer,skelAnimDest.getSkeleton().getPose());
        }
        else
        {
          //  secondPose = skelAnimSrc.computePose(0+timer,skelAnimSrc.getSkeleton().getPose());
          //  firstPose = skelAnimDest.computePose(skelAnimSrc.getDuration()-mDuration+timer+frameTime,skelAnimDest.getSkeleton().getPose());
             firstPose = skelAnimSrc.computePose(0+timer,skelAnimSrc.getSkeleton().getPose());
             secondPose = skelAnimDest.computePose(skelAnimSrc.getDuration()-mDuration+timer+frameTime,skelAnimDest.getSkeleton().getPose());
        }

        float mul = 1/mDuration;


        for(int k =0; k<skelAnimSrc.getSkeleton().getNumBones();k++)
        {
            Vector3f poss = new Vector3f(0,0,0);
            firstPose.getLocalPosition(k,poss);
            Vector3f possT = new Vector3f(0,0,0);
            secondPose.getLocalPosition(k,possT);

            posBlend[k][3] = ((mDuration-timer)*mul*poss.x)+(possT.x*(timer*mul));
            posBlend[k][4] = ((mDuration-timer)*mul*poss.y)+(possT.y*timer*mul);
            posBlend[k][5] = ((mDuration-timer)*mul*poss.z)+(possT.z*timer*mul);

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
    protected void animate(SXRHybridObject target, float ratio)
    {
        animate(mDuration * ratio);
    }


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

            mPosInterpolator.animate(timer,posIData);
            mRotInterpolator.animate(timer,rotIData);
            mSclInterpolator.animate(timer,sclIData);

            mat.translationRotateScale(posIData[0], posIData[1], posIData[2],rotIData[0], rotIData[1], rotIData[2], rotIData[3],sclIData[0], sclIData[1], sclIData[2]);
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