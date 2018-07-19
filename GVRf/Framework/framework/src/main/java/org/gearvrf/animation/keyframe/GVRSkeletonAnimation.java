package org.gearvrf.animation.keyframe;

import org.gearvrf.GVRHybridObject;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.PrettyPrint;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRPose;
import org.gearvrf.animation.GVRSkeleton;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.List;

/**
 * Animates a skeleton with separate animation channels for each bone.
 * <p>
 * Skeletal animation is performed by modify the current pose of
 * the {@link GVRSkeleton} associated with this animator.
 * Each bone of the skeleton can be driven be a separate
 * {@link GVRAnimationChannel} which contains a sequence
 * of translations, rotations and scaling to apply to the bone
 * over time.
 * <p>
 * Each frame the skeleton animation asks the animation channels
 * associated with each bone to compute the matrix for animating
 * that bone. This matrix is used to update the current pose
 * of the skeleton and the scene objects associated with the bone.
 * After all channels have been evaluated, the skinning pose is
 * computed to drive skinned meshes.
 * @see GVRSkeleton
 * @see org.gearvrf.animation.GVRSkin
 * @see GVRPose
 */
public class GVRSkeletonAnimation extends GVRAnimation implements PrettyPrint {
    protected String mName;
    private int mNumRoots = 0;
    private GVRSceneObject mSkeletonRoot = null;
    private GVRSkeleton mSkeleton = null;

    /**
     * List of animation channels for each of the
     * animated bones.
     */
    protected GVRAnimationChannel[] mBoneChannels;

    /**
     * Create a skeleton animation with bones from the given hierarchy.
     *
     * @param name The name of the animation.
     * @param target The target hierachy containing scene objects for bones.
     * @param duration Duration of the animation in seconds.
     */
    public GVRSkeletonAnimation(String name, GVRSceneObject target, float duration)
    {
    	super(target, duration);
        mName = name;
    }

    /**
     * Get the skeleton for this animation.
     * @return {@link GVRSkeleton} this animation drives.
     */
    public GVRSkeleton getSkeleton() { return mSkeleton; }

    /**
     * Add a channel to the animation to animate the named bone.
     * @param boneName  name of bone to animate.
     * @param channel   The animation channel.
     */
    public void addChannel(String boneName, GVRAnimationChannel channel)
    {
        int boneId = mSkeleton.getBoneIndex(boneName);
        if (boneId >= 0)
        {
            mBoneChannels[boneId] = channel;
            Log.d("BONE", "Adding animation channel %d %s ", boneId, boneName);

        }
    }

    /**
     * Find the channel in the animation that animates the named bone.
     * @param boneName  name of bone to animate.
     */
    public GVRAnimationChannel findChannel(String boneName)
    {
        int boneId = mSkeleton.getBoneIndex(boneName);
        if (boneId >= 0)
        {
            return mBoneChannels[boneId];
        }
        return null;
    }

    private GVRSceneObject findParent(GVRSceneObject child, List<String> boneNames)
    {
        GVRSceneObject parent = child.getParent();

        if (parent == null)
        {
            return null;
        }
        String nodeName = parent.getName();
        int parBoneId = boneNames.indexOf(nodeName);

        if (parBoneId >= 0)
        {
            return parent;
        }
        return null;
    }

    /**
     * Create a skeleton from the target hierarchy which has the given bones.
     * <p>
     * The structure of the target hierarchy is used to determine bone parentage.
     * The skeleton will have only the bones designated in the list.
     * The hierarchy is expected to be connected with no gaps or unnamed nodes.
     * @param boneNames names of bones in the skeleton.
     * @return {@link GVRSkeleton} created from the target hierarchy.
     */
    public GVRSkeleton createSkeleton(List<String> boneNames)
    {
        int numBones = boneNames.size();
        final int[] parentBones = new int[numBones];
        GVRSceneObject root = (GVRSceneObject) mTarget;

        Arrays.fill(parentBones, -1);
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            String boneName = boneNames.get(boneId);
            GVRSceneObject obj = root.getSceneObjectByName(boneName);

            if (obj == null)
            {
                Log.e("BONE", "bone %s not found in scene", boneName);
                continue;
            }
            GVRSceneObject parent = findParent(obj, boneNames);
            int parBoneId = -1;

            if (parent == null)
            {
                if (mSkeletonRoot == null)
                {
                    mSkeletonRoot = obj;
                }
                Log.d("BONE", "Skeleton root %d is %s", mNumRoots, boneNames.get(boneId));
                ++mNumRoots;
            }
            else
            {
                parBoneId = boneNames.indexOf(parent.getName());
            }
            parentBones[boneId] = parBoneId;
        }
        mSkeleton = new GVRSkeleton(mTarget.getGVRContext(), parentBones);
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            mSkeleton.setBoneName(boneId, boneNames.get(boneId));
            mSkeleton.setBoneOptions(boneId, GVRSkeleton.BONE_ANIMATE);
        }
        mBoneChannels = new GVRAnimationChannel[numBones];
        mSkeletonRoot.attachComponent(mSkeleton);
        return mSkeleton;
    }

    @Override
    protected void animate(GVRHybridObject target, float ratio)
    {
        animate(getDuration() * ratio);
    }

    /**
     * Compute pose of skeleton at the given time from the animation channels.
     * @param timeInSec animation time in seconds.
     */
    public void animate(float timeInSec)
    {
        Matrix4f temp = new Matrix4f();
        GVRSkeleton skel = getSkeleton();
        GVRPose pose = skel.getPose();

        for (int i = 0; i < skel.getNumBones(); ++i)
        {
            GVRAnimationChannel channel = mBoneChannels[i];
            if (channel != null)
            {
                channel.animate(timeInSec, temp);
                pose.setLocalMatrix(i, temp);
            }
        }
        skel.poseToBones();
        skel.computeSkinPose();
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRSkeletonAnimation.class.getSimpleName());
        sb.append("[name=" + mName + ", duration=" + getDuration() + ", "
                + mBoneChannels.length + " channels]");
        sb.append(System.lineSeparator());

        for (GVRAnimationChannel nodeAnim : mBoneChannels)
        {
            nodeAnim.prettyPrint(sb, indent + 2);
        }
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

}
