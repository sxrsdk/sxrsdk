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
 * Represents collision geometry that is a mesh.
 *
 * A {@link SXRCollider} is something that is being pointed at by a picking
 * ray. {@linkplain SXRCollider Colliders} are attached to
 * {@link SXRNode nodes.} The {@link SXRPicker} will return an
 * array of SXRColliders: you use {@link SXRCollider#getOwnerObject()} to retrieve the node.
 *
 * <p>
 * A MeshCollider holds the {@link SXRMesh} that the picking ray will be
 * tested against. If no mesh is specified, it will use the mesh
 * attached to the SXRNode that owns it.
 *
 * You do not need to wait for the mesh to load before attaching the collider.
 * If the node that owns the mesh collider does not have a mesh and
 * the mesh collider doesn't have one, the node will not be pickable.
 */
public class SXRMeshCollider extends SXRCollider {
    private SXRMesh mMesh;

    /**
     * Constructor to make mesh collider and attach a mesh.
     *
     * When the mesh is complicated, it will be cheaper - though less accurate -
     * to use the bounding box instead of the mesh.
     *
     * @param gvrContext
     *            The {@link SXRContext} used by the app.
     *
     * @param mesh
     *            The {@link SXRMesh} that the picking ray will test against.
     */
    public SXRMeshCollider(SXRContext gvrContext, SXRMesh mesh) {
        super(gvrContext, NativeMeshCollider.ctorMesh(mesh.getNative()));
        mMesh = mesh;
    }

    /**
     * Constructor to make mesh collider that supports coordinate picking such as
     * texture coordinates and Barycentric coordinates.
     *
     * @param gvrContext
     *            The {@link SXRContext} used by the app.
     *
     * @param mesh
     *            The {@link SXRMesh} that the picking ray will test against.
     *
     * @param pickCoordinates
     *            If true, coordinate information will be supplied in {@link com.samsungxr.SXRPicker.SXRPickedObject}.
     */
    public SXRMeshCollider(SXRContext gvrContext, SXRMesh mesh, boolean pickCoordinates) {
        super(gvrContext, NativeMeshCollider.ctorMeshPicking((mesh != null) ? mesh.getNative() : 0L, pickCoordinates));
        mMesh = mesh;
    }

    /**
     * Constructor to make mesh collider without a mesh.
     *
     * The collider will use the mesh attached to the
     * node that owns it. If there is no mesh
     * on that node, the collider will never be picked.
     *
     * Your application does not have to wait for the mesh to load
     * before attaching a collider - it will become pickable
     * when the mesh becomes available.
     *
     * @param gvrContext
     *            The {@link SXRContext} used by the app.
     * @param useMeshBounds
     *            If true, the mesh bounding box is used instead of the mesh.
     */
    public SXRMeshCollider(SXRContext gvrContext, boolean useMeshBounds) {
        super(gvrContext, NativeMeshCollider.ctor(useMeshBounds));
    }

    /**
     * Simple constructor.
     *
     * When the mesh is complicated, it will be cheaper - though less accurate -
     * to use {@link SXRMesh#getBoundingBox()} instead of the raw mesh.
     *
     * @param mesh
     *            The {@link SXRMesh} that the picking ray will test against.
     */
    public SXRMeshCollider(SXRMesh mesh) {
        this(mesh.getSXRContext(), mesh);
    }

    /**
     * Retrieve the mesh that is held by this SXRMeshCollider
     * 
     * @return the {@link SXRMesh}
     *
     */
    public SXRMesh getMesh() {
        return mMesh;
    }

    /**
     * Set the mesh to be tested against.
     *
     * @param mesh
     *            The {@link SXRMesh} that the picking ray will test against.
     *
     */
    public void setMesh(SXRMesh mesh) {
        mMesh = mesh;
        NativeMeshCollider.setMesh(getNative(), mesh.getNative());
    }
}

class NativeMeshCollider {
    static native long ctorMesh(long mesh);

    static native long ctor(boolean useMeshBounds);

    static native long ctorMeshPicking(long mesh, boolean pickCoordinates);

    static native void setMesh(long meshCollider, long mesh);
}
