package com.samsungxr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.lang.Math.max;

import com.samsungxr.animation.SXRAnimation;
import com.samsungxr.animation.SXRAnimator;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.animation.SXRSkin;
import com.samsungxr.animation.keyframe.SXRAnimationBehavior;
import com.samsungxr.animation.keyframe.SXRAnimationChannel;
import com.samsungxr.animation.keyframe.SXRNodeAnimation;
import com.samsungxr.animation.keyframe.SXRSkeletonAnimation;
import com.samsungxr.jassimp.AiAnimBehavior;
import com.samsungxr.jassimp.AiAnimMesh;
import com.samsungxr.jassimp.AiAnimation;
import com.samsungxr.jassimp.AiBone;
import com.samsungxr.jassimp.AiBoneWeight;
import com.samsungxr.jassimp.AiCamera;
import com.samsungxr.jassimp.AiColor;
import com.samsungxr.jassimp.AiLight;
import com.samsungxr.jassimp.AiLightType;
import com.samsungxr.jassimp.AiMaterial;
import com.samsungxr.jassimp.AiMesh;
import com.samsungxr.jassimp.AiNode;
import com.samsungxr.jassimp.AiNodeAnim;
import com.samsungxr.jassimp.AiPostProcessSteps;
import com.samsungxr.jassimp.AiScene;
import com.samsungxr.jassimp.AiTexture;
import com.samsungxr.jassimp.AiTextureMapMode;
import com.samsungxr.jassimp.AiTextureType;
import com.samsungxr.jassimp.SXRNewWrapperProvider;
import com.samsungxr.jassimp.Jassimp;
import com.samsungxr.jassimp.JassimpConfig;
import com.samsungxr.shaders.SXRPBRShader;
import com.samsungxr.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static java.lang.Integer.parseInt;

class  SXRJassimpAdapter
{
    private static final String TAG = SXRJassimpAdapter.class.getSimpleName();
    public static SXRNewWrapperProvider sWrapperProvider = new SXRNewWrapperProvider();
    private AiScene mScene;
    private SXRContext mContext;
    private String mFileName;
    private SXRSkeleton mSkeleton;

    private static final int MAX_TEX_COORDS = JassimpConfig.MAX_NUMBER_TEXCOORDS;
    private static final int MAX_VERTEX_COLORS = JassimpConfig.MAX_NUMBER_COLORSETS;

    /*
     * Maps the name of the SXRNode / AiNode to the SXRBone
     * attached to the SXRNode
     */
    private Hashtable<String, AiBone> mBoneMap = new Hashtable<>();

    /*
     * Maps SXRNode created for each Assimp node to the Assimp
     * mesh ID (the index of the mesh in AiScene).
     */
    private HashMap<SXRNode, Integer> mNodeMap = new HashMap<>();

    /**
     * Maps the Assimp mesh ID to the corresponding SXRMesh
     */
    private SXRMesh[] mMeshes;

    /**
     * Maps the Assimp material ID (index of the material in AiScene)
     *  to the corresponding SXRMaterial
     */
    private SXRMaterial[] mMaterials;


    public SXRJassimpAdapter(SXRAssetLoader loader, String filename)
    {
        mFileName = filename;
    }

    public SXRMesh createMesh(SXRContext ctx, AiMesh aiMesh, EnumSet<SXRImportSettings> settings)
    {
        String vertexDescriptor = "float3 a_position";
        float[] verticesArray = null;
        float[] tangentsArray = null;
        float[] bitangentsArray = null;
        float[] normalsArray = null;
        boolean doTexturing = !settings.contains(SXRImportSettings.NO_TEXTURING);
        boolean doLighting = !settings.contains(SXRImportSettings.NO_LIGHTING);

        // Vertices
        FloatBuffer verticesBuffer = aiMesh.getPositionBuffer();
        if (verticesBuffer != null)
        {
            verticesArray = new float[verticesBuffer.capacity()];
            verticesBuffer.get(verticesArray, 0, verticesBuffer.capacity());
        }
        // TexCoords
        if (doTexturing)
        {
            for (int texIndex = 0; texIndex < MAX_TEX_COORDS; texIndex++)
            {
                FloatBuffer fbuf = aiMesh.getTexCoordBuffer(texIndex);
                if (fbuf != null)
                {
                    vertexDescriptor += " float2 a_texcoord";
                    if (texIndex > 0)
                    {
                        vertexDescriptor += texIndex;
                    }
                }
            }
        }
        // Normals
        if (doLighting)
        {

            FloatBuffer normalsBuffer = aiMesh.getNormalBuffer();
            if (normalsBuffer != null)
            {
                vertexDescriptor += " float3 a_normal";

                normalsArray = new float[normalsBuffer.capacity()];
                normalsBuffer.get(normalsArray, 0, normalsBuffer.capacity());
            }

        }

        for (int c = 0; c < MAX_VERTEX_COLORS; c++)
        {
            FloatBuffer fbuf = aiMesh.getColorBuffer(c);
            if (fbuf != null)
            {
                String name = "a_color";

                if (c > 0)
                {
                    name += c;
                }
                vertexDescriptor += " float4 " + name;
            }
        }

        if (aiMesh.hasBones())
        {
            vertexDescriptor += " float4 a_bone_weights int4 a_bone_indices";
        }
        if (doLighting && aiMesh.hasTangentsAndBitangents())
        {

            vertexDescriptor += " float3 a_tangent float3 a_bitangent";

            FloatBuffer tangentBuffer = aiMesh.getTangentBuffer();

            tangentsArray = new float[tangentBuffer.capacity()];
            tangentBuffer.get(tangentsArray, 0, tangentBuffer.capacity());
            bitangentsArray = new float[tangentsArray.length];

            Vector3f tangent = new Vector3f();
            Vector3f normal = new Vector3f();
            Vector3f bitangent = new Vector3f();

            for (int i = 0; i < tangentsArray.length; i += 3)
            {
                tangent.set(tangentsArray[i], tangentsArray[i + 1], tangentsArray[i + 2]);
                normal.set(normalsArray[i], normalsArray[i + 1], normalsArray[i + 2]);
                normal.cross(tangent, bitangent);
                bitangentsArray[i] = bitangent.x; bitangentsArray[i+1] = bitangent.y; bitangentsArray[i + 2] = bitangent.z;
            }
        }

        SXRMesh mesh = new SXRMesh(ctx, vertexDescriptor);

        // Vertex Colors
        for (int c = 0; c < MAX_VERTEX_COLORS; c++)
        {
            FloatBuffer fbuf = aiMesh.getColorBuffer(c);
            if (fbuf != null)
            {
                FloatBuffer source = aiMesh.getColorBuffer(c);
                String name = "a_color";

                if (c > 0)
                {
                    name += c;
                }
                mesh.setFloatVec(name, source);
            }
        }

        IntBuffer indices = aiMesh.getIndexBuffer();
        int len = indices.capacity();
        SXRIndexBuffer indexBuffer = new SXRIndexBuffer(ctx, 4, len);

        indexBuffer.setIntVec(indices);
        mesh.setIndexBuffer(indexBuffer);

        if (verticesArray != null)
        {
            mesh.setVertices(verticesArray);
        }
        if (normalsArray != null)
        {
            mesh.setNormals(normalsArray);
        }
        if (tangentsArray != null)
        {
            mesh.setFloatArray("a_tangent", tangentsArray);
        }
        if (bitangentsArray != null)
        {
            mesh.setFloatArray("a_bitangent", bitangentsArray);
        }
        // TexCords
        if (doTexturing)
        {
            for (int texIndex = 0; texIndex < MAX_TEX_COORDS; texIndex++)
            {
                FloatBuffer fbuf = aiMesh.getTexCoordBuffer(texIndex);
                if (fbuf != null)
                {
                    FloatBuffer coords = FloatBuffer.allocate(aiMesh.getNumVertices() * 2);
                    if (aiMesh.getNumUVComponents(texIndex) == 2)
                    {
                        FloatBuffer coordsSource = aiMesh.getTexCoordBuffer(texIndex);
                        coords.put(coordsSource);
                    }
                    else
                    {
                        for (int i = 0; i < aiMesh.getNumVertices(); ++i)
                        {
                            float u = aiMesh.getTexCoordU(i, texIndex);
                            float v = aiMesh.getTexCoordV(i, texIndex);
                            coords.put(u);
                            coords.put(v);
                        }
                    }
                    mesh.setTexCoords(coords.array(), texIndex);
                }
            }
        }
        return mesh;
    }

