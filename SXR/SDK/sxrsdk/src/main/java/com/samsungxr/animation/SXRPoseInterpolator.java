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

public class SXRPoseInterpolator extends SXRAnimation {
    private SXRPose initialPose;
    private SXRPose finalPose;
    private SXRSkeleton pSkeleton;
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

    private float pDuration;
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
    private SXRSkeletonAnimation skelAnimOne;
    private SXRSkeletonAnimation skelAnimTwo;
    private SXRSkeleton dskeleton;
    private SXRPose combinePose;
    private float[][] posBlend;
    private float[][] rotBlend;
    private float[][] sclBlend;
    private int check = 0;
    static float frameTime = 0;

    public SXRPoseInterpolator(SXRNode target, float duration, SXRPose poseOne, SXRPose poseTwo, SXRSkeleton skeleton) {
        super(target, duration);

        modelTarget = target;

        initialPose = poseOne;
        finalPose = poseTwo;
        pSkeleton = skeleton;

        poseOnePos = new Vector3f(0, 0, 0);
        poseTwoPos = new Vector3f(0, 0, 0);
        poseOneScl = new Vector3f(0, 0, 0);
        poseTwoScl = new Vector3f(0, 0, 0);
        poseOneRot = new Quaternionf(0, 0, 0, 1);
        poseTwoRot = new Quaternionf(0, 0, 0, 1);

        rotData = new float[10];
        posData = new float[8];
        sclData = new float[8];

        posIData = new float[3];
        sclIData = new float[3];
        rotIData = new float[4];

        tempVec = new Vector3f(0, 0, 0);
        tempQuat = new Quaternionf(0, 0, 0, 1);
        initialPosIndex = 0; //initial position index
        finalPosIndex = 10; //final position index
        initialRotIndex = 3; //initial rotation index
        finalRotIndex = 13;  //final rotation index
        initialSclIndex = 7;
        finalSclIndex = 17;
        startTime = 0;
        endTime = duration;
        startTimeIndex = 0;
        endTimeIndex = 4;
        offset = 0;
        poseDataSize = 20;
        mBones = new Bone[pSkeleton.getNumBones()];
        poseData = new float[poseDataSize * pSkeleton.getNumBones()];
        pDuration = duration;

        for (int i = 0; i < pSkeleton.getNumBones(); i++) {
            poseInterpolate(i);
        }
        mat = new Matrix4f();
    }


    //dynamic update

    public SXRPoseInterpolator(SXRNode target, float duration, SXRSkeletonAnimation skelOne, SXRSkeletonAnimation skelTwo, SXRSkeleton skeleton) {
        super(target, duration);
        skelAnimOne = skelOne;
        skelAnimTwo = skelTwo;
        dskeleton = skeleton;
        rotData = new float[10];
        posData = new float[8];
        sclData = new float[8];
        posData[0] = 0;
        rotData[0] = 0;
        sclData[0] = 0;
        posIData = new float[3];
        sclIData = new float[3];
        rotIData = new float[4];
        mat = new Matrix4f();
        posBlend = new float[skelAnimOne.getSkeleton().getNumBones()][6];
        rotBlend = new float[skelAnimOne.getSkeleton().getNumBones()][8];
        sclBlend = new float[skelAnimOne.getSkeleton().getNumBones()][6];

        SXRPose firstPose = skelAnimOne.computePose(skelAnimOne.getDuration() - pDuration, skelAnimOne.getSkeleton().getPose());
        SXRPose secondPose = skelAnimTwo.computePose(0, skelAnimTwo.getSkeleton().getPose());

        for (int j = 0; j < firstPose.getNumBones(); j++) {
            Vector3f poss = new Vector3f(0, 0, 0);
            firstPose.getLocalPosition(j, poss);
            Vector3f possT = new Vector3f(0, 0, 0);
            secondPose.getLocalPosition(j, possT);


            //quaternion

            Quaternionf q1 = new Quaternionf(0, 0, 0, 1);
            firstPose.getLocalRotation(j, q1);
            Quaternionf q2 = new Quaternionf(0, 0, 0, 1);
            secondPose.getLocalRotation(j, q2);
            //  combinePose.setLocalRotation(j,q1.x,q1.y,q1.z,q1.w);


            posBlend[j][0] = poss.x;
            posBlend[j][1] = poss.y;
            posBlend[j][2] = poss.z;
            posBlend[j][3] = possT.x;
            posBlend[j][4] = possT.y;
            posBlend[j][5] = possT.z;

            rotBlend[j][0] = q1.x;
            rotBlend[j][1] = q1.y;
            rotBlend[j][2] = q1.z;
            rotBlend[j][3] = q1.w;
            rotBlend[j][4] = q2.x;
            rotBlend[j][5] = q2.y;
            rotBlend[j][6] = q2.z;
            rotBlend[j][7] = q2.w;


            Vector3f scl = new Vector3f(0, 0, 0);
            firstPose.getLocalScale(j, scl);
            Vector3f sclT = new Vector3f(0, 0, 0);
            secondPose.getLocalScale(j, sclT);

            sclBlend[j][0] = scl.x;
            sclBlend[j][1] = scl.y;
            sclBlend[j][2] = scl.z;
            sclBlend[j][3] = sclT.x;
            sclBlend[j][4] = sclT.y;
            sclBlend[j][5] = sclT.z;


        }
        pDuration = duration;


    }

