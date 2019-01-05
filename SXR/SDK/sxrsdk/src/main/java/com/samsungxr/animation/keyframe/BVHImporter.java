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
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Imports animation in the BioVision Hierarchical format
 * {@link "https://research.cs.wisc.edu/graphics/Courses/cs-838-1999/Jeff/BVH.html"}
 * BVH files are text and contain a skeleton description and animation data.
 * Only positions and rotations are animated in the BVH format.
 * <p>
 * The BVH importer imports the skeleton separately from the animation.
 * You can provide a compatible skeleton when importing the motion data
 * and the importer will output an animation for that skeleton.
 * <p>
 * The default output format of the BVH importer is a
 * {@link SXRSkeletonAnimation} with a {@link SXRAnimationChannel} for
 * each animated bone. The {@link BVHKey} interface is provided to
 * allow applications to provide their own position and rotation key
 * generation code. The keys must still be consecutive floats but
 * callers can do things like preserve the Euler angles in the
 * source animation format or scale the position keys.
 * @see SXRSkeleton
 * @see SXRSkeletonAnimation
 */
public class BVHImporter
{
    private String mFileName;
    private final SXRContext mContext;
    private final ArrayList<String> mBoneNames = new ArrayList();
    private final ArrayList<Vector3f> mBonePositions = new ArrayList();
    private final ArrayList<Integer> mBoneParents = new ArrayList();
    private final ArrayList<String> mBoneChannels = new ArrayList();
    private final BVHKey  mKeyMaker;
    private BufferedReader mReader;
    private SXRSkeleton mSkeleton;

    /**
     * Interface for making position and rotation keys from BVH data.
     * Position keys are assumed to be three floats on input and output.
     * Rotation keys are the Euler angles from the original BVH motion data
     * on input. The output rotation key is a fixed number of floats.
     */
    public interface BVHKey
    {
        /**
         * Called at the start of motion data import.
         * @param skeleton  {@link SXRSkeleton} the motion data is applied to.
         */
        public void start(SXRSkeleton skeleton);

        /**
         * Called to generate the next rotation key.
         * On entry the array contains the input rotation angles.
         * On return the array will have the output rotation key.
         * @param boneIndex index of bone this key applies to.
         * @param order     order of input rotation angles, "xyz", "yxz", "zxy", ...
         * @param rot       array containing the input key (3 floats).
         * @param offset    offset of input key in input array.
         */
        public void makeRotationKey(int boneIndex, String order, float[] rot, int offset);

        /**
         * Called to generate the next position key.
         * On entry the array contains the position values.
         * On return the array will have the output position key.
         * @param boneIndex index of bone this key applies to.
         * @param order     order of input position components, "xyz", "yxz", "zxy", ...
         * @param pos       array containing the input key (3 floats).
         * @param offset    offset of input key in input array.
         */
        public void makePositionKey(int boneIndex, String order, float[] pos, int offset);

        /**
         * Gets the size of a rotation key.
         * Each key is a set of consecutive floats.
         * Position keys are always 3 floats,
         * Rotation keys may Euler angles, quaternions,
         * angle axis, etc.
         * @return number of floats in a rotation key.
         */
        public int getRotKeySize();

        /**
         * Create an animation channel from the position and rotation keys.
         * <p>
         * {@link SXRAnimationChannel} assumes its input keys are quaternions.
         * Interfaces which change this format will need to subclass
         * SXRAnimationChannel to provide a way to animate rotation
         * using the custom key format.
         * @param boneName  name of bone tanimated by the channel.
         * @param posKeys   array of position keys.
         * @param rotKeys   array of rotation keys.
         * @return {@link SXRAnimationChannel} containing animation for this bone.
         */
        public SXRAnimationChannel makeAnimationChannel(String boneName, float[] posKeys, float[] rotKeys);
    };