    public void setMeshMorphComponent(SXRMesh mesh, SXRNode sceneObject, AiMesh aiMesh)
    {
        int nAnimationMeshes = aiMesh.getAnimationMeshes().size();
        if (nAnimationMeshes == 0)
            return;
        try
        {
            SXRMeshMorph morph = new SXRMeshMorph(mContext, nAnimationMeshes);
            sceneObject.attachComponent(morph);
            int blendShapeNum = 0;
            float[] weights = new float[nAnimationMeshes];

            for (AiAnimMesh animMesh : aiMesh.getAnimationMeshes())
            {
                SXRVertexBuffer animBuff = new SXRVertexBuffer(mesh.getVertexBuffer(),
                                                               "float3 a_position float3 a_normal float3 a_tangent float3 a_bitangent");
                float[] vertexArray = null;
                float[] normalArray = null;
                float[] tangentArray = null;
                float[] bitangentArray = null;

                weights[blendShapeNum] = animMesh.getDefaultWeight();
                //copy target positions to anim vertex buffer
                FloatBuffer animPositionBuffer = animMesh.getPositionBuffer();
                if (animPositionBuffer != null)
                {
                    vertexArray = new float[animPositionBuffer.capacity()];
                    animPositionBuffer.get(vertexArray, 0, animPositionBuffer.capacity());
                    animBuff.setFloatArray("a_position", vertexArray);
                }

                //copy target normals to anim normal buffer
                FloatBuffer animNormalBuffer = animMesh.getNormalBuffer();
                if (animNormalBuffer != null)
                {
                    normalArray = new float[animNormalBuffer.capacity()];
                    animNormalBuffer.get(normalArray, 0, animNormalBuffer.capacity());
                    animBuff.setFloatArray("a_normal", normalArray);
                }

                //copy target tangents to anim tangent buffer
                FloatBuffer animTangentBuffer = animMesh.getTangentBuffer();
                if (animTangentBuffer != null)
                {
                    tangentArray = new float[animTangentBuffer.capacity()];
                    animTangentBuffer.get(tangentArray, 0, animTangentBuffer.capacity());
                    animBuff.setFloatArray("a_tangent", tangentArray);
                    //calculate bitangents
                    bitangentArray = new float[tangentArray.length];
                    for (int i = 0; i < tangentArray.length; i += 3)
                    {
                        Vector3f tangent =
                            new Vector3f(tangentArray[i], tangentArray[i + 1], tangentArray[i + 2]);
                        Vector3f normal =
                            new Vector3f(normalArray[i], normalArray[i + 1], normalArray[i + 2]);
                        Vector3f bitangent = new Vector3f();
                        normal.cross(tangent, bitangent);
                        bitangentArray[i] = bitangent.x;
                        bitangentArray[i + 1] = bitangent.y;
                        bitangentArray[i + 2] = bitangent.z;
                    }
                    animBuff.setFloatArray("a_bitangent", bitangentArray);
                }
                morph.setBlendShape(blendShapeNum, animBuff);
                blendShapeNum++;
            }
            morph.setWeights(weights);
            morph.update();
        }
        catch (IllegalArgumentException ex)
        {
            sceneObject.detachComponent(SXRMeshMorph.getComponentType());
        }
    }

    public SXRSkin processBones(SXRMesh mesh, List<AiBone> aiBones)
    {
        final int MAX_WEIGHTS = 4;
        SXRVertexBuffer vbuf = mesh.getVertexBuffer();
        int nverts = vbuf.getVertexCount();
        int n = nverts * MAX_WEIGHTS;
        float[] weights = new float[n];
        int[] indices = new int[n];
        int[] boneMap = new int[aiBones.size()];
        int boneIndex = -1;
        SXRSkin skin = new SXRSkin(mSkeleton);

        // Process bones
        Arrays.fill(weights, 0, n - 1,  0.0f);
        Arrays.fill(indices, 0, n - 1, 0);

        /*
         * Accumulate vertex weights and indices for all the bones
         * in this mesh. All vertices have four indices and four weights.
         * If a vertex has less than four infuences, the weight is 0.
         */
        for (AiBone aiBone : aiBones)
        {
            String boneName = aiBone.getName();
            int boneId = mSkeleton.getBoneIndex(boneName);

            if (boneId < 0)
            {
                Log.e("BONE", "Bone %s not found in skeleton", boneName);
                continue;
            }
            Log.e("BONE", "%d %s -> %d", boneId, boneName, boneIndex + 1);
            boneMap[++boneIndex] = boneId;
            List<AiBoneWeight> boneWeights = aiBone.getBoneWeights();
            for (AiBoneWeight weight : boneWeights)
            {
                int vertexId = weight.getVertexId() * MAX_WEIGHTS;
                int i;
                for (i = 0; i < MAX_WEIGHTS; ++i)
                {
                    int j = vertexId + i;
                    if (weights[j] == 0.0f)
                    {
                        indices[j] = boneIndex;
                        weights[j] = weight.getWeight();
                        break;
                    }
                }
                if (i >= MAX_WEIGHTS)
                {
                    Log.w(TAG, "Vertex %d (total %d) has too many bones", vertexId, nverts);
                }
            }
        }
        skin.setBoneMap(boneMap);
        /*
         * Normalize the weights for each vertex.
         * Sum the weights and divide by the sum.
         */
        for (int v = 0; v < nverts; ++v)
        {
            float t = 0.0f;
            String is = v + " ";
            String ws = "";
            for (int i = 0; i < MAX_WEIGHTS; ++i)
            {
                int j = (v * MAX_WEIGHTS) + i;
                t += weights[j];
                //is += " " + indices[j];
                //ws += " " + weights[j];
            }
            //Log.v("BONES", is + ws);
            if (t > 0.000001f)
            {
                for (int i = 0; i < MAX_WEIGHTS; ++i)
                {
                    weights[(v * MAX_WEIGHTS) + i] /= t;
                }
            }
        }
        vbuf.setFloatArray("a_bone_weights", weights);
        vbuf.setIntArray("a_bone_indices", indices);
        return skin;
    }


