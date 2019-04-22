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

import com.samsungxr.SXRBehavior;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

/**
 * Represents a common Augmented Image in MixedReality
 */
public abstract class SXRMarker extends SXRBehavior
{
    static private long TYPE_MARKER = newComponentType(SXRMarker.class);
    protected SXRTrackingState mTrackingState;

    protected SXRMarker(SXRContext SXRContext)
    {
        super(SXRContext);
        mType = getComponentType();
    }

    /**
     * Return the marker's name.
     * @return
     */
    public abstract String getName();

    /**
     * @return Returns the estimated width
     */
    public abstract float getExtentX();

    /**
     * @return Returns the estimated height
     */
    public abstract float getExtentZ();

    /**
     *
     * @return The augmented image center pose
     */
    public abstract float[] getCenterPose();

    /**
     *
     * @return The tracking state
     */
    public abstract  SXRTrackingState getTrackingState();

    /**
     * Create an anchor attached to this image
     * @return SXRAnchor
     * @param owner {@link SXRNode} to attach the anchor component to.
     */
    public abstract SXRAnchor createAnchor(SXRNode owner);

    static public long getComponentType() { return TYPE_MARKER; }

}
