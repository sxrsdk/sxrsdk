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

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;

/**
 * Represents a fixed location and orientation in the real world.
 */
public abstract class SXRAnchor extends SXRNode {
    protected SXRTrackingState mTrackingState;

    protected SXRAnchor(SXRContext gvrContext) {
        super(gvrContext);
    }

    /**
     * Attach a scene object to be displayed on this anchor position
     *
     * @param obj
     */
    public void attachNode(SXRNode obj) {
        addChildObject(obj);
    }

    /**
     *
     * @return The anchor tracking state
     */
    public abstract SXRTrackingState getTrackingState();

    /**
     *
     * @return The cloud anchor ID
     */
    public abstract String getCloudAnchorId();

}
