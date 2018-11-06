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

import java.util.ArrayList;
import java.util.List;

import com.samsungxr.io.SXRCursorController;
import com.samsungxr.utility.Log;

/**
 * Finds the nodes in a scene which collide with the bounding volumes
 * of a set of specific objects (called collidables).
 * <p>
 * This picker provides a simple form of bounds-based collision detection
 * for a small set of objects. If you have a large number of objects or
 * complex collision shapes, the physics extension would be a better choice.
 * The bounds picker can be useful for designating trigger areas in your scene
 * that generate events when other objects penetrate. Internally GearVRF
 * uses this picker for object cursor hit testing.
 * <p/>
 * For a {@linkplain SXRNode node} to be pickable, it must have a
 * {@link SXRCollider} component attached to it that is enabled.
 * The picker compares the bounds for set of nodes against all these
 * colliders and returns an array with the collisions as instances of SXRPickedObject.
 * The picked object contains the collider instance hit, the distance from the
 * center of the object it collided with and the hit position.
 * <p/>
 * The picker maintains the list of current collisions
 * (which can be obtained with getPicked()) and continually updates it each frame.
 * The picker also generates one or more pick / touch events which are
 * sent to the event receiver of the scene and the picker. These events can be
 * observed by listeners.
 * <ul>
 * <li>onEnter          called when a collidable node penetrates a collider.</li>
 * <li>onExit           called when a collidable node leaves a collider.</li>
 * <li>onInside         called when a collidable node is inside a collider.</li>
 * <li>onTouchStart     called when the action button is pressed during a collision.</li>
 * <li>onTouchEnd       called when the action button is released during a collision.</li>
 * <li>onPick           called every frame if a collision occured.</li>
 * <li>onNoPick         called every frame if no collisions occurred.</li>
 * </ul>
 * @see IPickEvents
 * @see ITouchEvents
 * @see SXRNode#attachCollider(SXRCollider)
 * @see SXRCollider
 * @see SXRCollider#setEnable(boolean)
 * @see SXRPickedObject
 * @see SXRCursorController#addPickEventListener(IEvents)
 */
public class SXRBoundsPicker extends SXRPicker
{
    final ArrayList<SXRNode>   mCollidables = new ArrayList<SXRNode>();
    static private SXRPickedObject[] sEmptyList = new SXRPickedObject[0];

    protected SXRCursorController.IControllerEvent listener = new SXRCursorController.IControllerEvent()
    {
        public void onEvent(SXRCursorController controller, boolean isActive)
        {
            processPick(isActive, controller.getMotionEvent());
        }
    };


    /**
     * Construct a picker which picks from a given scene
     * using a set of spheres in world coordinates.
     *
     * @param scene scene containing the nodes to pick from
     * @param enable true to start in the enabled state (listening for events)
     */
    public SXRBoundsPicker(SXRScene scene, boolean enable)
    {
        super(scene, enable);
        mEventOptions.add(EventOptions.SEND_TOUCH_EVENTS);
    }


    public void setController(SXRCursorController controller)
    {
        if (mController != null)
        {
            mController.getEventReceiver().removeListener(listener);
        }
        mController = controller;
        if (controller != null)
        {
            controller.getEventReceiver().addListener(listener);
        }
    }

    /**
     * Adds another node to pick against.
     * Each frame all the colliders in the scene will be compared
     * against the bounding volumes of all the collidables associated
     * with this picker.
     * @param sceneObj new collidable
     * @return index of collidable added, this is the CursorID in the SXRPickedObject
     */
    public int addCollidable(SXRNode sceneObj)
    {
        synchronized (mCollidables)
        {
            int index = mCollidables.indexOf(sceneObj);
            if (index >= 0)
            {
                return index;
            }
            mCollidables.add(sceneObj);
            return mCollidables.size() - 1;
        }
    }

    /**
     * Remove a node from the list of collidables.
     * This object will no longer participate in collisions
     * for this picker.
     * @param sceneObj collidable node to remove
     */
    public void removeCollidable(SXRNode sceneObj)
    {
        synchronized(mCollidables)
        {
            mCollidables.remove(sceneObj);
        }
    }

