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

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRTransform;
import com.samsungxr.PrettyPrint;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Component that animates a mesh based on a set of bones.
 * <p>
 * This class provides a component that implements skinning on the GPU.
 * It is based on a {@link SXRSkeleton} representing the bones of a skeleton.
 * The {@link SXRSkin} designates which bones of the skeleton animate a particular mesh.
 * Only these bones are sent to the GPU when the mesh is skinned.
 * Each mesh can use a different set of bones from the skeleton.
 * The skin should be attached to the node that owns the mesh.
 * <p>
 * When an asset containing a skinned mesh is loaded by the {@link com.samsungxr.SXRAssetLoader},
 * the {@link SXRSkeleton} and the {@link SXRSkin} components for each mesh
 * are automatically generated.
 * @see SXRSkeleton
 * @see SXRPose
 * @see com.samsungxr.animation.keyframe.SXRSkeletonAnimation
 */
public class SXRSkin extends SXRComponent implements PrettyPrint
{
    private static final String TAG = Log.tag(SXRSkin.class);
    protected SXRSkeleton mSkeleton;
    int mNumBones = 0;

    static public long getComponentType()
    {
        return NativeSkin.getComponentType();
    }

    /**
     * Create a skin based on the given skeleton.
     * @param skel {@link SXRSkeleton} whose bones drive this skin.
     */
    public SXRSkin(SXRSkeleton skel)
    {
        super(skel.getSXRContext(), NativeSkin.ctor(skel.getNative()));
        mType = getComponentType();
        mSkeleton = skel;
    }

    /**
     * Gets the number of bones used by this mesh.
     * <p>
     * The number of bones is established when the bone map is
     * set and cannot be changed without changing the bone map.
     * @see #setBoneMap(int[])
     */
    public int getNumBones()
    {
        return mNumBones;
    }

    /**
     * Set the bone map which indicates which bone in the {@SXRSkeleton}
     * drives which bone in the {@link com.samsungxr.SXRVertexBuffer}.
     * <p>
     * The vertex buffer has up to four bone indices and corresponding
     * bone weights for each vertex. The bone indices in the vertex
     * buffer do not need to correspond to the bone indices in the
     * {@link SXRSkeleton} that drives the mesh. The bone map
     * establishes the correspondence between them.
     * @param boneMap   index of skeleton bones for each mesh bones.
     */
    public void setBoneMap(int[] boneMap)
    {
        mNumBones = boneMap.length;
        NativeSkin.setBoneMap(getNative(), boneMap);
    }

    /**
     * Change the skeleton which contains the bones that
     * control this mesh.
     * <p>
     * The new skeleton must have corresponding bones for
     * the skin or an exception is thrown.
     * @param newSkel   new skeleton to use
     */
    public void setSkeleton(SXRSkeleton newSkel)
    {
        if (mSkeleton != newSkel)
        {
            mSkeleton = newSkel;
            NativeSkin.setSkeleton(getNative(), newSkel.getNative());
        }
    }

    public void setInverseBindPose(float[] matrices)
    {
        NativeSkin.setInverseBindPose(getNative(), matrices);
    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent)
    {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(System.lineSeparator());
        sb.append(Log.getSpaces(indent) + 2);
        sb.append("numBones = " + Integer.toString(getNumBones()));
        sb.append(System.lineSeparator());
    }
}

class NativeSkin
{
    static native long ctor(long skeleton);
    static native long getComponentType();
    static native boolean setBoneMap(long object, int[] boneMap);
    static native void setSkeleton(long object, long skel);
    static native boolean setInverseBindPose(long object, float[] matrices);
}