    private class BoneCollector implements SXRNode.SceneVisitor
    {
        /**
         * List of node names in the order they are encountered
         * traversing the parents of the nodes before their children.
         * Only named nodes below the root which are animated and have
         * associated bones are in this list.
         */
        final List<String> mBoneNames = new ArrayList<>();
        private SXRNode mRoot = null;

        public BoneCollector()
        {
        }

        public List<String> getBoneNames() { return mBoneNames; }

        @Override
        public boolean visit(SXRNode obj)
        {
            String nodeName = obj.getName();

            if (!"".equals(nodeName))
            {
                AiBone aiBone = mBoneMap.get(nodeName);

                if (mBoneNames.contains(nodeName))    // duplicate bone ID
                {
                    Log.e("BONE", "Multiple bones with the same name: " + nodeName);
                    return true;
                }
                if (aiBone == null)
                {
                    SXRNode parent = obj.getParent();

                    if (obj.getChildrenCount() == 0)
                    {
                        Log.d("BONE", "Ignoring node %s with no children", nodeName);
                        return true;
                    }
                    if (parent == null)
                    {
                        Log.d("BONE", "Ignoring node %s with no parent", nodeName);
                        return true;
                    }
                    String parName = parent.getName();

                    if ("".equals(parName))
                    {
                        Log.d("BONE", "Ignoring node %s with unnamed parent", nodeName);
                        return true;
                    }
                    int parBoneId = mBoneNames.indexOf(parName);

                    if (parBoneId < 0)
                    {
                        Log.d("BONE", "Ignoring node %s with no parent bone");
                        return true;
                    }
                }
                int boneId = mBoneNames.size();
                mBoneNames.add(nodeName);
                if (mRoot == null)
                {
                    mRoot = obj;
                    Log.d("BONE", "Root bone %s id = %d", nodeName, boneId);
                }
                else if (mRoot.getParent() == obj.getParent())
                {
                    mRoot = mRoot.getParent();
                    mBoneNames.add(0, mRoot.getName());
                    Log.d("BONE", "Root bone %s id = %d", nodeName, mBoneNames.indexOf(mRoot.getName()));
                }
                Log.d("BONE", "Adding bone %s id = %d", nodeName, boneId);
            }
            return true;
        }
    };

    public void createAnimation(AiAnimation aiAnim, SXRNode target, SXRAnimator animator)
    {
        Map<String, SXRAnimationChannel> animMap = new HashMap<>();
        float duration =  (float) (aiAnim.getDuration() / aiAnim.getTicksPerSecond());

        for (AiNodeAnim aiNodeAnim : aiAnim.getChannels())
        {
            String nodeName = aiNodeAnim.getNodeName();
            SXRAnimationChannel channel = createAnimChannel(aiNodeAnim, (float) aiAnim.getTicksPerSecond());

            animMap.put(nodeName, channel);
        }
        if (mSkeleton != null)
        {
            SXRSkeletonAnimation anim = new SXRSkeletonAnimation(aiAnim.getName(), target, duration);

            anim.setSkeleton(mSkeleton, null);
            attachBoneAnimations(anim, animMap);
            animator.addAnimation(anim);
        }
        /*
         * Any animation channels that are not part of the skeleton
         * are added separately as node animations (which just modify
         * a single node's matrix)
         */
        for (AiNodeAnim aiNodeAnim : aiAnim.getChannels())
        {
            String nodeName = aiNodeAnim.getNodeName();
            SXRAnimationChannel channel = animMap.get(nodeName);

            if (channel == null)
            {
                continue;
            }
            SXRNode obj = target.getNodeByName(nodeName);
            if (obj != null)
            {
                SXRNodeAnimation nodeAnim = new SXRNodeAnimation(nodeName, obj, duration, channel);
                animator.addAnimation(nodeAnim);
                Log.d("BONE", "Adding node animation for %s", nodeName);
            }
        }
    }

    /*
     * if there was a skinned mesh the bone map acts as a lookup
     * table that maps bone names to their corresponding AiBone objects.
     * The BoneCollector constructs the skeleton from the bone names in
     * the map and the scene nodes, attempting to connect bones
     * where there are gaps to produce a complete skeleton.
     */
    private void makeSkeleton(SXRNode root)
    {
        if (!mBoneMap.isEmpty())
        {
            BoneCollector nodeProcessor = new BoneCollector();

            root.forAllDescendants(nodeProcessor);
            mSkeleton = new SXRSkeleton(root, nodeProcessor.getBoneNames());
            SXRPose bindPose = new SXRPose(mSkeleton);
            Matrix4f bindPoseMtx = new Matrix4f();
            SXRNode skelRoot = mSkeleton.getOwnerObject().getParent();
            Matrix4f rootMtx = skelRoot.getTransform().getModelMatrix4f();

            rootMtx.invert();
            for (int boneId = 0; boneId < mSkeleton.getNumBones(); ++boneId)
            {
                String boneName = mSkeleton.getBoneName(boneId);
                AiBone aiBone = mBoneMap.get(boneName);
                SXRNode bone = mSkeleton.getBone(boneId);

                if (aiBone != null)
                {
                    float[] matrixdata = aiBone.getOffsetMatrix(sWrapperProvider);

                    bindPoseMtx.set(matrixdata);
                    bindPoseMtx.invert();
                    bindPose.setWorldMatrix(boneId, bindPoseMtx);
                }
                else if (bone != null)
                {
                    SXRTransform t = bone.getTransform();
                    Matrix4f mtx = t.getModelMatrix4f();

                    mtx.invert();
                    rootMtx.mul(mtx, mtx);
                    bindPose.setWorldMatrix(boneId, mtx);
                    Log.w("BONE", "no bind pose matrix for bone %s", boneName);
                }
            }
            mSkeleton.setBindPose(bindPose);
        }
    }