    public void poseInterpolate(int index) {
        setPosePositions(index);
        setPoseRotations(index);
        setPoseScale(index);
    }


    private void setPosePositions(int index) {
        initialPose.getLocalPosition(index, poseOnePos);
        finalPose.getLocalPosition(index, poseTwoPos);

        offset = index * poseDataSize;

        setPosePositions(offset + initialPosIndex, poseOnePos);
        setPosePositions(offset + finalPosIndex, poseTwoPos);
    }

    public void setPosePositions(int posOffset, Vector3f posePos) {
        poseData[posOffset] = posePos.x();
        poseData[posOffset + 1] = posePos.y();
        poseData[posOffset + 2] = posePos.z();
    }

    private void setPoseRotations(int index) {
        initialPose.getLocalRotation(index, poseOneRot);
        finalPose.getLocalRotation(index, poseTwoRot);
        offset = index * poseDataSize;
        setPoseRotations(offset + initialRotIndex, poseOneRot);
        setPoseRotations(offset + finalRotIndex, poseTwoRot);
    }

    public void setPoseRotations(int rotOffset, Quaternionf poseRot) {
        poseData[rotOffset] = poseRot.x();
        poseData[rotOffset + 1] = poseRot.y();
        poseData[rotOffset + 2] = poseRot.z();
        poseData[rotOffset + 3] = poseRot.w();
    }

    private void setPoseScale(int index) {
        initialPose.getLocalScale(index, poseOneScl);
        finalPose.getLocalScale(index, poseTwoScl);
        offset = index * poseDataSize;
        setPoseScale(offset + initialSclIndex, poseOneScl);
        setPoseScale(offset + finalSclIndex, poseTwoScl);
    }


    public void setPoseScale(int sclOffset, Vector3f poseScl) {
        poseData[sclOffset] = poseScl.x();
        poseData[sclOffset + 1] = poseScl.y();
        poseData[sclOffset + 2] = poseScl.z();
    }

    public void updatePos(int offset) {
        posData[startTimeIndex] = startTime;
        posData[endTimeIndex] = endTime;
        updatePos(1, offset + initialPosIndex);
        updatePos(5, offset + finalPosIndex);
    }

    public void updatePos(int pos, int posOffset) {
        posData[pos] = poseData[posOffset];
        posData[pos + 1] = poseData[posOffset + 1];
        posData[pos + 2] = poseData[posOffset + 2];
    }

    public void updateRot(int offset) {
        rotData[startTimeIndex] = startTime;
        rotData[endTimeIndex + 1] = endTime;
        updateRot(1, offset + initialRotIndex);
        updateRot(6, offset + finalRotIndex);
    }

    public void updateRot(int rot, int rotOffset) {
        rotData[rot] = poseData[rotOffset];
        rotData[rot + 1] = poseData[rotOffset + 1];
        rotData[rot + 2] = poseData[rotOffset + 2];
        rotData[rot + 3] = poseData[rotOffset + 3];
    }

    public void updateScl(int offset) {
        sclData[startTimeIndex] = startTime;
        sclData[endTimeIndex] = endTime;
        updateScl(1, offset + initialSclIndex);
        updateScl(5, offset + finalSclIndex);
    }

    public void updateScl(int scl, int sclOffset) {
        sclData[scl] = poseData[sclOffset];
        sclData[scl + 1] = poseData[sclOffset + 1];
        sclData[scl + 2] = poseData[sclOffset + 2];
    }

