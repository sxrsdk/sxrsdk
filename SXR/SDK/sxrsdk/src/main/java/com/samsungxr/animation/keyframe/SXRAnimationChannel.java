package com.samsungxr.animation.keyframe;

import com.samsungxr.PrettyPrint;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Describes the animation of a single node's transform.
 *
 * The node name ({@link #getNodeName()} specifies the bone / node which is
 * affected by this animation channel. The keyframes are given in three
 * separate series of values, one each for position, rotation and scale.
 * The transformation matrix is computed from these values and is used
 * to update a node or a bone in a skeleton.
 * <p>>
 * This means all keys are absolute and not relative to the bone default pose.
 * The order in which the transformations are applied is - as usual -
 * scaling, rotation, translation.
 * @see SXRSkeletonAnimation
 * @see com.samsungxr.animation.SXRSkeleton
 */
public class SXRAnimationChannel implements PrettyPrint
{
    private static final String TAG = SXRAnimationChannel.class.getSimpleName();
    private static final float[] mTempVec = new float[3];

    /**
     * Construct an animation channel with room for the specified keys.
     *
     * @param nodeName     name of corresponding scene graph node
     * @param numPosKeys   number of expected position keys,
     *                     if 0 no position keys are used.
     * @param numRotKeys   number of expected rotation keys,
     *                     if 0 no rotation keys are used.
     * @param numScaleKeys number of expected scaling keys,
     *                     if 0, no scale keys are used.
     */
    public SXRAnimationChannel(String nodeName, int numPosKeys, int numRotKeys, int numScaleKeys)
    {
        m_nodeName = nodeName;
        mPosInterpolator = new SXRFloatAnimation(numPosKeys, 4);
        mRotInterpolator = new SXRQuatAnimation(numRotKeys);
        mSclInterpolator = new SXRFloatAnimation(numScaleKeys, 4);
    }

    /**
     * Construct an animation channel with the supplied key data.
     *
     * @param nodeName      name of corresponding scene graph node
     * @param posKeys       array of position keys, 3 floats per key.
     *                      if null no position keys are used.
     * @param rotKeys       array of rotation keys, 4 floats per key.
     *                      if null no rotation keys are used.
     * @param scaleKeys     array of scale keys, 3 floats per key.
     *                      if null, no scale keys are used.
     */
    public SXRAnimationChannel(String nodeName, float[] posKeys, float[] rotKeys, float[] scaleKeys)
    {
        m_nodeName = nodeName;
        if (posKeys != null)
        {
            mPosInterpolator = new SXRFloatAnimation(posKeys, 4);
        }
        else
        {
            mPosInterpolator = new SXRFloatAnimation(0, 4);
        }
        if (rotKeys != null)
        {
            mRotInterpolator = new SXRQuatAnimation(rotKeys);
        }
        else
        {
            mRotInterpolator = new SXRQuatAnimation(0);
        }
        if (scaleKeys != null)
        {
            mSclInterpolator = new SXRFloatAnimation(scaleKeys, 4);
        }
        else
        {
            mSclInterpolator = new SXRFloatAnimation(0, 4);
        }
    }

    /**
     * Construct an animation channel which shares data with another.
     */
    public SXRAnimationChannel(final SXRAnimationChannel src)
    {
        m_nodeName = src.m_nodeName;
        mPosInterpolator = src.mPosInterpolator;
        mRotInterpolator = src.mRotInterpolator;
        mSclInterpolator =src.mSclInterpolator;
    }

    /**
     * Returns the name of the scene graph node affected by this animation.<p>
     *
     * The node must exist and it must be unique.
     *
     * @return the name of the affected node
     */
    public String getNodeName() {
        return m_nodeName;
    }

    /**
     * Resize the position keys.
     * This function will truncate the position keys if the
     * initial setting was too large.
     *
     * @oaran numPosKeys the desired size for the position keys
     */
    public void resizePosKeys(int numPosKeys)
    {
        mPosInterpolator.resizeKeys(numPosKeys);
    }

    /**
     * Returns the number of position keys.
     *
     * @return the number of position keys
     */
    public int getNumPosKeys() {
        return mPosInterpolator.getNumKeys();
    }

    /**
     * Returns the time component of the specified position key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getPosKeyTime(int keyIndex) {
        return mPosInterpolator.getTime(keyIndex);
    }

    /**
     * Returns the position as vector.<p>
     *
     * @param keyIndex the index of the position key
     *
     * @return the position as vector
     */
    public void getPosKeyVector(int keyIndex, float[] pos)
    {
        mPosInterpolator.getKey(keyIndex, pos);
    }

    /**
     * Set the values for a given position key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param pos       array of 3 float values containing new position.
     */
    public void setPosKeyVector(int keyIndex, float time, final float[] pos)
    {
        mPosInterpolator.setKey(keyIndex, time, pos);
    }

    /**
     * Set the values for a given position key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param x         new X position.
     * @param y         new Y position.
     * @param z         new Z position.
     */
    public void setPosKeyVector(int keyIndex, float time, float x, float y, float z)
    {
        mTempVec[0] = x;
        mTempVec[1] = y;
        mTempVec[2] = z;
        mPosInterpolator.setKey(keyIndex, time, mTempVec);
    }

    /**
     * Returns the number of rotation keys.
     *
     * @return the number of rotation keys
     */
    public int getNumRotKeys() {
        return mRotInterpolator.getNumKeys();
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
        mRotInterpolator.resizeKeys(numRotKeys);
    }

    /**
     * Returns the time component of the specified rotation key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getRotKeyTime(int keyIndex) {
        return mRotInterpolator.getTime(keyIndex);
    }

    /**
     * Returns the rotation as quaternion.
     *
     * @param keyIndex the index of the rotation key
     *
     * @return the rotation as quaternion
     */
    public void getRotKeyQuaternion(int keyIndex, float[] rot)
    {
        mRotInterpolator.getKey(keyIndex, rot);
    }

    /**
     * Set the values for a given rotation key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param rot       array of 4 values (x, y, z, w) representing
     *                  the new rotation quaternions.
     */
    public void setRotKeyQuaternion(int keyIndex, float time, float[] rot)
    {
        mRotInterpolator.setKey(keyIndex, time, rot);
    }

    /**
     * Set the values for a given rotation key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param rot       new rotation quaternion.
     */
    public void setRotKeyQuaternion(int keyIndex, float time, Quaternionf rot)
    {
        mRotInterpolator.setKey(keyIndex, time, rot);
    }

    /**
     * Returns the number of scaling keys.
     *
     * @return the number of scaling keys
     */
    public int getNumScaleKeys() {
        return mSclInterpolator.getNumKeys();
    }

    /**
     * Resize the scale keys.
     * This function will truncate the scale keys if the
     * initial setting was too large.
     *
     * @oaran numScaleKeys the desired size for the position keys
     */
    public void resizeScaleKeys(int numScaleKeys)
    {
        mSclInterpolator.resizeKeys(numScaleKeys);
    }

    /**
     * Returns the time component of the specified scaling key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public double getScaleKeyTime(int keyIndex) {
        return mRotInterpolator.getTime(keyIndex);
    }

    /**
     * Returns the scaling factor as vector.<p>
     *
     * @param keyIndex the index of the scale key
     *
     * @return the scaling factor as vector
     */
    public void getScaleKeyVector(int keyIndex, float[] scale)
    {
        mSclInterpolator.getKey(keyIndex, scale);
    }

    /**
     * Set the values for a given scale key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param scale     array of 3 float values containing new scale factors.
     */
    public void setScaleKeyVector(int keyIndex, float time, final float[] scale)
    {
        mSclInterpolator.setKey(keyIndex, time, scale);
    }

    /**
     * Set the values for a given scale keys.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key.
     * @param x         new X scale.
     * @param y         new Y scale.
     * @param z         new Z scale.
     */
    public void setScaleKeyVector(int keyIndex, float time, float x, float y, float z)
    {
        mTempVec[0] = x;
        mTempVec[1] = y;
        mTempVec[2] = z;
        mSclInterpolator.setKey(keyIndex, time, mTempVec);
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
        mRotInterpolator.animate(animationTime, mRotKey);
        mPosInterpolator.animate(animationTime, mPosKey);
        mSclInterpolator.animate(animationTime, mScaleKey);
        mat.translationRotateScale(mPosKey[0], mPosKey[1], mPosKey[2], mRotKey[0], mRotKey[1], mRotKey[2], mRotKey[3], mScaleKey[0], mScaleKey[1], mScaleKey[2]);
    }

    /*
     * Scale the position keys and the scale keys for this animation channel.
     * @param scaleFactor   amount to scale the keys
    */
    public void scaleKeys(float scaleFactor)
    {
        float[] temp = new float[3];
        for (int i = 0; i < getNumPosKeys(); ++i)
        {
            float time = (float) getPosKeyTime(i);
            getPosKeyVector(i, temp);
            temp[0] *= scaleFactor;
            temp[1] *= scaleFactor;
            temp[2] *= scaleFactor;
            setPosKeyVector(i, time, temp);
        }
        for (int i = 0; i < getNumScaleKeys(); ++i)
        {
            float time = (float) getScaleKeyTime(i);
            getScaleKeyVector(i, temp);
            temp[0] *= scaleFactor;
            temp[1] *= scaleFactor;
            temp[2] *= scaleFactor;
            setScaleKeyVector(i, time, temp);
        }
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(SXRAnimationChannel.class.getSimpleName());
        sb.append(" [nodeName=" + m_nodeName + ", positionKeys="
                + getNumPosKeys() + ", rotationKeys="
                + getNumRotKeys() + ", scaleKeys="
                + getNumScaleKeys() + "]");
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

    /**
     * Node name.
     */
    protected final String m_nodeName;
    final protected float[] mPosKey = new float[] {  0, 0, 0 };
    final protected float[] mScaleKey = new float[] { 1, 1, 1 };
    final protected float[] mRotKey = new float[] { 0, 0, 0, 1 };
    final protected SXRFloatAnimation mPosInterpolator;
    final protected SXRQuatAnimation mRotInterpolator;
    final protected SXRFloatAnimation mSclInterpolator;
}