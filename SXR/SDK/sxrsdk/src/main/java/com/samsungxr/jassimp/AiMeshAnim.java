
package com.samsungxr.jassimp;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;


public class AiMeshAnim {
    private final int SIZEOF_DOUBLE = Jassimp.NATIVE_DOUBLE_SIZE;

    AiMeshAnim(String nodeName, int numMeshMorphKeys, int numMorphTargets) {
        m_nodeName = nodeName;
        m_numMeshMorphKeys = numMeshMorphKeys;
        m_numMorphTargets = numMorphTargets;
        m_morphTargetWeights = ByteBuffer.allocateDirect(numMeshMorphKeys * (numMorphTargets + 1) * SIZEOF_DOUBLE);
        m_morphTargetWeights.order(ByteOrder.nativeOrder());
    }

    /**
     * @return the morph animation keyframe data in the following timestamp blend weight format
     * <p>
     * t1, w, w, w, w, .....w, w, w
     * t2, w, w, w, w, .....w, w, w
     * t3, w, w, w, w, .....w, w, w
     * t4, w, w, w, w, .....w, w, w
     * .
     * .
     */
    public float[] getMorphAnimationKeys() {
        DoubleBuffer weights = m_morphTargetWeights.asDoubleBuffer();
        double[] arr = new double[weights.remaining()];
        weights.get(arr);
        float[] farr = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            farr[i] = (float) arr[i];
        }
        return farr;
    }

    /**
     * @return the number of morph targets in this mesh morph animation
     */
    public int getNumMorphTargets() {
        return m_numMorphTargets;
    }

    /**
     * @return the name of the mesh of this animation
     */
    public String getNodeName() {
        return m_nodeName;
    }

    /**
     * Node name.
     */
    private final String m_nodeName;
    /**
     * Number of position keys.
     */
    private final int m_numMeshMorphKeys;
    /**
     * Number of morph targets.
     */
    private final int m_numMorphTargets;
    /**
     * Buffer with position keys.
     */
    private ByteBuffer m_morphTargetWeights;
}