    /**
     * Default implementation of BVH key generation.
     * Position keys are shuffled based on the order specified.
     * Rotations keys are converted from Euler rotations (3 floats)
     * in a specified order to quaternions (4 floats).
     */
    private final BVHKey mDefaultKeyMaker = new BVHKey()
    {
        private Quaternionf mTempQuat1;
        private Quaternionf mTempQuat2;
        private SXRPose mBindPose;
        private float[] mPosKey;

        @Override
        public void start(SXRSkeleton skel)
        {
            mTempQuat1 = new Quaternionf();
            mTempQuat2 = new Quaternionf();
            mPosKey = new float[3];
        }

        @Override
        public void makeRotationKey(int boneIndex, String order, float[] key, int offset)
        {
            makeQuat(mTempQuat1, order, key, offset);
            mBindPose.getLocalRotation(boneIndex, mTempQuat2);
            mTempQuat1.mul(mTempQuat2);
            key[offset++] = mTempQuat1.x;
            key[offset++] = mTempQuat1.y;
            key[offset++] = mTempQuat1.z;
            key[offset] = mTempQuat1.w;
        }

        @Override
        public void makePositionKey(int boneIndex, String order, float[] pos, int offset)
        {
            mPosKey[0] = pos[offset];
            mPosKey[1] = pos[offset + 1];
            mPosKey[2] = pos[offset + 2];
            for (int i = 0; i < 3; i++)
            {
                char c = order.charAt(i);
                float v = mPosKey[i];
                if (c == 'x')
                {
                    pos[offset] = v;
                }
                else if (c == 'y')
                {
                    pos[offset + 1] = v;
                }
                else
                {
                    pos[offset + 2] = v;
                }
            }
        }

        @Override
        public int getRotKeySize()
        {
            return 4;
        }

        private void makeQuat(Quaternionf q, String order, float[] angles, int offset)
        {
            float deg2rad =  (float) Math.PI / 180;
            char c = order.charAt(0);
            float v = angles[offset];

            if (c == 'x')
            {
                q.rotationX(v * deg2rad);
            }
            else if (c == 'y')
            {
                q.rotationY(v * deg2rad);
            }
            else
            {
                q.rotationZ(v * deg2rad);
            }
            for (int i = 1; i < 3; i++)
            {
                v = angles[offset + i];
                c = order.charAt(i);
                if (c == 'x')
                {
                    q.rotateX(v * deg2rad);
                }
                else if (c == 'y')
                {
                    q.rotateY(v * deg2rad);
                }
                else
                {
                    q.rotateZ(v * deg2rad);
                }
            }
            q.normalize();
        }

        public SXRAnimationChannel makeAnimationChannel(String boneName, float[] posKeys, float[] rotKeys)
        {
            return new SXRAnimationChannel(boneName, posKeys, rotKeys, null, SXRAnimationBehavior.DEFAULT, SXRAnimationBehavior.DEFAULT);
        }
    };

    /**
     * Create a BVHImporter instance with custom key generation.
     * @param ctx       {@link SXRContext} to use for animation.
     * @param keyMaker  {@link BVHKey} interface to make position and rotation keys.
     */
    public BVHImporter(SXRContext ctx, BVHKey keyMaker)
    {
        mContext = ctx;
        if (keyMaker != null)
        {
            mKeyMaker = keyMaker;
        }
        else
        {
            mKeyMaker = mDefaultKeyMaker;
        }
    }

    /**
     * Create a BVHImporter instance with default key generation.
     * Positions are 3 floats and rotations are quaternions (4 floats).
     * @param ctx       {@link SXRContext} to use for animation.
     */
    public BVHImporter(SXRContext ctx)
    {
        mContext = ctx;
        mKeyMaker = mDefaultKeyMaker;
    }

