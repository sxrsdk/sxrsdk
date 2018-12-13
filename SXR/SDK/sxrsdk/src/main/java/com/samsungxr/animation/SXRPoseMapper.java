package com.samsungxr.animation;

import com.samsungxr.SXRHybridObject;
import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

public class SXRPoseMapper extends SXRAnimation
{
    protected SXRSkeleton mSourceSkeleton;
    protected SXRSkeleton mDestSkeleton;
    protected int[]       mBoneMap;
    protected SXRPose     mDestPose;

    /**
     * Constructs an animation retargeting engine.
     */
    public SXRPoseMapper(SXRSkeleton dstskel, float duration)
    {
        super(dstskel, duration);
        mDestSkeleton = dstskel;
        mDestPose = new SXRPose(dstskel);
    }

    /**
     * Constructs an animation retargeting engine.
     */
    public SXRPoseMapper(SXRSkeleton dstskel, SXRSkeleton srcskel, float duration)
    {
        super(dstskel, duration);
        mDestSkeleton = dstskel;
        mSourceSkeleton = srcskel;
        mDestPose = new SXRPose(dstskel);
    }

    public SXRAnimation setDuration(float start, float end)
    {
        animationOffset =  start;
        mDuration = end - start;
        return this;
    }

    /**
     * Set the source skeleton
     * @param source	source skeleton
     *
     * The source skeleton provides the input animation
     * which will be remapped to the target skeleton.
     *
     * @see SXRSkeleton
     */
    public void	setSourceSkeleton(SXRSkeleton source)
    {
        mSourceSkeleton = source;
    }

    /**
     * Get the source skeleton
     * @returns source skeleton (may be null if not set)
     *
     * The source skeleton provides the input animation
     * which will be remapped to the target skeleton.
     *
     * @see SXRSkeleton
     */
    public SXRSkeleton	getSourceSkeleton()
    {
        return mSourceSkeleton;
    }

    /**
     * Get the target skeleton
     * @returns target skeleton (will not be null)
     *
     * The target skeleton is modified by the pose
     * from the source skeleton.
     *
     * @see SXRSkeleton
     */
    public SXRSkeleton	getTargetSkeleton()
    {
        return mDestSkeleton;
    }

    /**
     * Get the bone map between source and target.
     * <p>
     * The bone map specifies the source bone index for each
     * target bone. It is an array of integers
     * with an entry for each target skeleton bone.
     * In the case where the target skeleton has a bone that is not in
     * the source skeleton, the index should be -1.
     * @return integer array with bone mapping or null if not defined
     *
     * @see SXRSkeleton
     * @see #setBoneMap(int[])
     */
    public int[] getBoneMap() { return mBoneMap; }

    /**
     * Set the bone map between source and target.
     * @param bonemap	bone mapping data
     *
     * The bone map specifies the source bone index for each
     * target bone. The input should be an array of integers
     * with an entry for each target skeleton bone.
     * In the case where the target skeleton has a bone that is not in
     * the source skeleton, the index should be -1.
     *
     * @see SXRSkeleton
     */
    public void	setBoneMap(int[] bonemap)
    {
        SXRSkeleton	dstskel = mDestSkeleton;
        int			numbones;

        if (bonemap == null)
        {
            return;
        }
        if (dstskel == null)
        {
            return;
        }
        numbones = dstskel.getNumBones();
        if (numbones == 0)
        {
            return;
        }
        mBoneMap = bonemap;
    }

