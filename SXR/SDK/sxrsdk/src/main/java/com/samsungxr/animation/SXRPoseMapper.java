package com.samsungxr.animation;

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
    protected int         mBoneOptions = 0;
    protected float       mScale = 1.0f;

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

    /**
     * Construct one pose mapper from another.
     * @param src   {@link SXRPoseMapper} to copy.
     */
    public SXRPoseMapper(final SXRPoseMapper src)
    {
        this(src.mDestSkeleton, src.mSourceSkeleton, src.mDuration);
    }

    @Override
    public SXRAnimation copy()
    {
        return new SXRPoseMapper(this);
    }

    public SXRAnimation setDuration(float dur)
    {
        mDuration = dur;
        return this;
    }

    @Override
    public SXRAnimation setStartOffset(float start)
    {
        mStartOffset = start;
        return this;
    }

    /**
     * Set the bone options for the bones that should be mapped.
     * @param options (one of SXRSkeleton.BONE_PHYSICS or SXRSkeleton.BONE_ANIMATE
     */
    public void setBoneOptions(int options)
    {
        mBoneOptions = options;
    }

    /**
     * Get the bone options for the bones that should be mapped.
     * @return SXRSkeleton.BONE_PHYSICS or SXRSkeleton.BONE_ANIMATE
     */
    public int getBoneOptions()
    {
        return mBoneOptions;
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
        synchronized (mDestSkeleton)
        {
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
                    mDestSkeleton.setBoneOptions(destIndex, mDestSkeleton.getBoneOptions(destIndex) | SXRSkeleton.BONE_ANIMATE);
                    Log.w("BONE", "%s %d -> %s %d", words[0], sourceIndex, words[1], destIndex);
                }
                else
                {
                    Log.w("SXRPoseMapper", "makeBoneMap: cannot find bone " + words[0]);
                }
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
        SXRPose srcPose = srcskel.getPose();
        SXRPose dstPose = dstskel.getPose();

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
                dstskel.setBoneOptions(boneindex, dstskel.getBoneOptions(boneindex) | SXRSkeleton.BONE_ANIMATE);
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
    public void animate(float timeInSec)
    {
        if ((mSourceSkeleton == null) || !mSourceSkeleton.isEnabled() || !mDestSkeleton.isEnabled())
        {
            return;
        }
        synchronized (mDestSkeleton)
        {
            mapLocalToTarget();
            mDestSkeleton.poseToBones();
        }
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
        if (!dstskel.isEnabled() || !srcskel.isEnabled())
        {
            return false;
        }
        if (mBoneMap == null)
        {
            mBoneMap = makeBoneMap(srcskel, dstskel);
        }
        synchronized (srcskel)
        {
            SXRPose srcpose = srcskel.getPose();
            Quaternionf q = new Quaternionf();
            int numsrcbones = srcskel.getNumBones();

            if (mDestPose.getNumBones() != dstskel.getNumBones())
            {
                mDestPose = new SXRPose(dstskel);
            }
            else
            {
                mDestPose.clearRotations();
            }
            srcskel.getPosition(v);
            v.mul(mScale);
            for (int i = 0; i < numsrcbones; ++i)
            {
                int boneindex = mBoneMap[i];

                if (boneindex >= 0)
                {
                    srcpose.getLocalRotation(i, q);
                    mDestPose.setLocalRotation(boneindex, q.x, q.y, q.z, q.w);
                }
            }
        }
        synchronized (dstskel)
        {
            dstskel.setPosition(v);
            dstskel.applyPose(mDestPose, SXRSkeleton.ROTATION_ONLY, mBoneOptions);
        }
        return true;
    }

    /**
     * Scale the output pose by a given factor.
     * <p>
     * The scale factor is applied to the computed positions.
     * For example, you can take an animation that is originally in
     * centimeters and convert it to meters.
     * </p>
     * @param sf    positive scale factor
     * @see SXRSkin#scalePositions(float)
     */
    public void setScale(float sf)
    {
        if (sf <= 0)
        {
            throw new IllegalArgumentException("Scale factor must be positive");
        }
        mScale = sf;
    }

}