    private void attachBoneAnimations(SXRSkeletonAnimation skelAnim, Map<String, SXRAnimationChannel> animMap)
    {
        SXRPose bindPose = mSkeleton.getBindPose();
        Matrix4f bindPoseMtx = new Matrix4f();
        Vector3f vec = new Vector3f();
        final float EPSILON = 0.00001f;
        String boneName = mSkeleton.getBoneName(0);
        SXRAnimationChannel channel = animMap.get(boneName);
        AiBone aiBone;

        bindPose.getWorldMatrix(0, bindPoseMtx);
        bindPoseMtx.getScale(vec);
        if (channel != null)
        {
            skelAnim.addChannel(boneName, channel);
            animMap.remove(boneName);
            /*
             * This is required because of a bug in the FBX importer
             * which does not scale the animations to match the bind pose
             */
            bindPoseMtx.getScale(vec);
            float delta = vec.lengthSquared();
            delta = 3.0f - delta;
            if (Math.abs(delta) > EPSILON)
            {
                fixKeys(channel, vec);
            }
        }
        for (int boneId = 1; boneId < mSkeleton.getNumBones(); ++boneId)
        {
            boneName = mSkeleton.getBoneName(boneId);
            aiBone = mBoneMap.get(boneName);
            channel = animMap.get(boneName);

            if (channel != null)
            {
                skelAnim.addChannel(boneName, channel);
                animMap.remove(boneName);
                if (aiBone == null)
                {
                    Log.e("BONE", "no bind pose matrix for bone %s", boneName);
                }
            }
        }
    }

    /*
     * Some FBX files are exported as centimeters. Assimp does not correctly compute the scale keys.
     * They should include the scaling from the bind pose since the animations are NOT relative
     * to the bind pose.
     */
    private void fixKeys(SXRAnimationChannel channel, Vector3f scaleFactor)
    {
        float[] temp = new float[3];
        for (int i = 0; i < channel.getNumPosKeys(); ++i)
        {
            float time = (float) channel.getPosKeyTime(i);
            channel.getPosKeyVector(i, temp);
            temp[0] *= scaleFactor.x;
            temp[1] *= scaleFactor.y;
            temp[2] *= scaleFactor.z;
            channel.setPosKeyVector(i, time, temp);
        }
        for (int i = 0; i < channel.getNumScaleKeys(); ++i)
        {
            float time = (float) channel.getScaleKeyTime(i);
            channel.getScaleKeyVector(i, temp);
            temp[0] *= scaleFactor.x;
            temp[1] *= scaleFactor.y;
            temp[2] *= scaleFactor.z;
            channel.setScaleKeyVector(i, time, temp);
        }
    }

    private SXRAnimationChannel createAnimChannel(AiNodeAnim aiNodeAnim, float ticksPerSec)
    {
        SXRAnimationChannel channel = new SXRAnimationChannel(aiNodeAnim.getNodeName(), aiNodeAnim.getNumPosKeys(),
                aiNodeAnim.getNumRotKeys(),  aiNodeAnim.getNumScaleKeys(),
                convertAnimationBehavior(aiNodeAnim.getPreState()),
                convertAnimationBehavior(aiNodeAnim.getPostState()));
        // Pos keys
        int i;
        float t;

        if (aiNodeAnim.getNumPosKeys() > 0)
        {
            float[] curpos = aiNodeAnim.getPosKeyVector(0, sWrapperProvider);
            int nextIndex = 1;

            t = (float) aiNodeAnim.getPosKeyTime(0) / ticksPerSec;
            channel.setPosKeyVector(0, t, curpos);
            for (i = 1; i < aiNodeAnim.getNumPosKeys(); ++i)
            {
                float[] pos = aiNodeAnim.getPosKeyVector(i, sWrapperProvider);
                if (!isEqual(pos, curpos))
                {
                    t = (float) aiNodeAnim.getPosKeyTime(i) / ticksPerSec;
                    channel.setPosKeyVector(nextIndex++, t, pos);
                    curpos = pos;
                }
            }
            channel.resizePosKeys(nextIndex);
        }

        if (aiNodeAnim.getNumRotKeys() > 0)
        {
            Quaternionf currot = aiNodeAnim.getRotKeyQuaternion(0, sWrapperProvider);
            int nextIndex = 1;

            t = (float) aiNodeAnim.getRotKeyTime(0) / ticksPerSec;
            channel.setRotKeyQuaternion(0, t, currot);
            for (i = 1; i < aiNodeAnim.getNumRotKeys(); ++i)
            {
                Quaternionf rot = aiNodeAnim.getRotKeyQuaternion(i, sWrapperProvider);
                if (!isEqual(rot, currot))
                {
                    t = (float) aiNodeAnim.getRotKeyTime(i) / ticksPerSec;
                    channel.setRotKeyQuaternion(nextIndex++, t, rot);
                    currot = rot;
                }
            }
            channel.resizeRotKeys(nextIndex);
        }

        if (aiNodeAnim.getNumScaleKeys() > 0)
        {
            int nextIndex = 1;
            float[] curscale = aiNodeAnim.getScaleKeyVector(0, sWrapperProvider);

            t = (float) aiNodeAnim.getScaleKeyTime(0) / ticksPerSec;
            channel.setScaleKeyVector(0, t, curscale);
            for (i = 1; i < aiNodeAnim.getNumScaleKeys(); ++i)
            {
                float[] scale = aiNodeAnim.getScaleKeyVector(i, sWrapperProvider);

                if (!isEqual(scale, curscale))
                {
                    t = (float) aiNodeAnim.getScaleKeyTime(i) / ticksPerSec;
                    channel.setScaleKeyVector(nextIndex++, t, scale);
                }
            }
            channel.resizeScaleKeys(nextIndex);
        }
        return channel;
    }

    private boolean isEqual(float[] arr1, float[] arr2)
    {
        final float EPSILON = 0.0001f;

        for (int i = 0; i < arr1.length; ++i)
        {
            if (Math.abs(arr2[i] - arr1[i]) > EPSILON)
            {
                return false;
            }
        }
        return true;
    }

    private boolean isEqual(Quaternionf q1, Quaternionf q2)
    {
        final float EPSILON = 0.00001f;

        if (Math.abs(q1.x - q2.x) > EPSILON) return false;
        if (Math.abs(q1.y - q2.y) > EPSILON) return false;
        if (Math.abs(q1.z - q2.z) > EPSILON) return false;
        if (Math.abs(q1.w - q2.w) > EPSILON) return false;
        return true;
    }

