package com.samsungxr.animation.keyframe;
import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRContext;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.keyframe.SXRAnimationBehavior;
import com.samsungxr.animation.keyframe.SXRAnimationChannel;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
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
    private SXRSkeleton mSkeleton;
    private final SXRContext mContext;
    private String mFileName;

    public TRSImporter(SXRContext ctx)
    {
        mContext = ctx;
    }

    public SXRSkeletonAnimation importAnimation(SXRAndroidResource res, SXRSkeleton skel) throws IOException
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

    public SXRSkeletonAnimation readMotion(BufferedReader buffreader, SXRSkeleton skel) throws IOException
    {
        int         numbones = skel.getNumBones();
        String      line;
        String 		bonename = "";
        float       secondsPerFrame = 1 / 30.0f;
        float       curTime = 0;
        SXRPose     curpose = null;
        Matrix4f mtx = new Matrix4f();
        Quaternionf q = new Quaternionf();
        ArrayList<SXRPose> posePerFrame = new ArrayList<>();

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
            float tx = Float.parseFloat(words[1]);
            float ty = Float.parseFloat(words[2]);
            float tz = Float.parseFloat(words[3]);
            float sx = Float.parseFloat(words[8]);
            float sy = Float.parseFloat(words[9]);
            float sz = Float.parseFloat(words[10]);
            q.x = Float.parseFloat(words[4]);
            q.y = Float.parseFloat(words[5]);
            q.z = Float.parseFloat(words[6]);
            q.w = Float.parseFloat(words[7]);
            q.normalize();
            if (boneIndex == 0)
            {
                curpose = new SXRPose(skel);
                posePerFrame.add(curpose);
                curTime =+ secondsPerFrame;
                mtx.translationRotateScale(tx, ty, tz, q.x, q.y, q.z, q.w, sx, sy, sz);
                curpose.setWorldMatrix(boneIndex, mtx);
            }
            else
            {
                int parentIndex = skel.getParentBoneIndex(boneIndex);
                curpose.getWorldMatrix(parentIndex, mtx);
                mtx.setTranslation(tx, ty, tz);
                mtx.rotate(q);
                mtx.scale(sx, sy, sz);
                curpose.setWorldMatrix(boneIndex, mtx);
            }
        }
        /*
         * Create a skeleton animation with separate channels for each bone
         */
        SXRSkeletonAnimation skelanim = new SXRSkeletonAnimation(mFileName, skel, curTime);
        SXRAnimationChannel channel;
        Vector3f v = new Vector3f();
        int numKeys = posePerFrame.size();

        curTime = 0;
        float[] positions = new float[numKeys * 4];
        float[] scales = new float[numKeys * 4];
        float[] rotations = new float[numKeys * 5];

        bonename = skel.getBoneName(0);
        for (SXRPose pose : posePerFrame)
        {
            int keyIndex = 0;
            int i = keyIndex * 4;

            pose.sync();
            pose.getLocalMatrix(0, mtx);
            mtx.getTranslation(v);
            positions[i] = curTime;
            positions[i + 1] = v.x;
            positions[i + 2] = v.y;
            positions[i + 3] = v.z;
            mtx.getScale(v);
            scales[i] = curTime;
            scales[i + 1] = v.x;
            scales[i + 2] = v.y;
            scales[i + 3] = v.z;
            i = keyIndex * 5;
            mtx.getUnnormalizedRotation(q);
            rotations[i] = curTime;
            rotations[i + 1] = q.x;
            rotations[i + 2] = q.y;
            rotations[i + 3] = q.z;
            rotations[i + 4] = q.w;
            keyIndex++;
            curTime += secondsPerFrame;
        }
        channel = new SXRAnimationChannel(bonename, positions, rotations, scales,
                                          SXRAnimationBehavior.DEFAULT, SXRAnimationBehavior.DEFAULT);
        skelanim.addChannel(bonename, channel);
        for (int boneIndex = 1; boneIndex < numbones; ++boneIndex)
        {
            rotations = new float[numKeys * 5];
            bonename = skel.getBoneName(boneIndex);
            curTime = 0;
            for (SXRPose pose : posePerFrame)
            {
                int keyIndex = 0;
                int i = keyIndex * 5;

                pose.getLocalRotation(boneIndex, q);
                rotations[i] = curTime;
                rotations[i + 1] = q.x;
                rotations[i + 2] = q.y;
                rotations[i + 3] = q.z;
                rotations[i + 4] = q.w;
                keyIndex++;
                curTime += secondsPerFrame;
            }
            channel = new SXRAnimationChannel(bonename, null, rotations, null,
                                              SXRAnimationBehavior.DEFAULT, SXRAnimationBehavior.DEFAULT);
            skelanim.addChannel(bonename, channel);
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