    /**
     * Set the bone map between source and target.
     * @param bonemap	string with source to target bone mappings
     *
     * Each line in the bone map string contains the name of a source
     * bone followed by the name of the corresponding target bone.
     * The names can be separated by spaces or tabs.
     *
     * @see SXRSkeleton
     */
    public void setBoneMap(String bonemap)
    {
        if ((bonemap == null) || bonemap.isEmpty())
        {
            throw new IllegalArgumentException("BoneMap cannot be empty");
        }
        if (mSourceSkeleton == null)
        {
            throw new IllegalArgumentException("Source skeleton cannot be null");
        }
        String[] lines = bonemap.split("[\r\n]");

        mBoneMap = new int[mSourceSkeleton.getNumBones()];
        Arrays.fill(mBoneMap, -1);
        for (String line : lines)
        {
            String[] words;

            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }
            words = line.split("[\t ]");
            int sourceIndex = mSourceSkeleton.getBoneIndex(words[0]);
            int destIndex = mDestSkeleton.getBoneIndex(words[1]);

            if ((sourceIndex >= 0) && (destIndex >= 0))
            {
                mBoneMap[sourceIndex] = destIndex;
                Log.w("BONE", "%s %d -> %s %d",
                      words[0], sourceIndex, words[1], destIndex);
            }
            else
            {
                Log.w("SXRPoseMapper", "makeBoneMap: cannot find bone " + words[0]);
            }
        }
    }

    /*!
     * @param	srcskel	source Skeleton
     * @pararm	dstskel	destination Skeleton
     *
     * Constructs a bone mapping table which gives the index of the destination skeleton bone
     * corresponding to each bone in the source skeleton.
     * @returns 	array with bone mapping indices
     * @see #mapPose
     */
    public int[] makeBoneMap(SXRSkeleton srcskel, SXRSkeleton dstskel)
    {
        int numsrcbones = srcskel.getNumBones();
        int[] bonemap = new int[numsrcbones];
        SXRPose srcPose = srcskel.getBindPose();
        SXRPose dstPose = dstskel.getBindPose();

        for (int i = 0; i < numsrcbones; ++i)
        {
            int boneindex = -1;
            String bonename = srcskel.getBoneName(i);

            if (bonename != null)
            {
                boneindex = dstskel.getBoneIndex(bonename);
            }
            bonemap[i] = boneindex;
            if (boneindex >= 0)
            {
                Log.w("BONE", "%s\n%d: %s\n%d: %s",
                        bonename, i, srcPose.getBone(i).toString(),
                        boneindex, dstPose.getBone(boneindex).toString());
            }
            else
            {
                Log.w("SXRPoseMapper", "makeBoneMap: cannot find bone " + bonename);
            }
        }
        return bonemap;
    }

    /*
     * Updates the color and depth map textures from the Kinect cameras.
     * If a Skeleton is our target or a child, we update the joint angles
     * for the user associated with it.
     */
    public void animate(SXRHybridObject target, float time)
    {
        if ((mSourceSkeleton == null) || !mSourceSkeleton.isEnabled())
        {
            return;
        }
        mapLocalToTarget();
        mDestSkeleton.poseToBones();
        mDestSkeleton.updateBonePose();
        mDestSkeleton.updateSkinPose();
    }


    /**
     * Maps the pose of the source skeleton onto the destination skeleton in local space.
     * <p>
     * The local bone rotations of matching bones are copied.
     * If the PoseMapper has a bone map, it is used to determine which bones
     * of the source skeleton correspond to which bones in the destination skeleton.
     * This function requires both the source and target skeletons to be set.
     *
     * @returns true if successful, false on error
     */
    public boolean mapLocalToTarget()
    {
        SXRSkeleton	srcskel = mSourceSkeleton;
        SXRSkeleton	dstskel = mDestSkeleton;
        Vector3f v = new Vector3f();

        if ((dstskel == null) || (srcskel == null))
        {
            return false;
        }
        if (mBoneMap == null)
        {
            mBoneMap = makeBoneMap(srcskel, dstskel);
        }
        SXRPose     srcpose = srcskel.getPose();
        Quaternionf q = new Quaternionf();
        int		    numsrcbones = srcskel.getNumBones();
        mDestPose.clearRotations();
        srcskel.getPosition(v);
        dstskel.setPosition(v);
        for (int i = 0; i < numsrcbones; ++i)
        {
            int	boneindex = mBoneMap[i];

            if (boneindex >= 0)
            {
                srcpose.getLocalRotation(i, q);
                mDestPose.setLocalRotation(boneindex, q.x, q.y, q.z, q.w);
            }
        }
        dstskel.applyPose(mDestPose, SXRSkeleton.ROTATION_ONLY);
        return true;
    }

    public boolean mapBindPose()
    {
        SXRSkeleton	srcskel = mSourceSkeleton;
        SXRSkeleton	dstskel = mDestSkeleton;
        Vector3f v = new Vector3f();
        Matrix4f mtx = new Matrix4f();

        if ((dstskel == null) || (srcskel == null))
        {
            return false;
        }
        if (mBoneMap == null)
        {
            mBoneMap = makeBoneMap(srcskel, dstskel);
        }
        SXRPose     srcpose = srcskel.getPose();
        SXRPose     dstbindpose = dstskel.getBindPose();
        int		    numsrcbones = srcskel.getNumBones();

        srcskel.getPosition(v);
        dstskel.setPosition(v);
        for (int i = 0; i < numsrcbones; ++i)
        {
            int	boneindex = mBoneMap[i];

            if (boneindex >= 0)
            {
                dstbindpose.getLocalMatrix(boneindex, mtx);
                mtx.invert();
                mtx.mul(srcpose.getBone(i).LocalMatrix);
                mDestPose.setLocalMatrix(boneindex, mtx);
            }
        }
        dstskel.applyPose(mDestPose, SXRSkeleton.BIND_POSE_RELATIVE);
        return true;
    }

    /**
     * Maps the pose of the source skeleton onto the destination skeleton in world space.
     * <p>
     * The world bone rotations of matching bones are copied.
     * If the PoseMapper has a bone map, it is used to determine which bones
     * of the source skeleton correspond to which bones in the destination skeleton.
     */
    public boolean mapWorldToTarget()
    {
        SXRSkeleton	srcskel = mSourceSkeleton;
        SXRSkeleton	dstskel = mDestSkeleton;

        if ((dstskel == null) || (srcskel == null))
        {
            return false;
        }
        if (mBoneMap == null)
        {
            mBoneMap = makeBoneMap(srcskel, dstskel);
        }
        SXRPose srcpose = srcskel.getPose();
        SXRPose	dstpose = dstskel.getPose();
        Vector3f    v = new Vector3f();
        int			numsrcbones = srcpose.getNumBones();
        Matrix4f	mtx = new Matrix4f();

        srcpose.sync();
        srcpose.getWorldPosition(0, v);
        dstpose.setPosition(v.x, v.y, v.z);
        for (int i = 0; i < numsrcbones; ++i)
        {
            int	boneindex = mBoneMap[i];

            if (boneindex >= 0)
            {
                srcpose.getWorldMatrix(i, mtx);
                dstpose.setWorldMatrix(boneindex, mtx);
            }
        }
        dstpose.sync();
        return true;
    }


    /**
     * Maps the pose of the destination skeleton onto the source skeleton in world space.
     * <p>
     * The world bone rotations of matching bones are copied.
     * If the PoseMapper has a bone map, it is used to determine which bones
     * of the source skeleton correspond to which bones in the destination skeleton.
     */
    public boolean mapWorldToSource()
    {
        SXRSkeleton	srcskel = mSourceSkeleton;
        SXRSkeleton	dstskel = mDestSkeleton;

        if ((dstskel == null) || (srcskel == null))
        {
            return false;
        }
        if (mBoneMap == null)
        {
            mBoneMap = makeBoneMap(srcskel, dstskel);
        }

        SXRPose     srcpose = srcskel.getPose();
        SXRPose	    dstpose = dstskel.getPose();
        int			numsrcbones = srcpose.getNumBones();
        Matrix4f	mtx = new Matrix4f();
        Vector3f    v = new Vector3f();

        dstpose.sync();
        dstpose.getWorldPosition(0, v);
        srcpose.setPosition(v.x, v.y, v.z);
        for (int i = 0; i < numsrcbones; ++i)
        {
            int	boneindex = mBoneMap[i];

            if (boneindex >= 0)
            {
                dstpose.getWorldMatrix(boneindex, mtx);
                srcpose.setWorldMatrix(i, mtx);
            }
        }
        srcpose.sync();
        return true;
    }

}