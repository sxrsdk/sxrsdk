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

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Finds the nodes that are within a view frustum.
 *
 * The picker can function in two modes. One way is to simply call its
 * static functions to make a single scan through the scene to determine
 * what is within the view frustum.
 *
 * The other way is to add the picker as a component to a node
 * and specify the view frustum dimensions. The viewpoint of the frustum
 * is the center of the node. The view direction is the forward
 * direction of the node. The frustum will pick what a camera
 * attached to the node with that view frustum would see.
 *
 * For a {@linkplain SXRNode node} to be pickable, it must have a
 * {@link SXRCollider} component attached to it that is enabled.
 * The picker returns an array containing all the collisions as instances of SXRPickedObject.
 * The picked object contains the collider instance, the distance from the
 * origin of the view frustum and the center of the object.
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
 */
public class SXRFrustumPicker extends SXRPicker {
    protected FrustumIntersection mCuller;
    protected float[] mProjMatrix = null;
    protected Matrix4f mProjection = null;

    /**
     * Construct a picker which picks from a given scene.
     * @param context context that owns the scene
     * @param scene scene containing the nodes to pick from
     */
    public SXRFrustumPicker(SXRContext context, SXRScene scene)
    {
        super(context, scene);
        setFrustum(90.0f, 1.0f, 0.1f, 1000.0f);
    }

    /**
     * Set the view frustum to pick against from the minimum and maximum corners.
     * The viewpoint of the frustum is the center of the node
     * the picker is attached to. The view direction is the forward
     * direction of that node. The frustum will pick what a camera
     * attached to the node with that view frustum would see.
     * If the frustum is not attached to a node, it defaults to
     * the view frustum of the main camera of the scene.
     *
     * @param frustum array of 6 floats as follows:
     *                frustum[0] = left corner of frustum
     *                frustum[1] = bottom corner of frustum
     *                frustum[2] = front corner of frustum (near plane)
     *                frustum[3] = right corner of frustum
     *                frustum[4] = top corner of frustum
     *                frustum[5 = back corner of frustum (far plane)
     */
    public void setFrustum(float[] frustum)
    {
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.setFrustum(frustum[0], frustum[3], frustum[1], frustum[4], frustum[2], frustum[5]);
        setFrustum(projMatrix);
    }

    /**
     * Set the view frustum to pick against from the field of view, aspect
     * ratio and near, far clip planes. The viewpoint of the frustum
     * is the center of the node the picker is attached to.
     * The view direction is the forward direction of that node.
     * The frustum will pick what a camera attached to the node
     * with that view frustum would see. If the frustum is not attached
     * to a node, it defaults to the view frustum of the main camera of the scene.
     *
     * @param fovy  vertical field of view in degrees
     * @param aspect aspect ratio (width / height)

     */
    public void setFrustum(float fovy, float aspect, float znear, float zfar)
    {
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective((float) Math.toRadians(fovy), aspect, znear, zfar);
        setFrustum(projMatrix);
    }

    /**
     * Set the view frustum to pick against from the given projection  matrix.
     *
     * If the projection matrix is null, the picker will revert to picking
     * objects that are visible from the viewpoint of the scene's current camera.
     * If a matrix is given, the picker will pick objects that are visible
     * from the viewpoint of it's owner the given projection matrix.
     *
     * @param projMatrix 4x4 projection matrix or null
     * @see SXRScene#setPickVisible(boolean)
     */
    public void setFrustum(Matrix4f projMatrix)
    {
        if (projMatrix != null)
        {
            if (mProjMatrix == null)
            {
                mProjMatrix = new float[16];
            }
            mProjMatrix = projMatrix.get(mProjMatrix, 0);
            mScene.setPickVisible(false);
            if (mCuller != null)
            {
                mCuller.set(projMatrix);
            }
            else
            {
                mCuller = new FrustumIntersection(projMatrix);
            }
        }
        mProjection = projMatrix;
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
     * manually set the pick ray and call doPick()
     * to generate the pick events.
     * @see IPickEvents
     * @see SXRFrustumPicker#pickVisible(SXRScene)
     */
    public void doPick()
    {
        SXRNode owner = getOwnerObject();
        SXRPickedObject[] picked = pickVisible(mScene);

        if (mProjection != null)
        {
            Matrix4f view_matrix;
            if (owner != null)
            {
                view_matrix = owner.getTransform().getModelMatrix4f();
            }
            else
            {
                view_matrix = mScene.getMainCameraRig().getHeadTransform().getModelMatrix4f();
            }
            view_matrix.invert();

            for (int i = 0; i < picked.length; ++i)
            {
                SXRPickedObject hit = picked[i];

                if (hit != null)
                {
                    SXRNode sceneObj = hit.hitObject;
                    SXRNode.BoundingVolume bv = sceneObj.getBoundingVolume();
                    Vector4f center = new Vector4f(bv.center.x, bv.center.y, bv.center.z, 1);
                    Vector4f p = new Vector4f(bv.center.x, bv.center.y, bv.center.z + bv.radius, 1);
                    float radius;

                    center.mul(view_matrix);
                    p.mul(view_matrix);
                    p.sub(center, p);
                    p.w = 0;
                    radius = p.length();
                    if (!mCuller.testSphere(center.x, center.y, center.z, radius))
                    {
                        picked[i] = null;
                    }
                }
            }
        }
        generatePickEvents(picked);
    }

    /**
     * Returns the list of colliders attached to nodes that are
     * visible from the viewpoint of the camera.
     *
     * <p>
     * This method is thread safe because it guarantees that only
     * one thread at a time is picking against particular scene graph,
     * and it extracts the hit data during within its synchronized block. You
     * can then examine the return list without worrying about another thread
     * corrupting your hit data.
     *
     * The hit location returned is the world position of the node center.
     *
     * @param scene
     *            The {@link SXRScene} with all the objects to be tested.
     *
     * @return A list of {@link com.samsungxr.SXRPicker.SXRPickedObject}, sorted by distance from the
     *         camera rig. Each {@link com.samsungxr.SXRPicker.SXRPickedObject} contains the node
     *         which owns the {@link SXRCollider} along with the hit
     *         location and distance from the camera.
     *
     * @since 1.6.6
     */
    public static final SXRPickedObject[] pickVisible(SXRScene scene) {
        sFindObjectsLock.lock();
        try {
            final SXRPickedObject[] result = NativePicker.pickVisible(scene.getNative());
            return result;
        } finally {
            sFindObjectsLock.unlock();
        }
    }
}
