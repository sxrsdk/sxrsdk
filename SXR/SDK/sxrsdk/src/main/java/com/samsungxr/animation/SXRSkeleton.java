/* Copyright 2018 Samsung Electronics Co., LTD
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

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTransform;
import com.samsungxr.SXRVertexBuffer;
import com.samsungxr.PrettyPrint;
import com.samsungxr.animation.keyframe.BVHImporter;
import com.samsungxr.nodes.SXRCylinderNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Component that animates a skeleton based on a set of bones.
 * <p>
 * This class provides a common infrastructure for skeletal animation.
 * It can construct an animation hierarchy representing the bones of a skeleton
 * which can be used to animate a skinned character made up of multiple meshes.
 * <p>
 * You can set the bone names in the skeleton
 * to designate which nodes in your hierarchy represent which bones.
 * If you attach the {@link SXRSkeleton} component to the root of a hierarchy of nodes,
 * it will link the skeleton bones to the nodes so that, when you change the
 * skeleton, the nodes will reflect that change.
 * <p>
 * The asset loader constructs the {@link SXRSkeleton} and attaches the bones when you export a rigged character.
 * It also makes a {@link com.samsungxr.animation.keyframe.SXRSkeletonAnimation} class
 * which associates the animations in the asset with the appropriate skeleton bones.
 * <p>
 * {@link SXRSkeleton} relies on the {@link SXRPose} class to represent the position and orientation of its bones.
 * All bones in the skeleton start out at the origin oriented along the bone axis (usually 0,0,1).
 * The pose orients and positions each bone in the skeleton respect to this initial state by maintaining
 * a matrix for each bone. The root bone which is the parent of all the other bones in the
 * hierarchy should be the first bone (with index 0).
 * <p>
 * The <i>bind pose</i> of the skeleton defines the neutral position of the bones before any
 * external transformations are applied. Usually it represents the pose of the skeleton
 * matching the meshes which will be skinned from the skeleton. The bone transformations
 * used for skinning the mesh are relative to the bind pose.
 * <p>
 * The <i>current pose</i> of the skeleton defines the current orientation and position of the bones.
 * It uses the same conventions as the bind pose, relative to bones at the origin along the bone axis
 * It is <b>not</b> relative to the bind pose, instead it is relative to the root of the skeleton.
 * When the current pose is updated, the skeleton modifies the matrices
 * attached to the models in the hierarchy and the bone matrices.
 * <p>
 *  Each mesh that is skinned by the skeleton bones has a {@link SXRSkin} component attached
 *  which tracks which bones in the skeleton affect the mesh. The skin is responsible
 *  for updating the matrices in the GPU which drive skinning.
 * </p>
 * @see SXRSkin
 * @see SXRPose
 * @see com.samsungxr.animation.keyframe.SXRSkeletonAnimation
 */
public class SXRSkeleton extends SXRComponent implements PrettyPrint
{
    /**
     * world positions and orientations are relative to the root bone of the skeleton.
     */
    public static final int SKELETON_ROOT = 0;

    /*
     * pose only contains local rotations
     */
    public static final int ROTATION_ONLY = 4;

    /*
     * Bone rotation is locked
     */
    public static final int BONE_LOCK_ROTATION = 1;

    /*
     * Bone driven by animation
     */
    public static final int BONE_ANIMATE = 4;
    /*
     * Bone driven by physics
     */
    public static final int BONE_PHYSICS = 2;

    private static final String TAG = Log.tag(SXRSkeleton.class);
    protected SXRNode[] mBones;
    protected int[] mParentBones;
    protected int[] mBoneOptions;
    final private Quaternionf mTempQuatA = new Quaternionf();
    final private Quaternionf mTempQuatB = new Quaternionf();
    final private Matrix4f mTempMtx = new Matrix4f();
    private static int[] sTempBoneParents;
    private SXRNode sTempRoot;

    protected String[] mBoneNames;
    protected Vector3f mRootOffset;     // offset for root bone animations
    protected SXRPose mPose;            // current pose for this skeleton
    protected float[] mPoseMatrices;

    static public long getComponentType()
    {
        return NativeSkeleton.getComponentType();
    }

