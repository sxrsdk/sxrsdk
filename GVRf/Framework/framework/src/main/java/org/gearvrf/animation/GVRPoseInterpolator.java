

package org.gearvrf.animation;

import org.gearvrf.GVRHybridObject;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.animation.keyframe.GVRFloatAnimation;
import org.gearvrf.animation.keyframe.GVRQuatAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static org.gearvrf.animation.GVRPose.Bone;



public class GVRPoseInterpolator extends GVRAnimation
{
    private GVRPose initialPose;
    private GVRPose finalPose;
    private GVRSkeleton pSkeleton;
    private Bone[] mBones;
    private Vector3f poseOnePos =  new Vector3f(0,0,0);
    private Vector3f poseTwoPos =  new Vector3f(0,0,0);
    private Vector3f poseOneScl =  new Vector3f(0,0,0);
    private Vector3f poseTwoScl =  new Vector3f(0,0,0);
    private Quaternionf poseOneRot =  new Quaternionf(0,0,0,1);
    private Quaternionf poseTwoRot =  new Quaternionf(0,0,0,1);

    private GVRQuatAnimation mRotInterpolator;
    private GVRFloatAnimation mPosInterpolator;
    private GVRFloatAnimation mSclInterpolator;
    private float pDuration;
    private float[] rotData = new float[10];
    private float[] posData = new float[8];
    private float[] sclData = new float[8];
    private int offset = 0;
    private float[] poseData;
    private float posValue[] = new float[3];
    private float sclValue[] = new float[3];
    private float rotValue[] = new float[4];



    public GVRPoseInterpolator(GVRSceneObject target, float duration, GVRPose poseOne, GVRPose poseTwo, GVRSkeleton skel)
    {
        super(target, duration);

        initialPose = poseOne;
        finalPose = poseTwo;
        pSkeleton = skel;

        mBones = new Bone[pSkeleton.getNumBones()];
        poseData = new float[22*pSkeleton.getNumBones()];
        pDuration = duration;
        for (int i = 0; i < pSkeleton.getNumBones(); i++) {

            poseInterpolate(i);

        }
        mRotInterpolator = new GVRQuatAnimation(rotData);
        mPosInterpolator = new GVRFloatAnimation(posData, 4);
        mSclInterpolator = new GVRFloatAnimation(sclData, 4);

    }



    public void poseInterpolate(int index)
    {

        setPosePositions(index);
        setPoseRotations(index);
        setPoseScale(index);

    }


    public void setPosePositions(int index) {

        initialPose.getLocalPosition(index,poseOnePos);
        finalPose.getLocalPosition(index,poseTwoPos);
        offset = index * 22;
        poseData[offset] = 0;
        poseData[offset+1] = poseOnePos.x();
        poseData[offset+2] = poseOnePos.y();
        poseData[offset+3] = poseOnePos.z();
        poseData[offset+11] = pDuration;
        poseData[offset+12] = poseTwoPos.x();
        poseData[offset+13] = poseTwoPos.y();
        poseData[offset+14] = poseTwoPos.z();


    }

    public void setPoseRotations(int index) {

        initialPose.getLocalRotation(index,poseOneRot);
        finalPose.getLocalRotation(index,poseTwoRot);

        offset = index * 22;

        poseData[offset+4] = poseOneRot.x();
        poseData[offset+5] = poseOneRot.y();
        poseData[offset+6] = poseOneRot.z();
        poseData[offset+7] = poseOneRot.w();

        poseData[offset+15] = poseTwoRot.x();
        poseData[offset+16] = poseTwoRot.y();
        poseData[offset+17] = poseTwoRot.z();
        poseData[offset+18] = poseTwoRot.w();


    }




    public void setPoseScale(int index) {

        initialPose.getLocalScale(index,poseOneScl);
        finalPose.getLocalScale(index,poseTwoScl);

        offset = index * 22;

        poseData[offset+8] = poseOneScl.x();
        poseData[offset+9] = poseOneScl.y();
        poseData[offset+10] = poseOneScl.z();

        poseData[offset+19] = poseTwoScl.x();
        poseData[offset+20] = poseTwoScl.y();
        poseData[offset+21] = poseTwoScl.z();


    }

    protected void animate(GVRHybridObject target, float ratio)
    {

        animate(pDuration * ratio);
    }


    public void animate(float timer)
    {
        initialPose = pSkeleton.getPose();



        for(int i=0;i<pSkeleton.getNumBones();i++)
        {
            offset = i*22;

            posData[0] = poseData[offset];
            posData[1] = poseData[offset+1];
            posData[2] = poseData[offset+2];
            posData[3] = poseData[offset+3];
            posData[4] = poseData[offset+11];
            posData[5] = poseData[offset+12];
            posData[6] = poseData[offset+13];
            posData[7] = poseData[offset+14];

            sclData[0] = poseData[offset];
            sclData[1] = poseData[offset+8];
            sclData[2] = poseData[offset+9];
            sclData[3] = poseData[offset+10];
            sclData[4] = poseData[offset+11];
            sclData[5] = poseData[offset+19];
            sclData[6] = poseData[offset+20];
            sclData[7] = poseData[offset+21];

            rotData[0] = poseData[offset];
            rotData[1] = poseData[offset+4];
            rotData[2] = poseData[offset+5];
            rotData[3] = poseData[offset+6];
            rotData[4] = poseData[offset+7];
            rotData[5] = poseData[offset+11];
            rotData[6] = poseData[offset+15];
            rotData[7] = poseData[offset+16];
            rotData[8] = poseData[offset+17];
            rotData[9] = poseData[offset+18];

            Matrix4f mat = new Matrix4f();

            mPosInterpolator.animate(timer,posValue);
            mSclInterpolator.animate(timer,sclValue);


            mRotInterpolator.animate(timer, rotValue);

            mat.translationRotateScale(posValue[0], posValue[1], posValue[2],rotValue[0], rotValue[1], rotValue[2], rotValue[3],sclValue[0], sclValue[1], sclValue[2]);
            initialPose.setLocalMatrix(i, mat);

            poseData[offset] =  timer;
            poseData[offset+1] = posValue[0];
            poseData[offset+2] = posValue[1];
            poseData[offset+3] = posValue[2];


            poseData[offset+4] = rotValue[0];
            poseData[offset+5] = rotValue[1];
            poseData[offset+6] = rotValue[2];
            poseData[offset+7] = rotValue[3];

            poseData[offset+8] = sclValue[0];
            poseData[offset+9] = sclValue[1];
            poseData[offset+10]= sclValue[2];


            }


        pSkeleton.poseToBones();

        pSkeleton.updateSkinPose();


    }


}