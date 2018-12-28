package com.samsungxr.animation.keyframe;

import com.samsungxr.PrettyPrint;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Describes the animation of a single node.<p>
 *
 * The node name ({@link #getNodeName()} specifies the bone/node which is
 * affected by this animation channel. The keyframes are given in three
 * separate series of values, one each for position, rotation and scaling.
 * The transformation matrix computed from these values replaces the node's
 * original transformation matrix at a specific time.<p>
 *
 * This means all keys are absolute and not relative to the bone default pose.
 * The order in which the transformations are applied is - as usual -
 * scaling, rotation, translation.
 */
public final class SXRBVHAnimationChannel extends SXRAnimationChannel
{
    private static final String TAG = SXRBVHAnimationChannel.class.getSimpleName();
    private static final float[] mTempVec = new float[3];
    final private SXRFloatAnimation mEulerInterpolator;
    final protected float[] mEulerKey = new float[] { 0, 0, 0 };

    /**
     * Constructor.
     *
     * @param nodeName name of corresponding scene graph node
     * @param numPosKeys number of position keys
     * @param numRotKeys number of rotation keys
     * @param preBehavior behavior before animation start
     * @param postBehavior behavior after animation end
     */
    public SXRBVHAnimationChannel(String nodeName, int numPosKeys, int numRotKeys,  SXRAnimationBehavior preBehavior, SXRAnimationBehavior postBehavior)
    {
        super(nodeName, numPosKeys, numRotKeys, 0, preBehavior, postBehavior);
        mEulerInterpolator = new SXRFloatAnimation(numRotKeys, 4);
    }

    public SXRBVHAnimationChannel(String nodeName, float[] posKeys, float[] rotKeys,
                                  SXRAnimationBehavior preBehavior,
                                  SXRAnimationBehavior postBehavior)
    {
        super(nodeName, posKeys, null, null, preBehavior, postBehavior);
        if (rotKeys != null)
        {
            mEulerInterpolator = new SXRFloatAnimation(rotKeys,4);
        }
        else
        {
            mEulerInterpolator = new SXRFloatAnimation(0, 4);
        }
    }

    /**
     * Returns the number of rotation keys.
     *
     * @return the number of rotation keys
     */
    public int getNumRotKeys() {
        return mEulerInterpolator.getNumKeys();
    }

    /**
     * Resize the rotation keys.
     * This function will truncate the rotation keys if the
     * initial setting was too large.
     *
     * @oaran numRotKeys the desired size for the rotation keys
     */
    public void resizeRotKeys(int numRotKeys)
    {
        mEulerInterpolator.resizeKeys(numRotKeys);
    }

    /**
     * Returns the time component of the specified rotation key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getRotKeyTime(int keyIndex) {
        return mEulerInterpolator.getTime(keyIndex);
    }

    /**
     * Returns the rotation as quaternion.<p>
     *
     *
     * @param keyIndex the index of the rotation key
     *
     * @return the rotation as quaternion
     */
    public void getRotKeyEuler(int keyIndex, float[] rot)
    {
        mEulerInterpolator.getKey(keyIndex, rot);
    }

    public void setRotKeyEuler(int keyIndex, float time, float[] rot)
    {
        mEulerInterpolator.setKey(keyIndex, time, rot);
    }

    public void getRotKeyEuler(float time, float[] rot)
    {
        SXRFloatAnimation.LinearInterpolator interp = mEulerInterpolator.getInterpolator();
        int keyIndex = interp.getKeyIndex(time);
        if (keyIndex >= 0)
        {
            interp.getValues(keyIndex, rot);
        }
    }

    public void getPosKey(float time, float[] pos)
    {
        SXRFloatAnimation.LinearInterpolator interp = mPosInterpolator.getInterpolator();
        int keyIndex = interp.getKeyIndex(time);
        if (keyIndex >= 0)
        {
            interp.getValues(keyIndex, pos);
        }
    }

    /**
     * Obtains the transform for a specific time in animation.
     *
     * @param animationTime The time in animation.
     *
     * @return The transform.
     */
    public void animate(float animationTime, Matrix4f mat)
    {
        mEulerInterpolator.animate(animationTime, mEulerKey);
        mPosInterpolator.animate(animationTime, mPosKey);
        mSclInterpolator.animate(animationTime, mScaleKey);
        mat.rotationXYZ(mEulerKey[0], mEulerKey[1], mEulerKey[2]);
        mat.setTranslation(mPosKey[0], mPosKey[1], mPosKey[2]);
    }

}