    private SXRAnimationBehavior convertAnimationBehavior(AiAnimBehavior behavior)
    {
        switch (behavior)
        {
            case DEFAULT:
                return SXRAnimationBehavior.DEFAULT;
            case CONSTANT:
                return SXRAnimationBehavior.CONSTANT;
            case LINEAR:
                return SXRAnimationBehavior.LINEAR;
            case REPEAT:
                return SXRAnimationBehavior.REPEAT;
            default:
                // Unsupported setting
                Log.e(TAG, "Cannot convert animation behavior: %s", behavior);
                return SXRAnimationBehavior.DEFAULT;
        }
    }

    public Set<AiPostProcessSteps> toJassimpSettings(EnumSet<SXRImportSettings> settings) {
        Set<AiPostProcessSteps> output = new HashSet<AiPostProcessSteps>();

        for (SXRImportSettings setting : settings) {
            AiPostProcessSteps aiSetting = fromSXRSetting(setting);
            if (aiSetting != null) {
                output.add(aiSetting);
            }
        }

        return output;
    }

    public AiPostProcessSteps fromSXRSetting(SXRImportSettings setting) {
        switch (setting) {
            case CALCULATE_TANGENTS:
                return AiPostProcessSteps.CALC_TANGENT_SPACE;
            case JOIN_IDENTICAL_VERTICES:
                return AiPostProcessSteps.JOIN_IDENTICAL_VERTICES;
            case TRIANGULATE:
                return AiPostProcessSteps.TRIANGULATE;
            case CALCULATE_NORMALS:
                return AiPostProcessSteps.GEN_NORMALS;
            case CALCULATE_SMOOTH_NORMALS:
                return AiPostProcessSteps.GEN_SMOOTH_NORMALS;
            case LIMIT_BONE_WEIGHT:
                return AiPostProcessSteps.LIMIT_BONE_WEIGHTS;
            case IMPROVE_VERTEX_CACHE_LOCALITY:
                return AiPostProcessSteps.IMPROVE_CACHE_LOCALITY;
            case SORTBY_PRIMITIVE_TYPE:
                return AiPostProcessSteps.SORT_BY_PTYPE;
            case OPTIMIZE_MESHES:
                return AiPostProcessSteps.OPTIMIZE_MESHES;
            case OPTIMIZE_GRAPH:
                return AiPostProcessSteps.OPTIMIZE_GRAPH;
            case FLIP_UV:
                return AiPostProcessSteps.FLIP_UVS;
            case START_ANIMATIONS:
                return null;
            case NO_ANIMATION:
            case NO_LIGHTING:
            case NO_TEXTURING:
                return null;
            default:
                // Unsupported setting
                Log.e(TAG, "Unsupported setting %s", setting);
                return null;
        }
    }

    public void processScene(SXRAssetLoader.AssetRequest request, final SXRNode model, AiScene scene)
    {
        Hashtable<String, SXRLight> lightList = new Hashtable<String, SXRLight>();
        EnumSet<SXRImportSettings> settings = request.getImportSettings();
        boolean doAnimation = !settings.contains(SXRImportSettings.NO_ANIMATION);
        SXRNode modelParent = model.getParent();

        if (modelParent != null)
        {
            modelParent.removeChildObject(model);
        }
        mScene = scene;
        mContext = model.getSXRContext();
        if (scene == null)
        {
            return;
        }
        SXRNode camera = makeCamera();
        if (camera != null)
        {
            model.addChildObject(camera);
        }
        if (!settings.contains(SXRImportSettings.NO_LIGHTING))
        {
            importLights(scene.getLights(), lightList);
        }
        mMeshes = new SXRMesh[scene.getNumMeshes()];
        mMaterials = new SXRMaterial[scene.getNumMaterials()];

        traverseGraph(model, scene.getSceneRoot(sWrapperProvider), lightList);
        makeSkeleton(model);
        if (doAnimation)
        {
            processAnimations(model, scene, settings.contains(SXRImportSettings.START_ANIMATIONS));
        }
        for (Map.Entry<SXRNode, Integer> entry : mNodeMap.entrySet())
        {
            SXRNode obj = entry.getKey();
            int meshId = entry.getValue();

            if (meshId >= 0)
            {
                processMesh(request, obj, meshId);
            }
        }
        if (modelParent != null)
        {
            modelParent.addChildObject(model);
        }
    }

    private SXRAnimator processAnimations(SXRNode model, AiScene scene, boolean startAnimations)
    {
        List<AiAnimation> animations = scene.getAnimations();
        if (animations.size() > 0)
        {
            SXRAnimator animator = new SXRAnimator(mContext, startAnimations);
            model.attachComponent(animator);
            for (AiAnimation aiAnim : scene.getAnimations())
            {
                createAnimation(aiAnim, model, animator);
            }
            return animator;
        }
        return null;
    }

    private SXRNode makeCamera()
    {
        List<AiCamera> cameras = mScene.getCameras();
        if (cameras.size() == 0)
        {
            return null;
        }
        SXRNode mainCamera = new SXRNode(mContext);
        SXRCameraRig cameraRig = SXRCameraRig.makeInstance(mContext);
        AiCamera aiCam = cameras.get(0);
        float[] up = (float[]) aiCam.getUp(Jassimp.BUILTIN);
        float[] fwd = (float[]) aiCam.getLookAt(Jassimp.BUILTIN);
        float[] pos = (float[]) aiCam.getPosition(Jassimp.BUILTIN);
        Matrix4f mtx = new Matrix4f();

        mtx.setLookAt(pos[0], pos[1], pos[2],
                pos[0] + fwd[0], pos[1] + fwd[1], pos[2] + fwd[2],
                up[0], up[1], up[2]);
        mainCamera.setName("MainCamera");
        mainCamera.getTransform().setModelMatrix(mtx);
        cameraRig.setNearClippingDistance(aiCam.getClipPlaneNear());
        cameraRig.setFarClippingDistance(aiCam.getClipPlaneFar());
        mainCamera.attachComponent(cameraRig);
        return mainCamera;
    }

