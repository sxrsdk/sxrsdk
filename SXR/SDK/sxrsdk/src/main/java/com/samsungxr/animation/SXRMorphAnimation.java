
package com.samsungxr.animation;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRMeshMorph;
import com.samsungxr.PrettyPrint;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.keyframe.SXRFloatAnimation;
import com.samsungxr.utility.Log;

/** 
 * Animates a set of blend shape weights over time.
 * <p>
 * The <i>target</i> of a morph animation is a {@link SXRMeshMorph}
 * component controlling a set of blend shapes used to morph a mesh
 * dynamically. Each blend shape has a weight associated with it
 * indicating what proportion of that blend shape is used to
 * morph the base mesh. This class provides an animation channel
 * for the blend weights of a mesh morph.
 * <p>
 * Each key frame for a morph animation has a time and N floating
 * point values for the N blend weights. Each frame the blend
 * weights given to the {@link SXRMeshMorph} are calculated
 * by interpolating between the key frames.
 * @see SXRMeshMorph
 * @see SXRAnimation
 */
public final class SXRMorphAnimation extends SXRAnimation implements PrettyPrint
{
    private static final String TAG = SXRMorphAnimation.class.getSimpleName();
    protected float[] mKeys;
    protected SXRFloatAnimation mKeyInterpolator;
    protected float[] mCurrentValues;

    /**
     * Creates a morph animation for the given mesh morph.
     * <p>
     * The key frames are all stored in a single contiguous
     * floating point array. Each key frame has a time followed
     * by N blend weights (where N is the key size - 1).
     * @param target  {@link SXRMeshMorph} component being animated.
     * @param keyData blend weight key data.
     * @param keySize number of floats per key.
     */
    public SXRMorphAnimation(SXRMeshMorph target, float[] keyData, int keySize)
    {
        super(target, keyData[keyData.length - keySize] - keyData[0]);
        mKeys = keyData;
        mKeyInterpolator = new SXRFloatAnimation(keyData, keySize);
        mCurrentValues = new float[keySize - 1];

        SXRNode meshOwner = target.getOwnerObject();

        if ((mName == null) && (meshOwner != null))
        {
            String name = meshOwner.getName();
            if (name != null)
            {
                setName(name + ".morph");
            }
        }
    }

    /**
     * Creates a morph animation for a node with a given name.
     * <p>
     * This constructor is used when to create a morph
     * animation that can be used later. The name identifies
     * which node it should animate.
     * <p>
     * The key frames are all stored in a single contiguous
     * floating point array. Each key frame has a time followed
     * by N blend weights (where N is the key size - 1).
     * @param name    name of morph animation.
     * @param keyData blend weight key data.
     * @param keySize number of floats per key.
     */
    public SXRMorphAnimation(String name, float[] keyData, int keySize)
    {
        super(null, keyData[keyData.length - keySize] - keyData[0]);
        mName = name;
        mKeys = keyData;
        mKeyInterpolator = new SXRFloatAnimation(keyData, keySize);
        mCurrentValues = new float[keySize - 1];
    }

    /**
     * Computes the blend weights for the given time and
     * updates them in the target.
     */
    public void animate(float timeInSec)
    {
        SXRMeshMorph morph  = (SXRMeshMorph) mTarget;

        if (morph != null)
        {
            mKeyInterpolator.animate(timeInSec, mCurrentValues);
            morph.setWeights(mCurrentValues);
        }
    }

    /**
     * Set the target morph being animated.
     *
     * @param morph {@SXRMeshMorph} with weights to animate.
     */
    public void setTarget(SXRMeshMorph morph)
    {
        int numweights = morph.getBlendShapeCount();
        if (numweights != mKeyInterpolator.getKeySize() - 1)
        {
            throw new IllegalArgumentException("The number blend weights on the target morph must match the animation");
        }
        mTarget = morph;
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent)
    {
        sb.append(Log.getSpaces(indent));
        sb.append(SXRMorphAnimation.class.getSimpleName());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }
}

