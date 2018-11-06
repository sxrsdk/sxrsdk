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
 * A sphere collider allows a node to be picked
 * when a ray penetrates its bounding sphere.
 * 
 * This is the fastest but least accurate method of picking.
 * It performs a ray-sphere intersection and works best
 * on nodes which do not vary a lot in their
 * X, Y and Z dimensions.
 * 
 * The center of the collision sphere is the center of
 * the node it is attached to. If no radius is
 * provided, the radius is obtained from the bounding
 * volume of the node.
 *
 * You can use a sphere collider on a node without
 * a mesh if you specify the radius by calling setRadius.
 * This radius is transformed by the world matrix on the node
 * so it will scale as the node does.
 *
 * @see SXRPicker
 * @see SXRSphereCollider
 * @see SXRMeshCollider
 * @see SXRNode#addChildObject(SXRComponent)
 * @see SXRNode#getBoundingVolume()
 */
public class SXRSphereCollider extends SXRCollider
{
    public SXRSphereCollider(SXRContext context)
    {
        super(context, NativeSphereCollider.ctor());
    }
    
    /**
     * Set the radius of the collision sphere.
     * 
     * This radius is transformed by the world matrix
     * associated with the node that owns
     * the sphere collider. If a non-zero radius
     * is specified, it overrides the bounding
     * volume of the node. Otherwise the
     * radius is computed from the bounds of the
     * mesh attached to the node.
     * 
     * @param radius radius of collision sphere
     * @see SXRSphereCollider#getRadius()
     * @see SXRMesh#getSphereBound(float[])
     * @see SXRPicker
     */
    public void setRadius(float radius)
    {
        NativeSphereCollider.setRadius(getNative(), radius);
    }

    /**
     * Get the radius set on the collision sphere.
     * 
     * This radius is the one provided by setRadius
     * or 0 if the radius has not been set.
     * It is NOT the radius of the bounding volume
     * on the node.
     * 
     * @return radius set for collision sphe4re
     * @see SXRSphereCollider#setRadius(float)
     * @see SXRPicker
     */
    public float getRadius()
    {
        return NativeSphereCollider.getRadius(getNative());        
    }
}

class NativeSphereCollider
{
    static native long ctor();
    
    static native float getRadius(long jcollider);
    
    static native void setRadius(long collider, float radius);
}