    /**
     * Import the animation from the input resource and apply it to the specified skeleton.
     * <p>
     * This skeleton should contain the same bones names as those used in the BVH file.
     * @param res       {@link SXRAndroidResource} containing the BVH animation data.
     * @param skel      {@link SXRSkeleton} to animate.
     * @return {@link SXRSkeletonAnimation} containing the animation data read from the BVH file.
     * @throws IOException if BVH file cannot be opened.
     */
    public SXRSkeletonAnimation importAnimation(SXRAndroidResource res, SXRSkeleton skel) throws IOException
    {
        InputStream stream = res.getStream();

        mFileName = res.getResourceFilename();
        if (stream == null)
        {
            throw new IOException("Cannot open " + mFileName);
        }
        InputStreamReader inputreader = new InputStreamReader(stream);
        mReader = new BufferedReader(inputreader);
        readSkeleton();
        return readMotion(skel);
    }

    public SXRPose importPose(SXRAndroidResource res)  throws IOException
    {
        InputStream stream = res.getStream();

        if (stream == null)
        {
            throw new IOException("Cannot open " + res.getResourceFilename());
        }
        InputStreamReader inputreader = new InputStreamReader(stream);
        mReader = new BufferedReader(inputreader);
        readSkeleton();
        SXRSkeleton skel = createSkeleton();
        return readPose(skel);
    }

    /**
     * Import the animation from the input resource.
     * The skeleton is constructed from the BVH file.
     * @param res       {@link SXRAndroidResource} containing the BVH animation data.
     * @return {@link SXRSkeletonAnimation} containing the animation data read from the BVH file.
     * @throws IOException if BVH file cannot be opened.
     */
    public SXRSkeleton importSkeleton(SXRAndroidResource res) throws IOException
    {
        InputStream stream = res.getStream();

        if (stream == null)
        {
            throw new IOException("Cannot open " + res.getResourceFilename());
        }
        InputStreamReader inputreader = new InputStreamReader(stream);
        mReader = new BufferedReader(inputreader);
        mFileName = res.getResourceFilename();
        readSkeleton();
        return createSkeleton();
    }

    private int readSkeleton() throws IOException
    {
        String line;

        while ((line = mReader.readLine()) != null)
        {
            line.trim();
            String[] words;

            if (line == "")
                continue;
            words = line.split("[ \t]");
            if (words[0].equals("ROOT"))
            {
                parseJoint(words[1], -1);
                return mBoneParents.size();
            }
        }
        return 0;
    }

    private void parseJoint(String bonename, final int parentIndex) throws IOException
    {
        String      line;
        final int   boneIndex = mBoneParents.size();

        mBoneParents.add(boneIndex, parentIndex);
        mBoneNames.add(boneIndex, bonename);
        mBoneChannels.add(boneIndex, "");
        while ((line = mReader.readLine().trim()) != null)
        {
            String[] words = line.split("[ \t]");
            String opcode;

            if (line == "")
                continue;
            if (words.length < 1)           // has an argument?
                continue;
            opcode = words[0];
            if (opcode.equals("End"))       // end site
            {
                bonename = "end_" + mBoneNames.get(boneIndex);
                parseJoint(bonename, boneIndex);
            }
            else if ((opcode.equals("ROOT")) ||   // found root bone?
                    (opcode.equals("JOINT")))      // found any bone?
            {
                parseJoint(words[1], boneIndex);
            }
            else if (opcode.equals("OFFSET"))       // bone position
            {
                float xpos = Float.parseFloat(words[1]);
                float ypos = Float.parseFloat(words[2]);
                float zpos = Float.parseFloat(words[3]);

                mBonePositions.add(boneIndex, new Vector3f(xpos, ypos, zpos));
            }
            else if (opcode.equals("CHANNELS"))
            {
                String channelOrder = "";
                for (int j = 2; j < words.length; j++)
                {
                    if (words[j].equals("Xposition"))  //positions order
                    {
                        channelOrder += 'x';
                    }
                    else if (words[j].equals("Yposition"))
                    {
                        channelOrder += 'y';
                    }
                    else if (words[j].equals("Zposition"))
                    {
                        channelOrder += 'z';
                    }
                    else if (words[j].equals("Xrotation"))  //rotations order
                    {
                        channelOrder += 'x';
                    }
                    else if (words[j].equals("Yrotation"))
                    {
                        channelOrder += 'y';
                    }
                    else if (words[j].equals("Zrotation"))
                    {
                        channelOrder += 'z';
                    }
                }
                mBoneChannels.add(boneIndex, channelOrder);
            }
            else if (opcode.equals("MOTION") || opcode.equals("}"))
            {
                break;
            }
        }
    }

