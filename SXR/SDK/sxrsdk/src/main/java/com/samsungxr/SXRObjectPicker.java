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

package com.samsungxr;

/**
 * Finds the nodes that intersect the node the
 * picker is attached to.
 *
 * For a {@linkplain SXRNode node} to be pickable, it must have a
 * {@link SXRCollider} component attached to it that is enabled.
 * The picker returns an array containing all the collisions as instances of SXRPickedObject.
 * The picked object contains the collider instance, the distance between
 * the colliding and the node and the center of the node hit
 * in world coordinates.
 *
 * The picker maintains the list of currently
 * picked objects which can be obtained with getPicked() and continually
 * updates it each frame. When a pickable object is inside the view frustum,
 * the picker generates one or more pick events (IPickEvents interface)
 * which are sent the event receiver of the scene. These events can be
 * observed by listeners.
 *  - onEnter(SXRNode)  called when the node enters the frustum.
 *  - onExit(SXRNode)   called when the node exits the frustum.
 *  - onInside(SXRNode) called while the node is inside the frustum.
 *  - onPick(SXRPicker)        called when the set of picked objects changes.
 *  - onNoPick(SXRPicker)      called once when nothing is picked.
 *
 * @see IPickEvents
 * @see SXRNode#attachComponent(SXRComponent)
 * @see SXRCollider
 * @see SXRComponent#setEnable(boolean)
 * @see com.samsungxr.SXRPicker.SXRPickedObject
 * @see SXRPicker
 */
public class SXRObjectPicker extends SXRPicker
{

    /**
     * Construct a picker which picks from a given scene.
     * @param context context that owns the scene
     * @param scene scene containing the nodes to pick from
     */
    public SXRObjectPicker(SXRContext context, SXRScene scene)
    {
        super(context, scene);
    }

    public void onDrawFrame(float frameTime)
    {
        if (isEnabled())
        {
            doPick();
        }
    }

    /**
     * Scans the scene graph to collect picked items
     * and generates appropriate pick events.
     * This function is called automatically by
     * the picker if it is attached to a node.
     * You can instantiate the picker and not attach
     * it to a node. In this case you must
     * manually set the pick ray and call processPick()
     * to generate the pick events.
     *
     * @see IPickEvents
     * @see SXRFrustumPicker#pickVisible
     */
    protected void doPick()
    {
        SXRNode owner = getOwnerObject();
        SXRPickedObject[] picked = SXRFrustumPicker.pickVisible(mScene);

        if (owner != null)
        {
            for (int i = 0; i < picked.length; ++i)
            {
                SXRPickedObject hit = picked[i];

                if (hit != null)
                {
                    SXRNode sceneObj = hit.hitObject;
                    if (!owner.intersectsBoundingVolume(sceneObj))
                    {
                        picked[i] = null;
                    }
                }
            }
        }
        generatePickEvents(picked);
    }

    /**
     * Determines whether or not two axially aligned bounding boxes in
     * the same coordinate space intersect.
     * @param bv1 first bounding volume to test.
     * @param bv2 second bounding volume to test.
     * @return true if the boxes intersect, false if not.
     */
    protected boolean intersect(SXRNode.BoundingVolume bv1, SXRNode.BoundingVolume bv2)
    {
        return  (bv1.maxCorner.x >= bv2.minCorner.x) &&
                (bv1.maxCorner.y >= bv2.minCorner.y) &&
                (bv1.maxCorner.z >= bv2.minCorner.z) &&
                (bv1.minCorner.x <= bv2.maxCorner.x) &&
                (bv1.minCorner.y <= bv2.maxCorner.y) &&
                (bv1.minCorner.z <= bv2.maxCorner.z);
    }
}
