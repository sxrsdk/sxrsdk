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
 * A capsule collider allows a node to be picked
 * when a ray penetrates its bounding capsule. The
 * capsule is made of two half-spheres joined
 * together by a cylinder.
 *
 * This is the second fastest method of picking.
 * It performs a ray-sphere intersection
 * along the height of the capsule.
 *
 * The center of the collision capsule is the center of
 * the node it is attached to.
 *
 * You can use a capsule collider on a node without
 * a mesh if you specify the radius by calling setRadius
 * and height by calling setHeight.
 * These radius and height are transformed by the world
 * matrix on the node so it will scale as the node does.
 *
 * @see SXRPicker
 * @see SXRMeshCollider
 * @see SXRNode#addChildObject(SXRComponent)
 */
public class SXRCapsuleCollider extends SXRCollider {
    public enum CapsuleDirection {
        X_AXIS,
        Y_AXIS,
        Z_AXIS,
    }

    /**
     * Constructor to make capsule collider.
     *
     * The capsule collider default direction is the Y Axis.
     *
     * @param context
     *            The {@link SXRContext} used by the app.
     *
     */
    public SXRCapsuleCollider(SXRContext context) {
        super(context, NativeCapsuleCollider.ctor());
    }

    /**
     * Set the radius of the collision capsule.
     *
     * This radius represent the local width of the
     * capsule and is transformed by the world
     * matrix associated with the node that owns
     * the capsule collider.
     *
     * @param radius radius of collision capsule
     * @see SXRPicker
     */
    public void setRadius(float radius) {
        NativeCapsuleCollider.setRadius(getNative(), radius);
    }

    /**
     * Set the height of the collision capsule.
     *
     * This height is transformed by the world matrix
     * associated with the node that owns
     * the capsule collider.
     *
     * @param height height of collision capsule
     * @see SXRPicker
     */
    public void setHeight(float height) {
        NativeCapsuleCollider.setHeight(getNative(), height);
    }

    /**
     * Set the axis direction of the collision capsule.
     *
     * It represents the axis of the capsule’s lengthwise
     * orientation in the object’s local space.
     * The default direction is the Y axis.
     * This direction is transformed by the world matrix
     * associated with the node that owns
     * the capsule collider.
     *
     * @param direction axis direction of collision capsule
     * @see SXRPicker
     */
    public void setDirection(CapsuleDirection direction) {
        switch (direction) {
            case X_AXIS:
                NativeCapsuleCollider.setToXDirection(getNative());
                break;
            case Y_AXIS:
                NativeCapsuleCollider.setToYDirection(getNative());
                break;
            case Z_AXIS:
                NativeCapsuleCollider.setToZDirection(getNative());
                break;
        }
    }
}

class NativeCapsuleCollider {
    static native long ctor();

    static native void setRadius(long jcollider, float radius);

    static native void setHeight(long jcollider, float height);

    static native void setToXDirection(long jcollider);

    static native void setToYDirection(long jcollider);

    static native void setToZDirection(long jcollider);
}