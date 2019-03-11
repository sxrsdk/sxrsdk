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

package com.samsungxr.mixedreality;

import android.support.annotation.NonNull;

import com.google.ar.core.Pose;
import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;

import java.nio.FloatBuffer;

/**
 * Represents the  current best knowledge of a real-world planar surface.
 */
public abstract class SXRPlane extends SXRBehavior
{
    static private long TYPE_PLANE = newComponentType(SXRPlane.class);
    protected SXRTrackingState mTrackingState;
    protected SXRPlane mParentPlane;
    protected Type mPlaneType;

    protected SXRPlane(SXRContext SXRContext)
    {
        super(SXRContext);
        mType = getComponentType();
    }

    static public long getComponentType() { return TYPE_PLANE; }

    /**
     *
     * @return The plane tracking state
     */
    public abstract SXRTrackingState getTrackingState();

    /**
     * Gets the center pose.
     *
     * @param poseOut Array to export the pose to.
     */
    public abstract void getCenterPose(@NonNull float[] poseOut);

    /**
     * Gets the center pose.
     *
     * @return the pose of the center of the detected plane.
     */
    public abstract Pose getCenterPose();

    public Type getPlaneType()
    {
        return mPlaneType;
    }

    /**
     * @return The plane width
     */
    public abstract float getWidth();

    /**
     * @return The plane height
     */
    public abstract float getHeight();

    /**
     * @return The polygon that best represents the plane
     */
    public abstract FloatBuffer getPolygon();

    /**
     * Create a array of float containing the 3 coordinates
     * vertices of the polygon that best represents the plane.
     *
     * @return The array of vertices
     */
    public abstract float[] get3dPolygonAsArray();

    /**
     * @return The parent plane
     */
    public SXRPlane getParentPlane()
    {
        return mParentPlane;
    }

    /**
     * Check if the given pose is in the plane's polygon.
     *
     * @param pose the pose matrix to check
     * @return whether the pose is in the plane's polygon or not.
     */
    public abstract boolean isPoseInPolygon(float[] pose);

    /**
     * Describes the possible types of planes
     */
    public enum Type
    {
        HORIZONTAL_DOWNWARD_FACING,
        HORIZONTAL_UPWARD_FACING,
        VERTICAL
    }
}
