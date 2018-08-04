package org.gearvrf.animation.keyframe;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
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
import java.util.HashMap;
import java.util.Map;

public class TRSImporter
{
    private GVRSkeleton mSkeleton;
    private final GVRContext mContext;
    private String mFileName;

    public TRSImporter(GVRContext ctx)
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
        BufferedReader buffreader = new BufferedReader(inputreader);

        mSkeleton = skel;
        return readMotion(buffreader, skel);
    }

    public GVRSkeletonAnimation readMotion(BufferedReader buffreader, GVRSkeleton skel) throws IOException
    {
        int         numbones = skel.getNumBones();
        String      line;
        String 		bonename = "";
        float       secondsPerFrame = 1 / 30.0f;
        float       curTime = -secondsPerFrame;
        ArrayList<ArrayList<Float>> rotKeysPerBone = new ArrayList<>(numbones);
        ArrayList<ArrayList<Float>> posKeysPerBone = new ArrayList<>(numbones);
        ArrayList<ArrayList<Float>> sclKeysPerBone = new ArrayList<>(numbones);
        ArrayList<Float> posKeys;
        ArrayList<Float> rotKeys;
        ArrayList<Float> sclKeys;
        /*
         * Parse and accumulate all the motion keyframes.
         */
        while ((line = buffreader.readLine()) != null)
        {
            String[]    words = line.split(",");
            String      boneName;

            if (words.length < 11)
            {
                throw new IOException("Syntax error in TRS file");
            }
            boneName = words[0];
            int boneIndex = skel.getBoneIndex(boneName);
            if (boneIndex < 0)
            {
                Log.w("BONE","Bone " + boneName + " not found in skeleton");
                continue;
            }
            if (boneIndex == 0)
            {
                curTime += secondsPerFrame;
            }
            if (boneIndex < posKeysPerBone.size())
            {
                posKeys = posKeysPerBone.get(boneIndex);
            }
            else
            {
                posKeys = new ArrayList<Float>();
                posKeysPerBone.add(boneIndex, posKeys);
            }
            posKeys.add(curTime);
            posKeys.add(Float.parseFloat(words[1]));	// root bone position
            posKeys.add(Float.parseFloat(words[2]));
            posKeys.add(Float.parseFloat(words[3]));
            if (boneIndex < rotKeysPerBone.size())
            {
                rotKeys = rotKeysPerBone.get(boneIndex);
            }
            else
            {
                rotKeys = new ArrayList();
                rotKeysPerBone.add(boneIndex, rotKeys);
            }
            rotKeys.add(curTime);
            rotKeys.add(Float.parseFloat(words[4]));
            rotKeys.add(Float.parseFloat(words[5]));
            rotKeys.add(Float.parseFloat(words[6]));
            rotKeys.add(Float.parseFloat(words[7]));
            if (boneIndex < sclKeysPerBone.size())
            {
                sclKeys = sclKeysPerBone.get(boneIndex);
            }
            else
            {
                sclKeys = new ArrayList();
                sclKeysPerBone.add(boneIndex, sclKeys);
            }
            sclKeys.add(curTime);
            sclKeys.add(Float.parseFloat(words[8]));	// root bone position
            sclKeys.add(Float.parseFloat(words[9]));
            sclKeys.add(Float.parseFloat(words[10]));
        }
        /*
         * Create a skeleton animation with separate channels for each bone
         */
        GVRSkeletonAnimation skelanim = new GVRSkeletonAnimation(mFileName, skel, curTime);
        GVRAnimationChannel channel;
        for (int boneIndex = 0; boneIndex < numbones; ++boneIndex)
        {
            bonename = skel.getBoneName(boneIndex);
            rotKeys = rotKeysPerBone.get(boneIndex);
            posKeys = posKeysPerBone.get(boneIndex);
            sclKeys = posKeysPerBone.get(boneIndex);
            if ((rotKeys != null) && (posKeys != null) && (sclKeys != null))
            {
                float[] rotations = listToArray(rotKeys, 5);
                float[] positions = listToArray(posKeys, 4);
                float[] scales = listToArray(sclKeys, 4);
                channel = new GVRAnimationChannel(bonename, positions,
                        rotations, scales,
                        GVRAnimationBehavior.DEFAULT, GVRAnimationBehavior.DEFAULT);

                skelanim.addChannel(bonename, channel);
                Log.v("BONE", "%s positions\n%s", bonename, arrayToString(positions, 4));
                Log.v("BONE", "%s rotations\n%s", bonename, arrayToString(rotations, 5));
            }
        }
        return skelanim;
    }

    private String arrayToString(float[] arr, int keySize)
    {
        String s = "";
        for (int j = 0; j < 10 * keySize; j += keySize)
        {
            s += " ";
            for (int i = 1; i < keySize; ++i)
            {
                s += Float.toString(arr[j * keySize + i]) + ",";
            }
            s += "\n";
        }
        return s;
    }

    private float[] listToArray(ArrayList<Float> source, int keySize)
    {
        int n = source.size() / keySize;
        float[] arr = new float[source.size()];
        for (int i = 0; i < n; ++i)
        {
            arr[i] = source.get(i);
        }
        return arr;
    }
}
