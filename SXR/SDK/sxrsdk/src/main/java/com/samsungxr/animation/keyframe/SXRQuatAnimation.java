package com.samsungxr.animation.keyframe;

import com.samsungxr.PrettyPrint;
import com.samsungxr.utility.Log;
import org.joml.Quaternionf;

/**
 * Describes the animation of a set of quaternions.
 * <p>
 * This type of animation is used for rotations.
 */
public final class SXRQuatAnimation extends SXRFloatAnimation
{
    private static final String TAG = SXRQuatAnimation.class.getSimpleName();

    public static class SphericalInterpolator extends LinearInterpolator
    {
        private Quaternionf mTempQuatA = new Quaternionf();
        private Quaternionf mTempQuatB = new Quaternionf();

        public SphericalInterpolator(float[] keyData, int keySize)
        {
            super(keyData, keySize);
        }

        public float[] getKeyData() { return mKeyData; }

        public boolean interpolateValues(int keyIndex, float[] values, float factor)
        {
            int firstOfs = getKeyOffset(keyIndex);
            int lastOfs = getKeyOffset(keyIndex + 1);

            if ((firstOfs < 0) || (lastOfs < 0))
            {
                return false;
            }
            ++firstOfs;
            ++lastOfs;
            mTempQuatA.x = mKeyData[firstOfs + 0];
            mTempQuatA.y = mKeyData[firstOfs + 1];
            mTempQuatA.z = mKeyData[firstOfs + 2];
            mTempQuatA.w = mKeyData[firstOfs + 3];
            mTempQuatB.x = mKeyData[lastOfs + 0];
            mTempQuatB.y = mKeyData[lastOfs + 1];
            mTempQuatB.z = mKeyData[lastOfs + 2];
            mTempQuatB.w = mKeyData[lastOfs + 3];
            mTempQuatA.slerp(mTempQuatB, factor, mTempQuatA);
            values[0] = mTempQuatA.x;
            values[1] = mTempQuatA.y;
            values[2] = mTempQuatA.z;
            values[3] = mTempQuatA.w;
            return true;
        }
    };

    /**
     * Constructs a quaternion animation from the supplied data.
     *
     * @param keyData animation key data, must be x,y,z,w (Quaterions)
     */
    public SXRQuatAnimation(float[] keyData)
    {
        super(keyData, 5);
        mFloatInterpolator =  new SphericalInterpolator(mKeys, 5);
    }

    /**
     * Constructs a quaternion animation with room for the
     * given number of keys.
     *
     * @param numKeys expected number of animation keys
     */
    public SXRQuatAnimation(int numKeys)
    {
        super(numKeys, 5);
        mFloatInterpolator =  new SphericalInterpolator(mKeys, 5);
    }

    /**
     * Clone a given quaternion animation.
     * This is a shallow copy and both animations share
     * the same data.
     *
     * @param src animation to clone
     */
    public SXRQuatAnimation(final SXRQuatAnimation src)
    {
        super(src.getNumKeys(), src.mFloatsPerKey);
        mKeys = src.mKeys;
        mFloatInterpolator = src.mFloatInterpolator;
    }

    /**
     * Returns the scaling factor as vector.
     *
     * @param keyIndex the index of the scale key
     *
     * @return the scaling factor as vector
     */
    public void getKey(int keyIndex, Quaternionf q)
    {
        int index = keyIndex * mFloatsPerKey;
        q.x = mKeys[index + 1];
        q.y = mKeys[index + 2];
        q.z = mKeys[index + 3];
        q.w = mKeys[index + 4];
    }

    /**
     * Set the value of a quaternion key.
     * @param keyIndex  0-based index of key to set
     * @param time      time (in seconds) for this key
     * @param q         quaternion value for this key
     * @see SXRFloatAnimation#setKey(int, float, float[])
     */
    public void setKey(int keyIndex, float time, final Quaternionf q)
    {
        int index = keyIndex * mFloatsPerKey;

        mKeys[index] = time;
        mKeys[index + 1] = q.x;
        mKeys[index + 2] = q.y;
        mKeys[index + 3] = q.z;
        mKeys[index + 4] = q.w;
    }


}

