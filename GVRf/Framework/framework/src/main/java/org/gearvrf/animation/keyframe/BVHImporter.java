package org.gearvrf.animation.keyframe;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRPose;
import org.gearvrf.animation.GVRSkeleton;
import org.gearvrf.animation.keyframe.GVRAnimationBehavior;
import org.gearvrf.animation.keyframe.GVRAnimationChannel;
import org.gearvrf.animation.keyframe.GVRSkeletonAnimation;
import org.gearvrf.utility.Log;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class BVHImporter
{
    private String mFileName;
    private final GVRContext mContext;
    private final ArrayList<String> mBoneNames = new ArrayList();
    private final ArrayList<Vector3f> mBonePositions = new ArrayList();
    private final ArrayList<Integer> mBoneParents = new ArrayList();
    private final ArrayList<Integer> mBoneChannels = new ArrayList();
    private BufferedReader mReader;

    public BVHImporter(GVRContext ctx)
    {
        mContext = ctx;
    }

    public GVRSkeletonAnimation importAnimation(GVRAndroidResource res, GVRSkeleton skel) throws IOException
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

    public GVRPose importPose(GVRAndroidResource res)  throws IOException
    {
        InputStream stream = res.getStream();

        if (stream == null)
        {
            throw new IOException("Cannot open " + res.getResourceFilename());
        }
        InputStreamReader inputreader = new InputStreamReader(stream);
        mReader = new BufferedReader(inputreader);

        readSkeleton();
        GVRSkeleton skel = createSkeleton();
        return readPose(skel);
    }

    public GVRSkeleton importSkeleton(GVRAndroidResource res) throws IOException
    {
        InputStream stream = res.getStream();

        if (stream == null)
        {
            throw new IOException("Cannot open " + res.getResourceFilename());
        }
        InputStreamReader inputreader = new InputStreamReader(stream);
        BufferedReader buffreader = new BufferedReader(inputreader);

        readSkeleton();
        return createSkeleton();
    }

    private int readSkeleton() throws IOException
    {
        int         numbones = 0;
        String      bonename = "";
        String      line;
        int         parentIndex = -1;
        int         boneIndex = 0;
        Stack<Integer> balancedBraces = new Stack<>();
        boolean     isJustClosed = false;
        boolean     isMultiple   = false;

        while ((line = mReader.readLine().trim()) != null)
        {
            String[]    words = line.split(" ");
            String      opcode;

            if (line == "")
                continue;
            /*
             * Parsing skeleton definition with joint names and positions.
             */
            if (words.length < 1)           // has an argument?
                continue;
            opcode = words[0];
            if (opcode.equals("End"))       // bone position
            {
                int i = 0;
                while (i < 3)
                {
                    mReader.readLine();
                    i++;
                }
                continue;
            }
            if (opcode.equals("}"))
            {
                parentIndex = balancedBraces.peek();
                balancedBraces.pop();
                if (!isJustClosed && !isMultiple)
                {
                    isJustClosed = true;
                }
                else
                {
                    isJustClosed = false;
                    isMultiple = true;
                }
            }
            if ((opcode.equals("ROOT")) ||       // found root bone?
                (opcode.equals("JOINT")))        // found any bone?
            {
                bonename = words[1];
                mBoneParents.add(boneIndex, parentIndex);
                mBoneNames.add(boneIndex, bonename);
                ++numbones;
            }

            else if (opcode.equals("OFFSET"))       // bone position
            {
                float xpos = Float.parseFloat(words[1]);
                float ypos = Float.parseFloat(words[2]);
                float zpos = Float.parseFloat(words[3]);

                if (bonename.length() > 0)    // save position for the bone
                {
                    mBonePositions.add(boneIndex, new Vector3f(xpos, ypos, zpos));
                    Log.d("BVH", "%s %f %f %f", bonename, xpos, ypos, zpos);
                }
            }
            else if (opcode.equals("CHANNELS"))
            {
                mBoneChannels.add(boneIndex, Integer.parseInt(words[1]));
                balancedBraces.push(parentIndex);
                if (isJustClosed == true)
                {
                    parentIndex++;
                }
                else
                {
                    parentIndex = boneIndex;
                }
                ++boneIndex;
                isJustClosed = false;
                isMultiple = false;
                bonename = "";
            }
            else if (opcode.equals("MOTION"))
            {
                break;
            }
        }
        return numbones;
    }

    private GVRSkeleton createSkeleton()
    {
        int[] boneparents = new int[mBoneParents.size()];
        GVRSkeleton skel;

        for (int i = 0; i < mBoneParents.size(); ++i)
        {
            boneparents[i] = mBoneParents.get(i);
        }
        skel = new GVRSkeleton(mContext, boneparents);
        for (int i = 0; i < mBoneNames.size(); ++i)
        {
            skel.setBoneName(i, mBoneNames.get(i));
        }
        return skel;
    }

    public GVRPose readPose(GVRSkeleton skel) throws IOException
    {
        float       x, y, z;
        String      line;
        String      bvhbonename = "";
        int         frameIndex = 0;
        Quaternionf q = new Quaternionf();
        GVRPose     pose = new GVRPose(skel);

        /*
         * Parse and accumulate all the motion keyframes.
         * Keyframes for the root bone position are in rootPosKeys;
         * Keyframes for each bone's rotations are in rootKeysPerBone;
         */

        while ((line = mReader.readLine().trim()) != null&&frameIndex < 1)
        {
            line = line.trim();
            String[]    words;
            if(line.contains("\t"))
            {
                words = line.split("\t");
            }
            else
            {
                words = line.split(" ");
            }
            if (line == "")
            {
                continue;
            }
            if (words[0].startsWith("Frames"))
            {
                continue;
            }
            if (words[0].startsWith("Frame "))
            {
                continue;
            }
            int boneIndex = 0;
            int bvhboneIndex = 0;
            int i = 0;
            while (i + 5 < words.length)
            {
                String boneNameSkel = skel.getBoneName(boneIndex);
                bvhbonename = mBoneNames.get(bvhboneIndex);

                if (bvhbonename.equals(boneNameSkel))
                {
                    if (bvhbonename == null)
                    {
                        throw new IOException("Cannot find bone " + bvhbonename + " in skeleton");
                    }
                    x = Float.parseFloat(words[i++]);    // positions
                    y = Float.parseFloat(words[i++]);
                    z = Float.parseFloat(words[i++]);
                    pose.setLocalPosition(boneIndex, x, y, z);

                    z = Float.parseFloat(words[i++]);        // Z, Y, X rotation angles
                    x = Float.parseFloat(words[i++]);
                    y = Float.parseFloat(words[i++]);

                    q.rotationZ(z * (float) Math.PI / 180);
                    q.rotateX(x * (float) Math.PI / 180);
                    q.rotateY(y * (float) Math.PI / 180);
                    q.normalize();

                    pose.setLocalRotation(boneIndex, q.x, q.y, q.z, q.w);
                    boneIndex++;
                    bvhboneIndex++;
                    Log.d("BVH", "%s %f %f %f %f", bvhboneIndex, q.x, q.y, q.z, q.w);
                }
                else
                {
                    boneIndex++;
                }
            }
            frameIndex++;
        }
        return pose;
    }

    public GVRSkeletonAnimation readMotion(GVRSkeleton skel) throws IOException
    {
        int         numbones = skel.getNumBones();
        float       x, y, z;
        String      line;
        String      bonename = "";
        float       secondsPerFrame = 0;
        float       curTime = 0;
        float[]     rotKeys;
        float[]     posKeys;
        int         frameIndex = 0;

        ArrayList<float[]> rotKeysPerBone = new ArrayList<>(numbones);
        ArrayList<float[]> posKeysPerBone = new ArrayList<>(numbones);
        Quaternionf q = new Quaternionf();
        Quaternionf b = new Quaternionf();
        GVRPose bindpose = skel.getBindPose();

        /*
         * Parse and accumulate all the motion keyframes.
         * Keyframes for the root bone position are in rootPosKeys;
         * Keyframes for each bone's rotations are in rootKeysPerBone;
         */
        while ((line = mReader.readLine()) != null)
        {
            line = line.trim();
            String[]    words;
            if (line.contains("\t"))
            {
                words = line.split("\t");
            }
            else
            {
                words = line.split(" ");
            }
            if (line == "")
            {
                continue;
            }
            if (words[0].startsWith("Frames"))
            {
                for (int i = 0; i < numbones; i++)
                {
                    float[] r = new float[5 * Integer.parseInt(words[1])];
                    float[] p = new float[4 * Integer.parseInt(words[1])];
                    rotKeysPerBone.add(r);
                    posKeysPerBone.add(p);
                }
                continue;
            }
            if (words[0].startsWith("Frame "))
            {
                secondsPerFrame = Float.parseFloat(words[1]);
                continue;
            }
            /*
             * Parsing motion for each frame.
             * Each line in the file contains the root joint position and rotations for all joints.
             */
            int boneIndex = 0;
            int i = 0;
            while (i + 3 < words.length)
            {
                bonename = mBoneNames.get(boneIndex);
                if (bonename == null)
                {
                    throw new IOException("Cannot find bone " + bonename + " in skeleton");
                }
                if (mBoneChannels.get(boneIndex) > 3) {
                    posKeys = posKeysPerBone.get(boneIndex);

                    int f = frameIndex * 4;
                    x = Float.parseFloat(words[i]);    // Z, Y, X rotation angles
                    y = Float.parseFloat(words[i + 1]);
                    z = Float.parseFloat(words[i + 2]);
                    posKeys[f] = curTime;
                    posKeys[f + 1] = x;    // bone position
                    posKeys[f + 2] = y;
                    posKeys[f + 3] = z;
                    i += 3;
                }
                rotKeys = rotKeysPerBone.get(boneIndex);
                z = Float.parseFloat(words[i]);        // Z, Y, X rotation angles
                x = Float.parseFloat(words[i + 1]);
                y = Float.parseFloat(words[i + 2]);
                i += 3;
                q.rotationZ(z * (float) Math.PI / 180);
                q.rotateX(x * (float) Math.PI / 180);
                q.rotateY(y * (float) Math.PI / 180);
                q.normalize();
                bindpose.getLocalRotation(boneIndex, b);
                q.mul(b);
                int f = 5 * frameIndex;
                rotKeys[f++] = curTime;
                rotKeys[f++] = q.x;
                rotKeys[f++] = q.y;
                rotKeys[f++] = q.z;
                rotKeys[f] = q.w;
                boneIndex++;

                Log.d("BVH", "%s %f %f %f %f", bonename, q.x, q.y, q.z, q.w);
            }
            curTime += secondsPerFrame;
            frameIndex++;
        }
        /*
         * Create a skeleton animation with separate channels for each bone
         */

        GVRAnimationChannel channel;
        GVRSkeletonAnimation skelanim = new GVRSkeletonAnimation(mFileName, skel, curTime);
        for (int boneIndex = 0; boneIndex < mBoneNames.size(); ++boneIndex)
        {
            bonename = mBoneNames.get(boneIndex);
            rotKeys = rotKeysPerBone.get(boneIndex);
            posKeys = posKeysPerBone.get(boneIndex);
            if (mBoneChannels.get(boneIndex) > 3)
            {
                channel = new GVRAnimationChannel(bonename, posKeys, rotKeys, null,
                        GVRAnimationBehavior.DEFAULT, GVRAnimationBehavior.DEFAULT);
            }
            else
            {
               channel = new GVRAnimationChannel(bonename, null, rotKeys, null,
                        GVRAnimationBehavior.DEFAULT, GVRAnimationBehavior.DEFAULT);
            }
            skelanim.addChannel(bonename, channel);
        }
        return skelanim;
    }
}