    public SXRSkeleton createSkeleton()
    {
        int[] boneparents = new int[mBoneParents.size()];
        SXRSkeleton skel;

        for (int i = 0; i < mBoneParents.size(); ++i)
        {
            boneparents[i] = mBoneParents.get(i);
        }
        skel = new SXRSkeleton(mContext, boneparents);
        SXRPose bindpose = new SXRPose(skel);

        for (int i = 0; i < mBoneNames.size(); ++i)
        {
            Vector3f p = mBonePositions.get(i);
            bindpose.setLocalPosition(i, p.x, p.y, p.z);
            skel.setBoneName(i, mBoneNames.get(i));
        }
        skel.setPose(bindpose);
        return skel;
    }

    private SXRPose readPose(SXRSkeleton skel) throws IOException
    {
        float       x, y, z;
        String      line;
        SXRPose     pose = new SXRPose(skel);

        mSkeleton = skel;
        /*
         * Parse and accumulate all the motion keyframes.
         * Keyframes for the root bone position are in rootPosKeys;
         * Keyframes for each bone's rotations are in rootKeysPerBone;
         */
        while ((line = mReader.readLine().trim()) != null)
        {
            String[]    words;

            line = line.trim();
            words = line.split("[\t ]");
            if (line == "")
            {
                continue;
            }
            if (words[0].startsWith("Frame"))
            {
                continue;
            }
            int boneIndex = 0;
            int i = 0;
            float[] p = new float[3];
            float[] r = new float[mDefaultKeyMaker.getRotKeySize()];
            while (i + 5 < words.length)
            {
                String order = mBoneChannels.get(boneIndex);
                if (order.isEmpty())
                {
                    continue;
                }
                if (order.length() > 3)
                {
                    Float.parseFloat(words[i]);
                    Float.parseFloat(words[i + 1]);
                    Float.parseFloat(words[i + 2]);
                    mDefaultKeyMaker.makePositionKey(boneIndex, order, p, 0);
                    order = order.substring(3);
                    pose.setLocalPosition(boneIndex, p[0], p[1], p[2]);
                    i += 3;
                }
                r[0] = Float.parseFloat(words[i]);
                r[1] = Float.parseFloat(words[i + 1]);
                r[2] = Float.parseFloat(words[i + 2]);
                mDefaultKeyMaker.makeRotationKey(boneIndex, order, r, 0);
                pose.setLocalRotation(boneIndex, r[0], r[1], r[2], r[3]);
                boneIndex++;
            }
        }
        return pose;
    }