    private void traverseGraph(SXRNode parent, AiNode node, Hashtable<String, SXRLight> lightlist)
    {
        SXRNode sceneObject = new SXRNode(mContext);
        final int[] nodeMeshes = node.getMeshes();
        String nodeName = node.getName();
        AiNode aiChild = null;

        mNodeMap.put(sceneObject, -1);
        sceneObject.setName(nodeName);
        attachLights(lightlist, sceneObject);
        parent.addChildObject(sceneObject);
        if (node.getTransform(sWrapperProvider) != null)
        {
            float[] matrix = node.getTransform(sWrapperProvider);
            sceneObject.getTransform().setModelMatrix(matrix);
        }

        if (node.getNumMeshes() == 1)
        {
            Integer meshId = nodeMeshes[0];
            if ("".equals(nodeName))
            {
                if ((mNodeMap.get(parent) == null) ||
                    ((aiChild = handleNoName(node, sceneObject, lightlist)) == null))
                {
                    nodeName = "mesh";
                    sceneObject.setName(nodeName + "-" + meshId);
                }
                else
                {
                    node = aiChild;
                }
            }
            mNodeMap.put(sceneObject, meshId);
            findBones(mScene.getMeshes().get(meshId));
        }
        else if (node.getNumMeshes() > 1)
        {
            for (Integer i = 0; i < node.getNumMeshes(); i++)
            {
                int meshId = nodeMeshes[i];
                SXRNode child = new SXRNode(mContext);
                child.setName(nodeName + "-" + meshId);
                sceneObject.addChildObject(child);
                mNodeMap.put(child, meshId);
                findBones(mScene.getMeshes().get(meshId));
            }
        }
        else if ("".equals(nodeName) &&
                ((aiChild = handleNoName(node, sceneObject, lightlist)) != null))
        {
            node = aiChild;
        }
        for (AiNode child : node.getChildren())
        {
            traverseGraph(sceneObject, child, lightlist);
        }
    }

    private AiNode handleNoName(AiNode ainode, SXRNode gvrnode, Hashtable<String, SXRLight> lightlist)
    {
        if (ainode.getNumChildren() > 1)
        {
            return null;
        }
        AiNode aichild = ainode.getChildren().get(0);
        String childName = aichild.getName();

        if ("".equals(childName))
        {
            return null;
        }
        if (aichild.getNumMeshes() > 0)
        {
            return null;
        }
        if (lightlist.containsKey(childName))
        {
            return null;
        }
        gvrnode.setName(childName);
        float[] matrix = aichild.getTransform(sWrapperProvider);
        Matrix4f childMtx = new Matrix4f();
        Matrix4f parMtx = gvrnode.getTransform().getLocalModelMatrix4f();
        childMtx.set(matrix);
        parMtx.mul(childMtx);
        gvrnode.getTransform().setModelMatrix(parMtx);
        return aichild;
    }

    private void findBones(AiMesh aiMesh)
    {
        for (AiBone aiBone : aiMesh.getBones())
        {
            String boneName = aiBone.getName();

            if (mBoneMap.get(boneName) == null)
            {
                mBoneMap.put(aiBone.getName(), aiBone);
                Log.e("BONE", "Adding bone %s", boneName);
            }
        }
    }

    private void attachLights(Hashtable<String, SXRLight> lightlist, SXRNode sceneObject)
    {
        String name = sceneObject.getName();
        if ("".equals(name))
        {
            return;
        }
        SXRLight light =  lightlist.get(name);
        if (light != null)
        {
            Quaternionf q = new Quaternionf();
            q.rotationX((float) -Math.PI / 2.0f);
            q.normalize();
            light.setDefaultOrientation(q);
            sceneObject.attachLight(light);
            lightlist.remove(light);
        }
    }

    /**
     * Helper method to create a new {@link SXRNode} with a given mesh
     *
     * @param assetRequest
     *            SXRAssetRequest containing the original request to load the model
     *
     * @param sceneObject
     *            The SXRNode to process
     *
     * @param meshId
     *            The index of the assimp mesh in the AiScene mesh list
     */
    private void processMesh(
            SXRAssetLoader.AssetRequest assetRequest,
            SXRNode sceneObject,
            int meshId)
    {
        EnumSet<SXRImportSettings> settings = assetRequest.getImportSettings();
        AiMesh aiMesh = mScene.getMeshes().get(meshId);
        SXRMesh mesh = mMeshes[meshId];
        SXRMaterial gvrMaterial = mMaterials[aiMesh.getMaterialIndex()];

        if (mesh == null)
        {
            mesh = createMesh(mContext, aiMesh, settings);
            mMeshes[meshId] = mesh;
            if (aiMesh.hasBones() && (mSkeleton != null))
            {
                SXRSkin skin = processBones(mesh, aiMesh.getBones());
                if (skin != null)
                {
                    sceneObject.attachComponent(skin);
                }
            }
        }
        else
        {
            Log.v("BONE", "instancing mesh %s", sceneObject.getName());
        }
        if (gvrMaterial == null)
        {
            AiMaterial material = mScene.getMaterials().get(aiMesh.getMaterialIndex());
            gvrMaterial = processMaterial(assetRequest, material, aiMesh);
            mMaterials[aiMesh.getMaterialIndex()] = gvrMaterial;
        }
        SXRRenderData renderData = new SXRRenderData(mContext, gvrMaterial);

        renderData.setMesh(mesh);
        if (settings.contains(SXRImportSettings.NO_LIGHTING))
        {
            renderData.disableLight();
        }
        sceneObject.attachRenderData(renderData);
        setMeshMorphComponent(mesh, sceneObject, aiMesh);
    }

    private static final Map<AiTextureType, String> textureMap;
    static
    {
        textureMap = new HashMap<AiTextureType, String>();
        textureMap.put(AiTextureType.DIFFUSE,"diffuse");
        textureMap.put(AiTextureType.SPECULAR,"specular");
        textureMap.put(AiTextureType.AMBIENT,"ambient");
        textureMap.put(AiTextureType.EMISSIVE,"emissive");
        textureMap.put(AiTextureType.HEIGHT,"height");
        textureMap.put(AiTextureType.NORMALS,"normal");
        textureMap.put(AiTextureType.SHININESS,"shininess");
        textureMap.put(AiTextureType.OPACITY,"opacity");
        textureMap.put(AiTextureType.DISPLACEMENT,"displacement");
        textureMap.put(AiTextureType.LIGHTMAP,"lightmap");
        textureMap.put(AiTextureType.REFLECTION,"reflection");
        textureMap.put(AiTextureType.UNKNOWN, "metallicRoughness");
    }

    private static final Map<AiTextureMapMode, SXRTextureParameters.TextureWrapType> wrapModeMap;
    static
    {
        wrapModeMap = new HashMap<AiTextureMapMode, SXRTextureParameters.TextureWrapType>();
        wrapModeMap.put(AiTextureMapMode.WRAP, SXRTextureParameters.TextureWrapType.GL_REPEAT );
        wrapModeMap.put(AiTextureMapMode.CLAMP, SXRTextureParameters.TextureWrapType.GL_CLAMP_TO_EDGE );
        wrapModeMap.put(AiTextureMapMode.MIRROR, SXRTextureParameters.TextureWrapType.GL_MIRRORED_REPEAT );
        wrapModeMap.put(AiTextureMapMode.GL_REPEAT, SXRTextureParameters.TextureWrapType.GL_REPEAT );
        wrapModeMap.put(AiTextureMapMode.GL_CLAMP, SXRTextureParameters.TextureWrapType.GL_CLAMP_TO_EDGE );
        wrapModeMap.put(AiTextureMapMode.GL_MIRRORED_REPEAT, SXRTextureParameters.TextureWrapType.GL_MIRRORED_REPEAT );
    }