    public void getSecondPose(float timer) {
        SXRPose firstPose = skelAnimOne.computePose(skelAnimOne.getDuration() - pDuration + timer + frameTime, skelAnimOne.getSkeleton().getPose());
        SXRPose secondPose = skelAnimTwo.computePose(0 + timer, skelAnimTwo.getSkeleton().getPose());

        float mul = 1 / pDuration;


        for (int j = 0; j < skelAnimOne.getSkeleton().getNumBones(); j++) {
            Vector3f poss = new Vector3f(0, 0, 0);
            firstPose.getLocalPosition(j, poss);
            Vector3f possT = new Vector3f(0, 0, 0);
            secondPose.getLocalPosition(j, possT);

            posBlend[j][3] = ((pDuration - timer) * mul * poss.x) + (possT.x * (timer * mul));
            posBlend[j][4] = ((pDuration - timer) * mul * poss.y) + (possT.y * timer * mul);
            posBlend[j][5] = ((pDuration - timer) * mul * poss.z) + (possT.z * timer * mul);

            Quaternionf q1 = new Quaternionf(0, 0, 0, 1);
            firstPose.getLocalRotation(j, q1);
            Quaternionf q2 = new Quaternionf(0, 0, 0, 1);
            secondPose.getLocalRotation(j, q2);

            rotBlend[j][4] = ((pDuration - timer) * mul * q1.x) + (q2.x * timer * mul);
            rotBlend[j][5] = ((pDuration - timer) * mul * q1.y) + (q2.y * timer * mul);
            rotBlend[j][6] = ((pDuration - timer) * mul * q1.z) + (q2.z * timer * mul);
            rotBlend[j][7] = ((pDuration - timer) * mul * q1.w) + (q2.w * timer * mul);


            Vector3f scl = new Vector3f(0, 0, 0);
            firstPose.getLocalScale(j, scl);
            Vector3f sclT = new Vector3f(0, 0, 0);
            secondPose.getLocalScale(j, sclT);

            sclBlend[j][3] = ((pDuration - timer) * mul * scl.x) + (sclT.x * timer * mul);
            sclBlend[j][4] = ((pDuration - timer) * mul * scl.y) + (sclT.y * timer * mul);
            sclBlend[j][5] = ((pDuration - timer) * mul * scl.z) + (sclT.z * timer * mul);


        }
    }

    protected void animate(SXRHybridObject target, float ratio) {
        animate(pDuration * ratio);
    }

    public void animate(float timer) {

        initialPose = dskeleton.getPose();

        Matrix4f temp = new Matrix4f();
        SXRSkeleton skel = dskeleton;

        posData[4] = timer;
        sclData[4] = timer;
        rotData[5] = timer;

        getSecondPose(timer);

        for (int i = 0; i < dskeleton.getNumBones(); ++i) {
            posData[1] = posBlend[i][0];
            posData[2] = posBlend[i][1];
            posData[3] = posBlend[i][2];
            posData[5] = posBlend[i][3];
            posData[6] = posBlend[i][4];
            posData[7] = posBlend[i][5];


            rotData[1] = rotBlend[i][0];
            rotData[2] = rotBlend[i][1];
            rotData[3] = rotBlend[i][2];
            rotData[4] = rotBlend[i][3];
            rotData[6] = rotBlend[i][4];
            rotData[7] = rotBlend[i][5];
            rotData[8] = rotBlend[i][6];
            rotData[9] = rotBlend[i][7];

            sclData[1] = sclBlend[i][0];
            sclData[2] = sclBlend[i][1];
            sclData[3] = sclBlend[i][2];
            sclData[5] = sclBlend[i][3];
            sclData[6] = sclBlend[i][4];
            sclData[7] = sclBlend[i][5];

            mPosInterpolator = new SXRFloatAnimation(posData, 4);
            mRotInterpolator = new SXRQuatAnimation(rotData);
            mSclInterpolator = new SXRFloatAnimation(sclData, 4);
            mPosInterpolator.animate(timer, posIData);
            mRotInterpolator.animate(timer, rotIData);
            mSclInterpolator.animate(timer, sclIData);
            mat.translationRotateScale(posIData[0], posIData[1], posIData[2], rotIData[0], rotIData[1], rotIData[2], rotIData[3], sclIData[0], sclIData[1], sclIData[2]);
            initialPose.setLocalMatrix(i, mat);

            posBlend[i][0] = posIData[0];
            posBlend[i][1] = posIData[1];
            posBlend[i][2] = posIData[2];

            rotBlend[i][0] = rotIData[0];
            rotBlend[i][1] = rotIData[1];
            rotBlend[i][2] = rotIData[2];
            rotBlend[i][3] = rotIData[3];

            sclBlend[i][0] = sclIData[0];
            sclBlend[i][1] = sclIData[1];
            sclBlend[i][2] = sclIData[2];

        }

        dskeleton.poseToBones();
        dskeleton.updateBonePose();
        dskeleton.updateSkinPose();
        posData[0] = timer;
        rotData[0] = timer;
        sclData[0] = timer;
        check++;


    }
}