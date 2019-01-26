package com.samsungxr.mixedreality;

public abstract class SXRPointCloud {
    /**
     * @return a array of point IDs
     */
    public abstract int[] getIds();

    /**
     * @return a array of 3D points coordinates
     */
    public abstract float[] getPoints();

    /**
     * @return confidence values of points
     */
    public abstract float[] getConfidenceValues();

    /**
     * Release point cloud resources back to ARCore
     */
    public abstract void release();
}