    /**
     * Import the motion data from the currently open BVH file.
     *
     * @param skel      {@link SXRSkeleton} to animate.
     * @return {@link SXRSkeletonAnimation} with an {@SXRAnimationChannel} for each animated bone.
     * @throws IOException if motion data cannot be read.
     */
    public SXRSkeletonAnimation readMotion(SXRSkeleton skel) throws IOException
    {
        int                 numbones = skel.getNumBones();
        ArrayList<float[]>  rotKeysPerBone = new ArrayList<>(numbones);
        ArrayList<float[]>  posKeysPerBone = new ArrayList<>(numbones);
        int                 rotKeySize = mKeyMaker.getRotKeySize();
        int                 frameIndex = 0;
        int                 numFrames = 0;
        String              line;
        float               secondsPerFrame = 0;
        float               curTime = 0;

        mSkeleton = skel;
        mKeyMaker.start(skel);
        /*
         * Parse and accumulate all the motion keyframes.
         * Keyframes for the root bone position are in rootPosKeys;
         * Keyframes for each bone's rotations are in rootKeysPerBone;
         */
        while ((line = mReader.readLine()) != null)
        {
            String[] words;

            line = line.trim();
            if (line == "")
            {
                continue;
            }
            words = line.split("[\t ]");
            if (words[0].equals("MOTION"))
            {
                continue;
            }
            if (words[0].startsWith("Frames"))
            {
                for (int i = 0; i < numbones; i++)
                {
                    numFrames = Integer.parseInt(words[1]);
                    posKeysPerBone.add(new float[4 * numFrames]);
                    rotKeysPerBone.add(new float[(rotKeySize + 1) * numFrames]);
                }
                continue;
            }
            if (words[0].equals("Frame") && words[1].startsWith("Time"))
            {
                secondsPerFrame = Float.parseFloat(words[2]);
                continue;
            }
            /*
             * Parsing motion for each frame.
             * Each line in the file contains the root joint position and rotations for all joints.
             */
            Matrix4f mtx = new Matrix4f();
            int boneIndex = 0;
            int i = 0;
            int f;
            float x, y, z;
            float deg2rad = (float) Math.PI / 180.0f;
            float[] pos = new float[3];
            float[] rot = new float[mKeyMaker.getRotKeySize()];
            while (i + 3 <= words.length)
            {
                String order = mBoneChannels.get(boneIndex);
                String bonename = mBoneNames.get(boneIndex);
                if (bonename == null)
                {
                    throw new IOException("Cannot find bone " + bonename + " in skeleton");
                }
                if (order.isEmpty())
                {
                    ++boneIndex;
                    continue;
                }
                if (order.length() > 3)
                {
                    float[] posKeys = posKeysPerBone.get(boneIndex);
                    f = frameIndex * 4;
                    posKeys[f++] = curTime;
                    posKeys[f] = Float.parseFloat(words[i]);
                    posKeys[f + 1] = Float.parseFloat(words[i + 1]);
                    posKeys[f + 2] = Float.parseFloat(words[i + 2]);
                    mKeyMaker.makePositionKey(boneIndex, order, posKeys, f);
                    order = order.substring(3);
                    i += 3;
                }
                float[] rotKeys = rotKeysPerBone.get(boneIndex);
                f = (rotKeySize + 1) * frameIndex;
                rotKeys[f++] = curTime;
                rotKeys[f] = Float.parseFloat(words[i]);
                rotKeys[f + 1] = Float.parseFloat(words[i + 1]);
                rotKeys[f + 2] = Float.parseFloat(words[i + 2]);
                i += 3;
                mKeyMaker.makeRotationKey(boneIndex, order, rotKeys, f);
                boneIndex++;
                //Log.d("BVH", "%s %f %f %f %f", bonename, q.x, q.y, q.z, q.w);
            }
            curTime += secondsPerFrame;
            if (++frameIndex >= numFrames)
            {
                break;
            }
        }
        return  makeAnimation(curTime, posKeysPerBone, rotKeysPerBone);
    }

    /*
     * Create a skeleton animation with separate channels for each bone
     */
    protected SXRSkeletonAnimation makeAnimation(float duration, ArrayList<float[]> posKeysPerBone,  ArrayList<float[]> rotKeysPerBone)
    {
        SXRAnimationChannel channel;
        SXRSkeletonAnimation skelanim = new SXRSkeletonAnimation(mFileName, mSkeleton, duration);
        Vector3f pos = new Vector3f();
        for (int boneIndex = 0; boneIndex < mBoneNames.size(); ++boneIndex)
        {
            String order = mBoneChannels.get(boneIndex);
            if (order.isEmpty())
            {
                continue;
            }
            String bonename = mBoneNames.get(boneIndex);
            float[] rotKeys = rotKeysPerBone.get(boneIndex);
            float[] posKeys = posKeysPerBone.get(boneIndex);
            if (order.length() == 3)
            {
                mSkeleton.getPose().getLocalPosition(boneIndex, pos);
                posKeys = new float[]{0, pos.x, pos.y, pos.z};
            }
            channel = mKeyMaker.makeAnimationChannel(bonename, posKeys, rotKeys);
            skelanim.addChannel(bonename, channel);
        }
        return skelanim;
    }
}