    /**
     * Get a collidabled based on its index (as returned
     * by #addCollidable).
     * @param index index of collidable to get
     * @returns {@link SXRNode} or null if not found
     */
    public SXRNode getCollidable(int index)
    {
        synchronized (mCollidables)
        {
            if ((index < 0) || (index >= mCollidables.size()))
            {
                return null;
            }
            return mCollidables.get(index);
        }
    }

    /**
     * Remove a collidabled based on its index (as returned
     * by #addCollidable).
     * This object will no longer participate in collisions
     * for this picker.
     * @param index index of collidable to remove
     */
    public void removeCollidable(int index)
    {
        synchronized (mCollidables)
        {
            if (index < mCollidables.size())
            {
                mCollidables.remove(index);
            }
        }
    }

    /**
     * Cleara all the collidable nodes.
     * No collisions will be reported.
     */
    public void clearCollidables()
    {
        synchronized (mCollidables)
        {
            mCollidables.clear();
        }
    }

    /**
     * Scans the scene graph to collect picked items
     * and generates appropriate pick events.
     * This function is called automatically by
     * the picker every frame.
     * @see IPickEvents
     * @see ITouchEvents
     * @see #pickBounds(SXRScene, List<SXRNode>)
     */
    @Override
    protected void doPick()
    {
        if (mCollidables.size() > 0)
        {
            SXRPickedObject[] picked = null;

            synchronized (mCollidables)
            {
                picked = pickBounds(mScene, mCollidables);
            }
            if (mPickClosest && (picked.length > 0))
            {
                SXRPickedObject closest = null;
                float dist = 100000.0f;
                for (SXRPickedObject hit : picked)
                {
                    if ((hit != null) && (hit.hitDistance < dist))
                    {
                        dist = hit.hitDistance;
                        closest = hit;
                    }
                }
                if (closest != null)
                {
                    picked = new SXRPickedObject[] { closest };
                }
                else
                {
                    picked = sEmptyList;
                }
            }
            generatePickEvents(picked);
        }
    }

    /**
     * Tests the bounding volumes of a set of nodes against
     * all the colliders the scene and returns a list of collisions.
     * <p/>
     * This function is not meant for general collision detection
     * but can be used to implement simple bounds-based collisions.
     * Inside GearVRF it is used for cursor hit testing.
     * <p>
     * This method is thread safe because it guarantees that only
     * one thread at a time is examining the scene graph,
     * and it extracts the hit data during within its synchronized block. You
     * can then examine the return list without worrying about another thread
     * corrupting your hit data.
     * <p/>
     * Unlike ray based picking, the hit location for sphere picking is very
     * inexact. Currently the hit location reported is on the surface of the collider.
     * Mesh colliders are not supported and the mesh is not examined
     * during collision detection. Instead the bounding volume of the node
     * is used, not it's mesh collider.
     *
     * @param scene
     *            The {@link SXRScene} with all the objects to be tested.
     * @param collidables
     *            An array of {@link SXRNode}s to collide against the scene.
     *
     * @return A list of {@link SXRPickedObject}, sorted by distance from the
     *         pick ray origin. Each {@link SXRPickedObject} contains the node
     *         which owns the {@link SXRCollider} along with the hit
     *         location and distance.
     *
     * @since 1.6.6
     */
    public static final SXRPickedObject[] pickBounds(SXRScene scene, List<SXRNode> collidables)
    {
        sFindObjectsLock.lock();
        try
        {
            final SXRPickedObject[] result = NativePicker.pickBounds(scene.getNative(), collidables);
            if (result == null)
            {
                return sEmptyList;
            }
            return result;
        }
        finally
        {
            sFindObjectsLock.unlock();
        }
    }

    /**
     * Internal utility to help JNI add hit objects to the pick list.
     */
    static SXRPickedObject makeObjectHit(long colliderPointer, int collidableIndex, float distance, float hitx, float hity, float hitz)
    {
        SXRCollider collider = SXRCollider.lookup(colliderPointer);
        if (collider == null)
        {
            Log.d("SXRBoundsPicker", "makeObjectHit: cannot find collider for %x", colliderPointer);
            return null;
        }
        SXRPicker.SXRPickedObject hit = new SXRPicker.SXRPickedObject(collider, new float[] { hitx, hity, hitz }, distance);
        hit.collidableIndex = collidableIndex;
        return hit;
    }
}


