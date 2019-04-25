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

package com.samsungxr.animation.keyframe;

import com.samsungxr.PrettyPrint;
import com.samsungxr.SXRHybridObject;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Animates a skeleton with separate animation channels for each bone.
 * <p>
 * Skeletal animation is performed by modify the current pose of
 * the {@link SXRSkeleton} associated with this animator.
 * Each bone of the skeleton can be driven be a separate
 * {@link SXRAnimationChannel} which contains a sequence
 * of translations, rotations and scaling to apply to the bone
 * over time.
 * <p>
 * Each frame the skeleton animation asks the animation channels
 * associated with each bone to compute the matrix for animating
 * that bone. This matrix is used to update the current pose
 * of the skeleton and the nodes associated with the bone.
 * After all channels have been evaluated, the skinning pose is
 * computed to drive skinned meshes.
 * @see SXRSkeleton
 * @see com.samsungxr.animation.SXRSkin
 * @see SXRPose
 */
public class SXRSkeletonAnimation extends SXRAnimation implements PrettyPrint {
    private SXRSkeleton mSkeleton = null;


    /**
     * List of animation channels for each of the
     * animated bones.
     */
    protected SXRAnimationChannel[] mBoneChannels;

    /**
     * Create a skeleton animation with bones from the given hierarchy.
     *
     * @param name The name of the animation.
     * @param target The target hierachy containing nodes for bones.
     * @param duration Duration of the animation in seconds.
     */
    public SXRSkeletonAnimation(String name, SXRNode target, float duration)
    {
        super(target, duration);
        mName = name;
    }

    /**
     * Create a skeleton animation with bones from the given hierarchy.
     *
     * @param name The name of the animation.
     * @param skel The skeleton being animated.
     * @param duration Duration of the animation in seconds.
     */
    public SXRSkeletonAnimation(String name, SXRSkeleton skel, float duration)
    {
        super(skel.getOwnerObject(), duration);
        mName = name;
        mSkeleton = skel;
        for (int boneId = 0; boneId < mSkeleton.getNumBones(); ++boneId)
        {
            mSkeleton.setBoneOptions(boneId, SXRSkeleton.BONE_ANIMATE);
        }
        mBoneChannels = new SXRAnimationChannel[mSkeleton.getNumBones()];
    }

    /**
     * Get the skeleton for this animation.
     * @return {@link SXRSkeleton} this animation drives.
     */
    public SXRSkeleton getSkeleton() { return mSkeleton; }

    /**
     * Add a channel to the animation to animate the named bone.
     * @param boneName  name of bone to animate.
     * @param channel   The animation channel.
     */
    public void addChannel(String boneName, SXRAnimationChannel channel)
    {
        int boneId = mSkeleton.getBoneIndex(boneName);
        if (boneId >= 0)
        {
            mBoneChannels[boneId] = channel;
            mSkeleton.setBoneOptions(boneId, SXRSkeleton.BONE_ANIMATE);
            Log.d("BONE", "Adding animation channel %d %s ", boneId, boneName);
        }
    }

    /**
     * Get the number of animation channels
     */
    public int getNumChannels()
    {
        if (mBoneChannels == null)
        {
            return 0;
        }
        return mBoneChannels.length;
    }

    /**
     * Find the channel in the animation that animates the named bone.
     * @param boneName  name of bone to animate.
     */
    public SXRAnimationChannel findChannel(String boneName)
    {
        int boneId = mSkeleton.getBoneIndex(boneName);
        if (boneId >= 0)
        {
            return mBoneChannels[boneId];
        }
        return null;
    }

    private SXRNode findParent(SXRNode child, List<String> boneNames)
    {
        SXRNode parent = child.getParent();

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
     * @return {@link SXRSkeleton} created from the target hierarchy.
     */
    public SXRSkeleton createSkeleton(List<String> boneNames)
    {
        int numBones = boneNames.size();
        SXRNode root = (SXRNode) mTarget;
        mSkeleton = new SXRSkeleton(root, boneNames);
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            mSkeleton.setBoneOptions(boneId, SXRSkeleton.BONE_ANIMATE);
        }
        mBoneChannels = new SXRAnimationChannel[numBones];
        return mSkeleton;
    }

    public void setSkeleton(SXRSkeleton skel, List<String> boneNames)
    {
        int numBones = skel.getNumBones();

        mSkeleton = skel;
        if (boneNames != null)
        {
            for (int boneId = 0; boneId < numBones; ++boneId)
            {
                mSkeleton.setBoneName(boneId, boneNames.get(boneId));
            }
        }
        if (mBoneChannels == null)
        {
            mBoneChannels = new SXRAnimationChannel[numBones];
        }
        else
        {
            for (int i = 0; i < mBoneChannels.length; ++i)
            {
                if (mBoneChannels[i] != null)
                {
                    skel.setBoneOptions(i, SXRSkeleton.BONE_ANIMATE);
                }
            }
        }
    }

    public void setTarget(SXRNode target)
    {
        mTarget = target;
        if ((mSkeleton != null) &&
                target.getComponent(SXRSkeleton.getComponentType()) == null)
        {
            target.attachComponent(mSkeleton);
        }
    }

    /**
     * Compute pose of skeleton at the given time from the animation channels.
     * @param timeInSec animation time in seconds.
     */
    public void animate(float timeInSec)
    {
        SXRSkeleton skel = getSkeleton();
        SXRPose pose = skel.getPose();

        if (skel.isEnabled())
        {
            synchronized (skel)
            {
                computePose(timeInSec, pose);
                skel.poseToBones();
            }
        }
    }

    public SXRPose computePose(float timeInSec, SXRPose pose)
    {
        Matrix4f temp = new Matrix4f();
        SXRSkeleton skel = getSkeleton();
        Vector3f rootOffset = skel.getRootOffset();

        for (int i = 0; i < skel.getNumBones(); ++i)
        {
            SXRAnimationChannel channel = mBoneChannels[i];
            if ((channel != null) &&
                (skel.getBoneOptions(i) == SXRSkeleton.BONE_ANIMATE))
            {
                channel.animate(timeInSec, temp);
                if (rootOffset != null)
                {
                    temp.m30(rootOffset.x + temp.m30());
                    temp.m31(rootOffset.y + temp.m31());
                    temp.m32(rootOffset.z + temp.m32());
                    rootOffset = null;
                }
                pose.setLocalMatrix(i, temp);
            }
        }
        return pose;
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(SXRSkeletonAnimation.class.getSimpleName());
        sb.append("[name=" + mName + ", duration=" + getDuration() + ", "
                + getNumChannels() + " channels]");
        sb.append(System.lineSeparator());

        for (SXRAnimationChannel nodeAnim : mBoneChannels)
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