    private static final Map<Integer, SXRTextureParameters.TextureFilterType> filterMap;
    static
    {
        filterMap = new HashMap<Integer, SXRTextureParameters.TextureFilterType>();
        filterMap.put(GLES20.GL_LINEAR, SXRTextureParameters.TextureFilterType.GL_LINEAR);
        filterMap.put(GLES20.GL_NEAREST, SXRTextureParameters.TextureFilterType.GL_NEAREST);
        filterMap.put(GLES20.GL_NEAREST_MIPMAP_NEAREST, SXRTextureParameters.TextureFilterType.GL_NEAREST_MIPMAP_NEAREST);
        filterMap.put(GLES20.GL_NEAREST_MIPMAP_LINEAR, SXRTextureParameters.TextureFilterType.GL_NEAREST_MIPMAP_LINEAR);
        filterMap.put(GLES20.GL_LINEAR_MIPMAP_NEAREST, SXRTextureParameters.TextureFilterType.GL_LINEAR_MIPMAP_NEAREST);
        filterMap.put(GLES20.GL_LINEAR_MIPMAP_LINEAR, SXRTextureParameters.TextureFilterType.GL_LINEAR_MIPMAP_LINEAR);
    }

    private SXRMaterial processMaterial(
            SXRAssetLoader.AssetRequest assetRequest,
            AiMaterial aiMaterial,
            AiMesh aiMesh)
    {
        EnumSet<SXRImportSettings> settings = assetRequest.getImportSettings();
        SXRMaterial gvrMaterial = createMaterial(aiMaterial, settings);
        AiColor diffuseColor = aiMaterial.getDiffuseColor(sWrapperProvider);
        float opacity = diffuseColor.getAlpha();

        if (!settings.contains(SXRImportSettings.NO_TEXTURING))
        {
            loadTextures(assetRequest, aiMaterial, gvrMaterial, aiMesh);
        }
        if (settings.contains(SXRImportSettings.NO_LIGHTING))
        {
            if (aiMaterial.getOpacity() > 0)
            {
                opacity *= aiMaterial.getOpacity();
            }
            gvrMaterial.setVec3("u_color",
                    diffuseColor.getRed(),
                    diffuseColor.getGreen(),
                    diffuseColor.getBlue());
            gvrMaterial.setFloat("u_opacity", opacity);
        }
        else
        {
            /* Diffuse color & Opacity */
            if (aiMaterial.getOpacity() > 0)
            {
                opacity *= aiMaterial.getOpacity();
            }
            gvrMaterial.setVec4("diffuse_color",diffuseColor.getRed(),
                    diffuseColor.getGreen(), diffuseColor.getBlue(), opacity);

            /* Specular color */
            AiColor specularColor = aiMaterial.getSpecularColor(sWrapperProvider);
            gvrMaterial.setSpecularColor(specularColor.getRed(),
                    specularColor.getGreen(), specularColor.getBlue(),
                    specularColor.getAlpha());


            /* Ambient color */
            AiColor ambientColor = aiMaterial.getAmbientColor(sWrapperProvider);
            if (gvrMaterial.hasUniform("ambient_color"))
            {
                gvrMaterial.setAmbientColor(ambientColor.getRed(),
                        ambientColor.getGreen(), ambientColor.getBlue(),
                        ambientColor.getAlpha());
            }


            /* Emissive color */
            AiColor emissiveColor = aiMaterial.getEmissiveColor(sWrapperProvider);
            gvrMaterial.setVec4("emissive_color", emissiveColor.getRed(),
                    emissiveColor.getGreen(), emissiveColor.getBlue(),
                    emissiveColor.getAlpha());
        }

        /* Specular Exponent */
        float specularExponent = aiMaterial.getShininess();
        gvrMaterial.setSpecularExponent(specularExponent);
        return gvrMaterial;
    }

    private SXRMaterial createMaterial(AiMaterial material, EnumSet<SXRImportSettings> settings)
    {
        boolean layered = false;
        SXRShaderId shaderType;

        for (final AiTextureType texType : AiTextureType.values())
        {
            if (texType != AiTextureType.UNKNOWN)
            {
                if (material.getNumTextures(texType) > 1)
                {
                    layered = true;
                }
            }
        }
        if (!settings.contains(SXRImportSettings.NO_LIGHTING))
        {
            try
            {
                boolean glosspresent = material.getSpecularGlossinessUsage();
                shaderType = new SXRShaderId(SXRPBRShader.class);
                SXRMaterial m = new SXRMaterial(mContext, shaderType);

                //use specular glossiness workflow, if present
                if (glosspresent)
                {
                    AiColor diffuseFactor = material.getDiffuseColor(sWrapperProvider);
                    AiColor specularFactor = material.getSpecularColor(sWrapperProvider);
                    //gltf2importer.cpp in the assimp lib defines shininess as glossiness_factor * 1000.0f
                    float glossinessFactor = material.getShininess() / 1000.0f;

                    m.setDiffuseColor(diffuseFactor.getRed(), diffuseFactor.getGreen(), diffuseFactor.getBlue(), diffuseFactor.getAlpha());
                    m.setSpecularColor(specularFactor.getRed(), specularFactor.getGreen(), specularFactor.getBlue(), specularFactor.getAlpha());
                    m.setFloat("glossinessFactor", glossinessFactor);
                }
                else
                {
                    float metallic = material.getMetallic();
                    float roughness = material.getRoughness();
                    AiColor baseColorFactor = material.getDiffuseColor(sWrapperProvider);

                    m.setFloat("roughness", roughness);
                    m.setFloat("metallic", metallic);
                    m.setDiffuseColor(baseColorFactor.getRed(), baseColorFactor.getGreen(), baseColorFactor.getBlue(), baseColorFactor.getAlpha());
                }

                Bitmap bitmap = BitmapFactory.decodeResource(
                        mContext.getContext().getResources(), R.drawable.brdflookup);
                SXRTexture brdfLUTtex = new SXRTexture(mContext);
                brdfLUTtex.setImage(new SXRBitmapImage(mContext, bitmap));
                m.setTexture("brdfLUTTexture", brdfLUTtex);
                return m;
            }
            catch (IllegalArgumentException e)
            {
                shaderType = SXRMaterial.SXRShaderType.Phong.ID;
            }
            if (layered)
            {
                shaderType = SXRMaterial.SXRShaderType.PhongLayered.ID;
            }
        }
        else
        {
            shaderType = SXRMaterial.SXRShaderType.Texture.ID;
        }
        return new SXRMaterial(mContext, shaderType);
    }