    /**
     * Construct a skeleton given a bone hierarchy
     * @param ctx           {@link SXRContext} to associate the skeleton with
     * @param parentBones   Array of integers describing the bone hierarchy.
     *                      Each bone has an index, with bone 0 being the root bone.
     *                      Parent bones must appear in the array before their children.
     *                      Each entry in the array gives the index of that bone's
     *                      parent, -1 indicates the root bone (no parent).
     * @see #setBoneName(int, String)
     * @see #getBoneIndex(String)
     * @see #getParentBoneIndex(int)
     */
    public SXRSkeleton(SXRContext ctx, int[] parentBones)
    {
        super(ctx, NativeSkeleton.ctor(parentBones));
        mType = getComponentType();
        mParentBones = parentBones;
        mRootOffset = new Vector3f(0, 0, 0);
        mBoneOptions = new int[parentBones.length];
        mBoneNames = new String[parentBones.length];
        mBones = new SXRNode[parentBones.length];
        mPose = new SXRPose(this);
        mPoseMatrices =  new float[parentBones.length * 16];
    }

    /**
     * Construct a skeleton given a scene object hierarchy.
     * <p>
     * The scene object hierarchy is used to determine how the
     * bones are connected. Only nodes that are in the bone name list will be
     * in the skeleton.
     * @param root          {@link SXRNode} at the root of the hierarchy.
     * @param boneNames     Array of string with the names of skeleton bones.
     *                      parent, -1 indicates the root bone (no parent).
     * @see #setBoneName(int, String)
     * @see #getBoneIndex(String)
     * @see #getParentBoneIndex(int)
     */
    public SXRSkeleton(SXRNode root, List<String> boneNames)
    {
        super(root.getSXRContext(), 0);
        setNative(NativeSkeleton.ctor(makeParentBoneIds(root, boneNames)));
        int numBones = boneNames.size();
        mType = getComponentType();
        mParentBones = sTempBoneParents;
        mRootOffset = new Vector3f(0, 0, 0);
        mBoneOptions = new int[numBones];
        mBoneNames = new String[numBones];
        mBones = new SXRNode[numBones];
        mPoseMatrices =  new float[numBones * 16];
        mPose = new SXRPose(this);
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            String boneName = boneNames.get(boneId);
            setBoneName(boneId, boneName);
        }
        if (sTempRoot != null)
        {
            sTempRoot.attachComponent(this);
        }
    }

    /**
     * Construct a {@link SXRSkeleton} from a native C++ skeleton.
     * <p>
     * The C++ skeleton is assume to already have been constructed
     * and able to provide bone parentage, name and pose information.
     * @param ctx           {@link SXRContext} the skeleton belongs to.
     * @param nativePtr     -> C++ Skeleton component - cannot be null.
     * @see #setBoneName(int, String)
     * @see #getBoneIndex(String)
     * @see #getParentBoneIndex(int)
     */
    public SXRSkeleton(SXRContext ctx, long nativePtr)
    {
        super(ctx, nativePtr);
        int numBones = NativeSkeleton.getNumBones(nativePtr);
        mType = getComponentType();
        mParentBones = new int[numBones];
        mRootOffset = new Vector3f(0, 0, 0);
        mBoneOptions = new int[numBones];
        mBoneNames = new String[numBones];
        mBones = new SXRNode[numBones];
        mPoseMatrices =  new float[numBones * 16];
        NativeSkeleton.getBoneParents(nativePtr, mParentBones);
        NativeSkeleton.getPose(nativePtr, mPoseMatrices);
        mPose = new SXRPose(this);
        Matrix4f mtx = new Matrix4f();
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            mtx.set(mPoseMatrices, boneId * 16);
            mPose.setWorldMatrix(boneId, mtx);
            mBoneNames[boneId] = NativeSkeleton.getBoneName(nativePtr, boneId);
        }
        mPose.sync();
    }

    protected int[] makeParentBoneIds(SXRNode root, List<String> boneNames)
    {
        int numBones = boneNames.size();
        int[] parentBones = new int[numBones];
        int numRoots = 0;
        sTempRoot = null;
        Arrays.fill(parentBones, -1);
        for (int boneId = 0; boneId < numBones; ++boneId)
        {
            String boneName = boneNames.get(boneId);
            SXRNode obj = root.getNodeByName(boneName);

            if (obj == null)
            {
                Log.e("BONE", "bone %s not found in scene", boneName);
                continue;
            }
            SXRNode parent = obj.getParent();
            int parBoneId = -1;

            if (parent != null)
            {
                String nodeName = parent.getName();
                parBoneId = boneNames.indexOf(nodeName);
            }
            if ((parBoneId < 0) && (sTempRoot == null))
            {
                sTempRoot = obj;
                Log.d("BONE", "Skeleton root %d is %s", numRoots, boneNames.get(boneId));
                ++numRoots;
            }
            parentBones[boneId] = parBoneId;
        }
        sTempBoneParents = parentBones;
        return parentBones;
    }

    /**
     * Get world space position of skeleton (position of the root bone, index 0).
     * <p>
     * The world space position of the skeleton is the translation of
     * the root bone (index 0) of the current pose.
     */
    public void getPosition(Vector3f pos)
    {
        mPose.getWorldPosition(0, pos);
    }

    /**
     * Sets the world space position of the skeleton.
     * <p>
     * @param pos world space position of the skeleton.
     *            <p>
     *            The world space position of the skeleton is the translation of
     *            the root bone (index 0) of the current pose. Setting the skeleton position
     *            directly updates both the root bone position and the translation
     *            on the target model of the skeleton.
     * @see SXRPose#setWorldPositions
     */
    public void setPosition(Vector3f pos)
    {
        mPose.setPosition(pos.x, pos.y, pos.z);
    }

    /**
     * Gets the offset of the root bone when animated.
     * <p>
     * This offset is useful for moving the starting point of animations.
     * For example, you can make a zero-based animation relative to the
     * current position of the character by setting this offset to that position.
     * By default, this offset is zero.
     *
     * @see #setRootOffset(Vector3f)
     * @see #poseFromBones()
     */
    public Vector3f getRootOffset()
    {
        return mRootOffset;
    }

    /**
     * Sets the offset of the root bone when animated.
     * <p>
     * This offset is useful for moving the starting point of animations.
     * For example, you can make a zero-based animation relative to the
     * current position of the character by setting this offset to that position.
     * By default, this offset is zero.
     * <p>
     * The root bone offset only affects animations. It does not affect any
     * updates to the current pose.
     * @param ofs vector with new root bone offset.
     * @see #getRootOffset()
     * @see #poseFromBones()
     */
    public void setRootOffset(Vector3f ofs)
    {
        mRootOffset = ofs;
    }

    /**
     * Get the bounding volume of the skeleton's bones.
     * @return float array with minX, minY, minZ, maxX, maxY maxZ
     */
    public float[] getBound()
    {
        return mPose.getBound();
    }

    /**
     * Get the current skeleton pose.
     * <p>
     * The<i>current pose</i> is the pose the skeleton is currently in. It contains the
     * location and orientation of each bone relative to the skeleton root bone.
     * The current pose is a reference to an internal {@link SXRPose} that changes dynamically.
     * Modifications made to this pose will affect the skeleton.
     *
     * @see SXRPose
     * @see #setPose
     * @see SXRPose#setWorldRotations
     * @see SXRPose#setWorldPositions
     */
    public SXRPose getPose()
    {
        return mPose;
    }

    /**
     * Get the pose from the native skeleton.
     * <p>
     * Usually the animation of a skeleton is controlled from Java.
     * In this case, the current pose of the skeleton is maintained
     * in a Java data structure. If the skeleton animation is controlled
     * by C++ code, the current pose of the skeleton is not known to
     * Java and must be obtained by calling this function.
     * After calling the function, the current pose of the skeleton
     * is updated from the values calculated by the native C++ code.
     * Modifications made to this pose will affect the skeleton.
     *
     * @see SXRPose
     * @see #setPose
     * @see SXRPose#setWorldRotations
     * @see SXRPose#setWorldPositions
     */
    public void getNativePose()
    {
        getNativePose(mPoseMatrices);
    }

    public void getNativePose(float[] poseMatrices)
    {
        if (poseMatrices.length < getNumBones())
        {
            throw new IllegalArgumentException("Not enough room in array to capture the pose");
        }
        SXRPose pose = getPose();
        int ofs = 0;
        NativeSkeleton.getPose(getNative(), poseMatrices);
        for (int i = 0; i < mParentBones.length; ++i)
        {
            mTempMtx.set(poseMatrices, ofs);
            pose.setLocalMatrix(i, mTempMtx);
            ofs += 16;
        }
        pose.sync();
    }

    /**
     * Sets the the current pose for this skeleton.
     * <p>
     * The given {@link SXRPose} object becomes linked to the skeleton
     * so that changes to this pose change the bones of the skeleton
     * and any skinned meshes driven by it.
     * The current pose contains the current orientation and position of the bones in the skeleton,
     * relative to bones at the origin along the bone axis.
     * The "world" matrices of the bind pose are relative to the root bone
     * of the skeleton.
     *
     * @param pose {@link @SXRPose} containing rotations and positions relative
     *                              to the root bone of the skeleton.
     * @see #getPose
     */
    public void setPose(SXRPose pose)
    {
        SXRSkeleton skel = pose.getSkeleton();
        if (skel != this)
        {
            throw new IllegalArgumentException("setPose: input pose has incompatible skeleton");
        }
        synchronized (this)
        {
            if (pose == mPose)
            {
                return;
            }
            pose.sync();
            mPose.copy(pose);
            updateBonePose();
        }
    }

    /**
     * Updates the current pose of a skeleton without replacing it.
     * <p>
     * This function does not require the input pose to be relative to the root bone,
     * it will also accept poses relative to the bind pose of the skeleton.
     * It does not replace the current pose with the new pose, it updates the
     * current pose in place. If a bone is locked, it's input is ignored.
     *
     * @param newpose new pose to apply.
     * @param poseSpace indicates the coordinate space of the input pose.
     * <TABLE>
     * <TR><TD>SKELETON_ROOT</TD>
     * <TD>Indicates the world matrices in the pose are relative to the root of the skeleton.</TD>
     * </TR>
     * <TR><TD>BIND_POSE_RELATIVE</TD>
     * <TD>Indicates the local rotations in the pose are relative to the bind pose the skeleton.
     * Translations are ignored.</TD>
     * </TR>
     * <TR><TD>ROTATION_ONLY</TD>
     * <TD>Indicates the local rotations in the pose are relative to the root of the skeleton.
     * Translations are ignored.</TD>
     * </TR>
     * </TABLE>
     * @see #setPose
     * @see SXRPose
     */
    public void applyPose(SXRPose newpose, int poseSpace)
    {
        synchronized (this)
        {
            int numbones = getNumBones();

            /*
             * Apply input pose that contains only local rotations.
             */
            if (poseSpace == ROTATION_ONLY)
            {
                for (int i = 0; i < numbones; ++i)
                {
                    SXRPose.Bone srcBone = newpose.getBone(i);

                    if ((srcBone.Changed != 0) && !isLocked(i))
                    {
                        newpose.getLocalRotation(i, mTempQuatA);
                        mPose.setLocalRotation(i, mTempQuatA.x, mTempQuatA.y, mTempQuatA.z, mTempQuatA.w);
                        srcBone.Changed = 0;
                    }
                }
            }
            /*
             * Apply the world rotations from the input pose to
             * the skeleton's current pose. The rotations are
             * already relative to the skeleton root.
             */
            else
            {
                newpose.sync();
                if (mPose != newpose)
                {
                    for (int i = 0; i < numbones; ++i)
                    {
                        if (!isLocked(i))
                        {
                            SXRPose.Bone srcBone = newpose.getBone(i);
                            srcBone.getLocalMatrix(mTempMtx);
                            mPose.setLocalMatrix(i, mTempMtx);
                        }
                    }
                }
            }
            mPose.sync();
            updateBonePose();
        }
    }

    /**
     * Updates the current pose of a skeleton and applies the
     * transform of the {@link SXRSkeleton} owner.
     * <p>
     * This function does not require the input pose to be relative to the root bone,
     * it will also accept poses relative to the bind pose of the skeleton.
     * It does not replace the current pose with the new pose, it updates the
     * current pose in place. If a bone is locked, it's input is ignored.
     *
     * @param newpose new pose to apply, supports all pose spaces.
     * @see #setPose
     * @see SXRPose
     */
    public void transformPose(SXRPose newpose)
    {
        synchronized (this)
        {
            int numbones = getNumBones();
            Matrix4f rootMtx = owner.getTransform().getModelMatrix4f();

            rootMtx.getUnnormalizedRotation(mTempQuatB);
            mTempQuatB.conjugate();
            newpose.sync();
            for (int i = 0; i < numbones; ++i)
            {
                SXRPose.Bone srcBone = newpose.getBone(i);

                if ((mBoneOptions[i] & BONE_LOCK_ROTATION) == 0)
                {
                    rootMtx.mul(mTempMtx, mTempMtx);
                    mTempMtx.mul(srcBone.LocalMatrix, mTempMtx);
                    mPose.setWorldMatrix(i, mTempMtx);
                }
            }
            updateBonePose();
        }
    }

    /**
     * Gets the bone index for the parent of the specified bone.
     *
     * @param boneindex index of bone whose parent to set.
     * @return parent index or -1 for root bone
     * @see #getBoneIndex
     */
    public int getParentBoneIndex(int boneindex)
    {
        return mParentBones[boneindex];
    }

    /**
     * Get the bone index for the given node.
     *
     * @param bone SXRNode bone to search for
     * @return bone index or -1 for root bone.
     * @see #getParentBoneIndex
     */
    public int getBoneIndex(SXRNode bone)
    {
        synchronized (this)
        {
            for (int i = 0; i < getNumBones(); ++i)
            {
                if (mBones[i] == bone)
                {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Get the bone index for the bone with the given name.
     *
     * @param bonename string identifying the bone whose index you want
     * @return 0 based bone index or -1 if bone with that name is not found.
     */
    public int getBoneIndex(String bonename)
    {
        synchronized (this)
        {
            for (int i = 0; i < getNumBones(); ++i)
            {
                if (mBoneNames[i].equals(bonename))
                {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Gets the number of bones in the skeleton.
     * <p>
     * The number of bones is established when the skeleton is first created
     * and cannot be subsquently changed.
     */
    public int getNumBones()
    {
        return mParentBones.length;
    }

    /**
     * Sets the name of the designated bone.
     *
     * @param boneindex zero based index of bone to rotate.
     * @param bonename  string with name of bone.
.     * @see #getBoneName
     */
    public void setBoneName(int boneindex, String bonename)
    {
        synchronized (this)
        {
            mBoneNames[boneindex] = bonename;
            NativeSkeleton.setBoneName(getNative(), boneindex, bonename);
        }
    }

    /**
     * Get the name of a bone given it's index.
     *.
     * @param boneindex index of bone to get
     * @return name of bone or null if not found.
     */
    public String getBoneName(int boneindex)
    {
        synchronized (this)
        {
            return mBoneNames[boneindex];
        }
    }

    /**
     * Get the list of bone names in the order of bone indices.
     * @return bone name list
     */
    public final String[] getBoneNames()
    {
        synchronized (this)
        {
            return mBoneNames;
        }
    }

    /**
     * Set the bone names for all of the bones from an array.
     * This array will now be owned by the skeleton,
     * it is not copied. This function throws an exception
     * if the length of the list does not match the number
     * of bones in the skeleton.
     * @see #getBoneNames()
     * @see #getBoneName(int)
     */
    public void setBoneNames(String[] boneNames)
    {
        synchronized (this)
        {
            if (getNumBones() != boneNames.length)
            {
                throw new IllegalArgumentException("Bone names array has wrong length");
            }
            mBoneNames = boneNames;
        }
    }

    /**
     * Get the node driven by the given bone.
     *
     * @param boneindex index of bone to get.
     * @return SXRNode whose transform represents the bone.
     */
    public SXRNode getBone(int boneindex)
    {
        synchronized (this)
        {
            return mBones[boneindex];
        }
    }

    public void setBone(int boneindex, SXRNode bone)
    {
        synchronized (this)
        {
            mBones[boneindex] = bone;
        }
    }

    /**
     * Set rotation and physics options for this bone.
     *
     * @param boneindex 0 based bone index.
     * @param options   Options to control how this bone moves.
     *                  {@link #BONE_PHYSICS} will use physics (rigid body dynamics) to calculate
     *                  the motion of the bone.
     *                  {@link #BONE_LOCK_ROTATION} will lock the bone rotation, freezing its current
     *                  local rotation.
     *                  {@link #BONE_ANIMATE} uses animation to drive the bone.
     * @see SXRPose#setLocalRotations
     * @see SXRPose#setWorldRotations
     * @see #poseFromBones()
     * @see #poseToBones()
     */
    public void setBoneOptions(int boneindex, int options)
    {
        synchronized (this)
        {
            mBoneOptions[boneindex] = options;
        }
    }

    /**
     * Get rotation and physics options for this bone.
     *
     * @param boneindex 0 based bone index.
     * @see #setBoneOptions
     */
    public int getBoneOptions(int boneindex)
    {
        synchronized (this)
        {
            return mBoneOptions[boneindex];
        }
    }

    /**
     * Determine if given bone is locked or not.
     *
     * @param boneindex 0 based bone index.
     */
    public boolean isLocked(int boneindex)
    {
        synchronized (this)
        {
            return (mBoneOptions[boneindex] & BONE_LOCK_ROTATION) != 0;
        }
    }

    public void onAttach(SXRNode root)
    {
        super.onAttach(root);
        attachBones(null);
    }

    public void onDetach(SXRNode root)
    {
        super.onDetach(root);
        synchronized (this)
        {
            for (int i = 0; i < mBones.length; ++i)
            {
                setBone(i, null);
            }
        }
    }

    /**
     * Links the bones in the skeleton with scene nodes in the owner's hierarchy.
     * <p>
     * Examines the descendants of the skeleton owner looking for nodes whose names match the
     * bones in the skeleton. This function assumes the hierarchy below the owner contains
     * the nodes to animate to control the skeleton in the scene.
     * The input pose will have the positions and rotations of the nodes.
     */
    public void attachBones(final SXRPose savepose)
    {
        SXRNode owner = getOwnerObject();

        if (owner == null)
        {
            return;
        }
        SXRNode.SceneVisitor visitor = new SXRNode.SceneVisitor()
        {
            @Override
            public boolean visit(SXRNode newbone)
            {
                String bonename = newbone.getName();

                if (bonename.isEmpty())            // ignore nodes without names
                {
                    return true;
                }
                int boneindex = getBoneIndex(bonename);
                if (boneindex >= 0)                // a bone we recognize?
                {
                    setBone(boneindex, newbone);
                    if (savepose != null)
                    {
                        savepose.setWorldMatrix(boneindex, newbone.getTransform().getModelMatrix4f());
                    }
                }
                return true;
            }
        };
        synchronized (this)
        {
            owner.forAllDescendants(visitor);
        }
    }

    /**
     * Applies the matrices computed from the node's
     * linked to the skeleton bones to the current pose.
     * @see #applyPose(SXRPose, int)
     * @see #setPose(SXRPose)
     */
    public void poseFromBones()
    {
        synchronized (this)
        {
            for (int i = 0; i < getNumBones(); ++i)
            {
                SXRNode bone = mBones[i];
                if (bone == null)
                {
                    continue;
                }
                if ((mBoneOptions[i] & BONE_LOCK_ROTATION) != 0)
                {
                    continue;
                }
                SXRTransform trans = bone.getTransform();
                mPose.setLocalMatrix(i, trans.getLocalModelMatrix4f());
            }
            mPose.sync();
            updateBonePose();
        }
    }

    /**
     * Applies the matrices computed from the node's
     * linked to the skeleton bones to the current pose.
     * @param boneOptions   Only update bones with the given options
     *                      (BONE_PHYSICS or BONE_ANIMATE)
     * @see #applyPose(SXRPose, int)
     * @see #setPose(SXRPose)
     */
    public void poseFromBones(int boneOptions)
    {
        synchronized (this)
        {
            for (int i = 0; i < getNumBones(); ++i)
            {
                SXRNode bone = mBones[i];
                if ((bone != null) && (mBoneOptions[i] & boneOptions) != 0)
                {
                    SXRTransform trans = bone.getTransform();
                    mPose.setLocalMatrix(i, trans.getLocalModelMatrix4f());
                }
            }
            mPose.sync();
            updateBonePose();
        }
    }

    /**
     * Applies the matrices from the skeleton's current pose
     * to the nodes associated with each bone.
     * <p>
     * The {@link com.samsungxr.animation.keyframe.SXRSkeletonAnimation} class
     * does this as a part of skeletal animation. It does not occur
     * automatically when the current pose is updated.
     * @see #applyPose(SXRPose, int)
     * @see #setPose(SXRPose)
     */
    public void poseToBones()
    {
        synchronized (this)
        {
            mPose.sync();
            for (int i = 0; i < getNumBones(); ++i)
            {
                SXRNode bone = mBones[i];
                if ((bone != null) && ((mBoneOptions[i] & BONE_LOCK_ROTATION) == 0))
                {
                    mPose.getLocalMatrix(i, mTempMtx);
                    bone.getTransform().setModelMatrix(mTempMtx);
                }
            }
            updateBonePose();
        }
    }

    /*
     * Update the C++ skeleton with the current pose.
     * <p>
     * The skin pose is relative to the untransformed mesh.
     * The bone pose is relative to the skeleton bone.
     * The skin pose is used to transform the vertices before
     * overall translate, rotation and scale are performed.
     * It is the current pose relative to the bind pose
     * of the mesh.
     * <p>
     * Each skinned mesh has an associated @{link SXRSkin} component
     * which manages the bones that affect that mesh.
     * This skin component maintains the inverse bind pose
     * of the mesh so it can be multiplied by the world bone pose
     * to compute the proper skin pose.
     * This function updates the local and world pose matrices each frame.
     * @see SXRSkin
     */
    public void updateBonePose()
    {
        mPose.sync();
        mPose.getLocalMatrices(mPoseMatrices);
        NativeSkeleton.setPose(getNative(), mPoseMatrices);
        mPose.getWorldMatrices(mPoseMatrices);
        NativeSkeleton.setWorldPose(getNative(), mPoseMatrices);
    }

    /**
     * Merge the source skeleton with this one.
     * The result will be that this skeleton has all of its
     * original bones and all the bones in the new skeleton.
     *
     * @param newSkel skeleton to merge with this one
     */
    public void merge(SXRSkeleton newSkel)
    {
        synchronized (this)
        {
            int numBones = getNumBones();
            int[] parentBoneIds = new int[newSkel.getNumBones()];
            List<String> newBoneNames = new ArrayList<String>(newSkel.getNumBones());
            List<Matrix4f> matrices = new ArrayList<Matrix4f>(numBones + newSkel.getNumBones());
            List<SXRNode> bones = new ArrayList<SXRNode>(newSkel.getNumBones());
            int numNewBones = 0;
            SXRPose curPose = getPose();
            SXRPose newPose = newSkel.getPose();

            /*
             * Accumulate the bind pose matrices and parent bone IDs
             * for the old skeleton
             */
            for (int j = 0; j < numBones; ++j)
            {
                Matrix4f m = new Matrix4f();
                curPose.getLocalMatrix(j, m);
                matrices.add(m);
            }
            /*
             * Add bones in the new skeleton that are not in
             * the old skeleton.
             */
            for (int j = 0; j < newSkel.getNumBones(); ++j)
            {
                String boneName = newSkel.getBoneName(j);
                int boneId = getBoneIndex(boneName);
                Matrix4f m = new Matrix4f();
                int parentId;

                newPose.getLocalMatrix(j, m);
                parentId = newSkel.getParentBoneIndex(j);
                bones.add(newSkel.getBone(j));
                if (boneId < 0)            // source bone not in existing skeleton?
                {
                    if (parentId >= 0)     // parent is found?
                    {
                        String parName = newSkel.getBoneName(parentId);
                        parentId = getBoneIndex(parName);
                        if (parentId < 0)  // parent is a new bone
                        {
                            parentId = newBoneNames.indexOf(parName);
                            if (parentId >= 0)
                            {
                                parentId += numBones;
                            }
                        }
                    }
                    if (parentId < 0)
                    {
                        parentId = 0;
                    }
                    matrices.add(m);
                    newBoneNames.add(boneName);
                    parentBoneIds[numNewBones] = parentId;
                    ++numNewBones;
                }
            }
            if (numNewBones == 0)
            {
                return;
            }
            /*
             * Enlarge arrays in this skeleton to accomodate
             * the new bones added and copy the old data
             */
            int n = numBones + numNewBones;
            int[] parentIds = Arrays.copyOf(mParentBones, n);
            int[] boneOptions = Arrays.copyOf(mBoneOptions, n);
            String[] boneNames = Arrays.copyOf(mBoneNames, n);

            mBones = Arrays.copyOf(mBones, n);
            mPoseMatrices = new float[n * 16];
            /*
             * Add bones from the new skeleton into this skeleton
             */
            for (int i = 0; i < numNewBones; ++i)
            {
                int m = numBones + i;
                parentIds[m] = parentBoneIds[i];
                boneNames[m] = newBoneNames.get(i);
                setBone(m, bones.get(i));
            }
            mBoneOptions = boneOptions;
            mBoneNames = boneNames;
            mParentBones = parentIds;
            mPose = new SXRPose(this);

            /*
             * Update the native skeleton
             */
            NativeSkeleton.updateBones(getNative(), mParentBones, boneNames);
            /*
             * Update the poses for this skeleton
             */
            for (int j = 0; j < n; ++j)
            {
                mPose.setLocalMatrix(j, matrices.get(j));
            }
            mPose.sync();
            poseToBones();
        }
    }

    public SXRNode createSkeletonGeometry(SXRNode parent, float boneScale)
    {
        GeometryHelper helper = new GeometryHelper(this);
        return helper.createSkeletonGeometry(parent, boneScale);
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(System.lineSeparator());
        sb.append(Log.getSpaces(indent) + 2);
        sb.append("numBones = " + Integer.toString(getNumBones()));
        sb.append(System.lineSeparator());
        for (int i = 0; i < getNumBones(); ++i)
        {
            String boneName = getBoneName(i);
            sb.append(Log.getSpaces(indent) + 4);
            sb.append(Integer.toString(i));
            sb.append(": ");
            sb.append(boneName);
        }
    }

    private static class GeometryHelper
    {
        private SXRNode mSphereProto;
        private SXRNode mCylProto;
        private final SXRSkeleton mSkeleton;

        public GeometryHelper(SXRSkeleton skel)
        {
            mSkeleton = skel;
        }

        public SXRNode createSkeletonGeometry(SXRNode root, float boneScale)
        {
            SXRContext ctx = mSkeleton.getSXRContext();
            SXRMaterial flatMaterialSphr = new SXRMaterial(ctx);
            SXRMaterial flatMaterialCyl = new SXRMaterial(ctx);

            flatMaterialSphr.setColor(1f, 1f, 0f);
            flatMaterialCyl.setColor(1f, 0f, 0f);
            mCylProto =  new SXRCylinderNode(ctx, 0.2f * boneScale, 0.2f * boneScale, 1f, 2, 8, true);
            mSphereProto =  new SXRSphereNode(ctx, true, flatMaterialSphr, boneScale * 0.5f);
            mCylProto.getRenderData().setMaterial(flatMaterialCyl);
            SXRNode rootGeo = makeSpheres();

            if (rootGeo.getParent() == null)
            {
                root.addChildObject(rootGeo);
            }
            mSkeleton.poseToBones();
            return root;
        }

        private SXRNode makeSpheres()
        {
            SXRContext ctx = mSkeleton.getSXRContext();
            SXRPose pose = mSkeleton.getPose();
            Vector3f childDir = new Vector3f(0,0,0);

            for (int j = 0; j < mSkeleton.getNumBones(); j++)
            {
                SXRNode boneGeo = mSkeleton.getBone(j);
                int parentIndex = mSkeleton.getParentBoneIndex(j);
                String boneName = mSkeleton.getBoneName(j);

                if (boneGeo == null)
                {
                    boneGeo = new SXRNode(ctx, mSphereProto.getRenderData().getMesh(),
                                                mSphereProto.getRenderData().getMaterial());
                    mSkeleton.setBone(j, boneGeo);
                    boneGeo.setName(boneName);
                }
                else if (boneGeo.getRenderData() != null)
                {
                    continue;
                }
                else
                {
                    SXRRenderData rdata = new SXRRenderData(ctx);
                    rdata.setMesh(mSphereProto.getRenderData().getMesh());
                    rdata.setMaterial(mSphereProto.getRenderData().getMaterial());
                    boneGeo.attachRenderData(rdata);
                }
                if (parentIndex >= 0)
                {
                    SXRNode parent = mSkeleton.getBone(parentIndex);
                    if (parent != null)
                    {
                        parent.addChildObject(boneGeo);
                        float height = calcCylHeight(j);
                        pose.getLocalPosition(j, childDir);

                        SXRNode cyl = createBoneGeo(childDir, height);
                        cyl.setName(mSkeleton.getBoneName(parentIndex) + "_" + boneName);
                        parent.addChildObject(cyl);
                    }
                }
            }
            return mSkeleton.getBone(0);
        }

        private float calcCylHeight(int boneIndex)
        {
            Vector3f p = new Vector3f(0,0,0);

            mSkeleton.getPose().getLocalPosition(boneIndex, p);
            return (float) p.length();
        }

        private SXRNode createBoneGeo(Vector3f boneDir, float height)
        {
            SXRContext ctx = mCylProto.getSXRContext();
            Vector3f downNormal = new Vector3f(0,-1,0);
            Quaternionf q = new Quaternionf(0,0,0,1);
            Quaternionf quatRot = q.rotateTo(downNormal, boneDir);
            SXRMesh oldMesh = mCylProto.getRenderData().getMesh();
            SXRVertexBuffer oldVerts = oldMesh.getVertexBuffer();
            SXRMesh newMesh = new SXRMesh(ctx, oldVerts.getDescriptor());
            float[] verts = oldMesh.getVertices();

            quatRot.normalize();
            for (int t = 0; t < verts.length; t += 3)
            {
                Vector3f dest = new Vector3f(0,0,0);
                quatRot.transform(verts[t], (verts[t + 1] - 0.5f) * height, verts[t + 2], dest);
                verts[t] = dest.x();
                verts[t + 1] = dest.y();
                verts[t + 2] = dest.z();
            }
            newMesh.setIndexBuffer(oldMesh.getIndexBuffer());
            newMesh.setNormals(oldMesh.getNormals());
            newMesh.setVertices(verts);
            return new SXRNode(mCylProto.getSXRContext(), newMesh, mCylProto.getRenderData().getMaterial());
        }
    }
}

class NativeSkeleton
{
    static native long ctor(int[] boneParents);

    static native int getNumBones(long object);

    static native boolean getBoneParents(long object, int[] parentIds);

    static native void updateBones(long object, int[] parentids, String[] bonenames);

    static native long getComponentType();

    static native boolean setPose(long object, float[] matrices);

    static native boolean getPose(long object, float[] matrices);

    static native boolean setWorldPose(long object, float[] matrices);

    static native void setBoneName(long object, int index, String name);

    static native String getBoneName(long object, int index);
};
