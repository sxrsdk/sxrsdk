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

import com.samsungxr.IEvents;

/**
 * This interface defines events generated by {@link SXRPlane}
 */
public interface IPlaneEvents extends IEvents
{
    /**
     * Called when a new plane is detected.
     *
     * @param plane the new {@link SXRPlane} component created
     */
    void onPlaneDetected(SXRPlane plane);

    /**
     * Called when the tracking state of the plane changes.
     *
     * @param plane the {@link SXRPlane} component which changed
     * @param trackingState
     */
    void onPlaneStateChange(SXRPlane plane, SXRTrackingState trackingState);

    /**
     * Called when the childPlane merges with the parentPlane.
     *
     * @param childPlane the {@link SXRPlane} component which is being consumed.
     * @param parentPlane the {@link SXRPlane} component which remains.
     */
    void onPlaneMerging(SXRPlane childPlane, SXRPlane parentPlane);

    /**
     * Called when the polygon that represents the plane changes.
     *
     * @param plane the {@link SXRPlane} component which its polygon changed.
     */
    void onPlaneGeometryChange(SXRPlane plane);
}