    private void loadTexture(SXRAssetLoader.AssetRequest assetRequest,
                             final AiMaterial aimtl, final SXRMaterial gvrmtl,
                             final AiTextureType texType, int texIndex,
                             int uvIndex)
    {
        int blendop = aimtl.getTextureOp(texType, texIndex).ordinal();
        String typeName = textureMap.get(texType);
        String textureKey = typeName + "Texture";
        String texCoordKey = "a_texcoord";
        String shaderKey = typeName + "_coord";
        final String texFileName = aimtl.getTextureFile(texType, texIndex);
        final boolean usingPBR = (gvrmtl.getShaderType() == mContext.getShaderManager().getShaderType(SXRPBRShader.class));

        if (uvIndex > 0)
        {
            texCoordKey += uvIndex;
        }
        if (texIndex > 1)
        {
            assetRequest.onModelError(mContext, "Layering only supported for two textures, ignoring " + texFileName, mFileName);
            return;
        }
        if (texIndex > 0)
        {
            if (usingPBR)
            {
                return;
            }
            textureKey += texIndex;
            shaderKey += texIndex;
            gvrmtl.setInt(textureKey + "_blendop", blendop);
        }
        SXRTextureParameters texParams = new SXRTextureParameters(mContext);
        texParams.setWrapSType(wrapModeMap.get(aimtl.getTextureMapModeU(texType, texIndex)));
        texParams.setWrapTType(wrapModeMap.get(aimtl.getTextureMapModeV(texType, texIndex)));
        texParams.setMinFilterType(filterMap.get(aimtl.getTextureMinFilter(texType, texIndex)));
        texParams.setMagFilterType(filterMap.get(aimtl.getTextureMagFilter(texType, texIndex)));

        SXRTexture gvrTex = new SXRTexture(mContext, texParams);
        SXRAssetLoader.TextureRequest texRequest;

        gvrTex.setTexCoord(texCoordKey, shaderKey);
        gvrmtl.setTexture(textureKey, gvrTex);
        if (!usingPBR && typeName.equals("lightmap"))
        {
            gvrmtl.setVec2("u_lightmap_scale", 1, 1);
            gvrmtl.setVec2("u_lightmap_offset", 0, 0);
        }

        if (texFileName.startsWith("*"))
        {
            AiTexture tex = null;
            try
            {
                int embeddedIndex = parseInt(texFileName.substring(1));
                tex = mScene.getTextures().get(embeddedIndex);
                texRequest = new SXRAssetLoader.TextureRequest(assetRequest, gvrTex, mFileName + texFileName);
                assetRequest.loadEmbeddedTexture(texRequest, tex);
            }
            catch (NumberFormatException | IndexOutOfBoundsException ex)
            {
                assetRequest.onModelError(mContext, ex.getMessage(), mFileName);
            }
            catch (IOException ex2)
            {
                assetRequest.onTextureError(gvrTex, mFileName, ex2.getMessage());
            }
        }
        else
        {
            texRequest = new SXRAssetLoader.TextureRequest(assetRequest, gvrTex, texFileName);
            assetRequest.loadTexture(texRequest);
        }
    }

    private void loadTextures(SXRAssetLoader.AssetRequest assetRequest, AiMaterial aimtl, final SXRMaterial gvrmtl, final AiMesh aimesh)
    {
        for (final AiTextureType texType : AiTextureType.values())
        {
            for (int i = 0; i < aimtl.getNumTextures(texType); ++i)
            {
                final String texFileName = aimtl.getTextureFile(texType, i);

                if (!"".equals(texFileName))
                {
                    int uvIndex = aimtl.getTextureUVIndex(texType, i);
                    if (!aimesh.hasTexCoords(uvIndex))
                    {
                        uvIndex = 0;
                    }
                    loadTexture(assetRequest, aimtl, gvrmtl, texType, i, uvIndex);
                }
            }
        }
    }

    private void importLights(List<AiLight> lights, Hashtable<String, SXRLight> lightlist)
    {
        for (AiLight light: lights)
        {
            SXRLight l;
            AiLightType type = light.getType();
            String name = light.getName();

            if (type == AiLightType.DIRECTIONAL)
            {
                l = new SXRDirectLight(mContext);
            }
            else if (type == AiLightType.POINT)
            {
                l = new SXRPointLight(mContext);
            }
            else if (type == AiLightType.SPOT)
            {
                float outerAngleRadians = light.getAngleOuterCone();
                float innerAngleRadians = light.getAngleInnerCone();
                SXRSpotLight gvrLight = new SXRSpotLight(mContext);

                if (innerAngleRadians == 0.0f)
                {
                    innerAngleRadians = outerAngleRadians / 1.5f;
                }
                gvrLight.setInnerConeAngle((float) Math.toDegrees(innerAngleRadians));
                gvrLight.setOuterConeAngle((float) Math.toDegrees(outerAngleRadians));
                l = gvrLight;
            }
            else
            {
                continue;
            }
            lightlist.put(name, l);
            com.samsungxr.jassimp.AiColor ambientCol = light.getColorAmbient(sWrapperProvider);
            com.samsungxr.jassimp.AiColor diffuseCol = light.getColorDiffuse(sWrapperProvider);
            com.samsungxr.jassimp.AiColor specular = light.getColorSpecular(sWrapperProvider);
            float[] c = new float[3];
            getColor(ambientCol, c);
            l.setVec4("ambient_intensity", c[0], c[1], c[2], 1.0f);
            getColor(diffuseCol, c);
            l.setVec4("diffuse_intensity", c[0], c[1], c[2], 1.0f);
            getColor(specular, c);
            l.setVec4("specular_intensity", c[0], c[1], c[2], 1.0f);
            if ((l instanceof SXRPointLight) || (l instanceof SXRSpotLight))
            {
                setAttenuation(l, light);
            }
        }
    }

    private void setAttenuation(SXRLight gvrLight, AiLight assimpLight)
    {
        float aconstant = assimpLight.getAttenuationConstant();
        float alinear = assimpLight.getAttenuationLinear();
        float aquad = assimpLight.getAttenuationQuadratic();

        if (Double.isInfinite(alinear))
        {
            alinear = 1.0f;
        }
        if (Double.isInfinite(aquad))
        {
            aquad = 1.0f;
        }
        if ((aconstant + aquad + alinear) == 0.0f)
        {
            aconstant = 1.0f;
        }
        gvrLight.setFloat("attenuation_constant", aconstant);
        gvrLight.setFloat("attenuation_linear", alinear);
        gvrLight.setFloat("attenuation_quadratic", aquad);
    }

    private void getColor(AiColor c, float[] color)
    {
        color[0] = c.getRed();
        color[1] = c.getGreen();
        color[2] = c.getBlue();
        float scale = max(max(color[0], color[1]), color[2]);
        if (scale > 1)
        {
            color[0] /= scale;
            color[1] /= scale;
            color[2] /= scale;
        }
    }
}