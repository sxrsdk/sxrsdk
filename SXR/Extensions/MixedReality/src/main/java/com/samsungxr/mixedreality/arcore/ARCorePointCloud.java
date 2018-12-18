package com.samsungxr.mixedreality.arcore;

import com.google.ar.core.PointCloud;
import com.samsungxr.mixedreality.SXRPointCloud;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ARCorePointCloud extends SXRPointCloud {
    PointCloud mARPointCloud;
    float mScale;
    private float[] mPoints;
    private float[] mConfidenceValues;

    ARCorePointCloud(float scale) {
        mScale = scale;
    }

    @Override
    public int[] getIds() {
        IntBuffer ib = mARPointCloud.getIds();
        int[] pointIds = new int[ib.limit()];

        for (int i = 0; i < ib.limit(); i++) {
            pointIds[i] = ib.get(i);
        }

        return pointIds;
    }

    @Override
    public float[] getPoints() {
        if (mPoints == null) {
            setPoints();
        }

        return mPoints;
    }

    @Override
    public float[] getConfidenceValues() {
        if (mConfidenceValues == null) {
            setPoints();
        }

        return mConfidenceValues;
    }

    @Override
    public void release() {
        mARPointCloud.release();
    }

    public void setARPointCloud(PointCloud pointCloud) {
        mARPointCloud = pointCloud;
    }

    private void setPoints() {
        FloatBuffer fb = mARPointCloud.getPoints();
        int pointCount = fb.limit() / 4;
        mPoints = new float[pointCount * 3];
        mConfidenceValues = new float[pointCount];

        for (int i = 0, j = 0, k = 0; i < fb.limit(); i += 4) {
            mPoints[j++] = fb.get(i) * mScale;
            mPoints[j++] = fb.get(i + 1) * mScale;
            mPoints[j++] = fb.get(i + 2) * mScale;
            mConfidenceValues[k++] = fb.get(i + 3);
        }
    }
}
