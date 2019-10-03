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

package com.samsungxr.physics;

import com.samsungxr.SXRCollider;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.SXRPoseMapper;
import com.samsungxr.animation.SXRSkeleton;

/**
 * Represents a joint in an articulated body.
 * <p>
 * Articulated bodies are implemented using the Bullet
 * Featherstone multibody simulator. A joint can only
 * be added to a simulation world in which multibody
 * support has been enabled.
   A joint can be static or dynamic.
 * Static joints don't move at all, dynamic joints are moved by
 * the physics engine.
 * <p>
 * Joints also have mass and respond to physical forces.
 * To participate in collisions, a joint must have a
 * {@link SXRCollider} component attached to its owner
 * which describes the shape of the rigid body.
 * <p>
 * The joint component is attached to a {@linkplain com.samsungxr.SXRNode node}
 * and uses the transform of its owner object, updating it if the joint is dynamic.
 * Before attaching a joint to a node, make sure the node has the proper
 * position and orientation. You cannot attach a rigid body to a node
 * unless it is in the scene and has a collider component.
 * <p>
 * Unlike rigid bodies, joints are connected in a hierarchy which is
 * not necessarily the same as the hierarchy of the corresponding owner nodes.
 * Each joint has a type which constrains it with respect to its parent.
 * Currently fixed, ball, hinge and slider joints are supported.
 * Constraints between rigid bodies and joints are not currently
 * supported (although the Bullet multibody simulator can do it).
 * </p>
 * @see SXRNode
 * @see SXRCollider
 * @see SXRWorld
 */
public class SXRPhysicsJoint extends SXRPhysicsCollidable
{
    private final SXRPhysicsContext mPhysicsContext;
    private SXRSkeleton mSkeleton = null;
    private SXRPoseMapper mPoseMapper = null;
    private final int mCollisionGroup;

    /**
     * Joint is fixed and does not move.
     */
    static final int FIXED = 1;

    /**
     * Joint rotates spherically around its parent (ball joint).
     */
    static final int SPHERICAL = 2;

    /**
     * Joint rotates around an axis relative to its parent (hinge joint).
     */
    static final int REVOLUTE = 3;

    /**
     * Joint slides along an axis relative to its parent (slider joint).
     */
    static final int PRISMATIC = 4;

    /**
     * Constructs the root joint of a multibody chain which collides with everything.
     *
     * @param ctx      The context of the app.
     * @param mass     mass of the root joint.
     * @oaran numBones number of child joints in the hierarchy.
     */
    public SXRPhysicsJoint(SXRContext ctx, float mass, int numBones)
    {
        this(ctx, mass, numBones, -1);
    }

    /**
     * Constructs the root joint of a multibody chain.
     *
     * @param ctx            The context of the app.
     * @param mass           mass of the root joint.
     * @oaran numBones       number of child joints in the hierarchy.
     * @param collisionGroup inteeger between 0 and 16 indicating which
     *                       collision group the joint belongs to
     */
    public SXRPhysicsJoint(SXRContext ctx, float mass, int numBones, int collisionGroup)
    {
        super(ctx, NativePhysicsJoint.ctorRoot(mass, numBones));
        mCollisionGroup = collisionGroup;
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    /**
     * Constructs a multibody joint in a chain.
     * <p>
     * This joint is linked to the parent joint in the physics world.
     * @param parent    The parent joint of this one.
     * @param jointType Type of joint: one of (FIXED, SPHERICAL, REVOLUTE, PRISMATIC, PLANAR)
     * @param boneID    0 based bone ID indicating which bone of the skeleton
     *                  this joint belongs to
     * @param mass      mass of this joint
     */
    public SXRPhysicsJoint(SXRPhysicsJoint parent, int jointType, int boneID, float mass)
    {
        this(parent, jointType, boneID, mass, -1);
    }

    /**
     * Constructs a multibody joint in a chain.
     * <p>
     * This joint is linked to the parent joint in the physics world.
     * This parent should be consistent with the node hierarchy
     * the joints are attached to.
     * @param parent    The parent joint of this one.
     * @param jointType Type of joint: one of (FIXED, SPHERICAL, REVOLUTE, PRISMATIC, PLANAR)
     * @param boneID    0 based bone ID indicating which bone of the skeleton
     *                  this joint belongs to
     * @param mass      mass of this joint
     */
    public SXRPhysicsJoint(SXRPhysicsJoint parent, int jointType, int boneID, float mass, int collisionGroup)
    {
        super(parent.getSXRContext(),
              NativePhysicsJoint.ctorLink(parent.getNative(),
                                          jointType, boneID, mass));
        if (boneID < 1)
        {
            throw new IllegalArgumentException("BoneID must be greater than zero");
        }
        mCollisionGroup = collisionGroup;
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRPhysicsJoint(SXRContext ctx, long nativeJoint)
    {
        super(ctx, nativeJoint);
        mPhysicsContext = SXRPhysicsContext.getInstance();
        mCollisionGroup = -1;
    }

    /**
     * Set the joint axis for hinge or slider.
     * @param x X direction.
     * @param y Y direction.
     * @param z Z direction.
     */
    public void setAxis(float x, float y, float z)
    {
        NativePhysicsJoint.setAxis(getNative(), x, y, z);
    }

    /**
     * Set the pivot point for this joint.
     * @param x X pivot.
     * @param y Y pivot.
     * @param z Z pivot.
     */
    public void setPivot(float x, float y, float z)
    {
        NativePhysicsJoint.setPivot(getNative(), x, y, z);
    }

    static public long getComponentType()
    {
        return NativePhysicsJoint.getComponentType();
    }

    /**
     * Returns the {@linkplain SXRWorld physics world} of this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The physics world of this {@link SXRRigidBody}
     */
    public SXRWorld getWorld() {
        return getWorld(getOwnerObject());
    }

    /**
     * Returns the {@linkplain SXRWorld physics world} of the {@linkplain com.samsungxr.SXRScene scene}.
     *
     * @param owner Owner of the {@link SXRRigidBody}
     * @return Returns the {@link SXRWorld} of the scene.
     */
    private static SXRWorld getWorld(SXRNode owner) {
        return getWorldFromAscendant(owner);
    }

    /**
     * Looks for {@link SXRWorld} component in the ascendants of the scene.
     *
     * @param worldOwner Scene object to search for a physics world in the scene.
     * @return Physics world from the scene.
     */
    private static SXRWorld getWorldFromAscendant(SXRNode worldOwner)
    {
        SXRComponent world = null;

        while (worldOwner != null && world == null)
        {
            world = worldOwner.getComponent(SXRWorld.getComponentType());
            worldOwner = worldOwner.getParent();
        }

        return (SXRWorld) world;
    }

    /**
     * Returns the mass of the joint.
     *
     * @return The mass of the joint.
     */
    public float getMass() {
        return NativePhysicsJoint.getMass(getNative());
    }

    /**
     * Returns the collision group of this joint..
     *
     * @return The collision group id as an int
     */
    @Override
    public int getCollisionGroup() {
        return mCollisionGroup;
    }

    /**
     * Set the friction for this joint.
     * @param friction  friction value
     * @see #getFriction()
     */
    public void setFriction(float friction)
    {
        NativePhysicsJoint.setFriction(getNative(), friction);
    }

    /**
     * Get the friction for this joint.
     * @return friction value
     * @see #setFriction(float)
     */
    public float getFriction()
    {
        return NativePhysicsJoint.getFriction(getNative());
    }


    /**
     * Apply a torque vector [X, Y, Z] to this {@linkplain SXRPhysicsJoint joint}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void applyTorque(final float x, final float y, final float z)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                NativePhysicsJoint.applyTorque(getNative(), x, y, z);
            }
        });
    }

    /**
     * Apply a torque to a single DOF joint {@linkplain SXRPhysicsJoint joint}
     *
     * @param t torque on the joint
     */
    public void applyTorque(final float t)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                NativePhysicsJoint.applyTorque(getNative(), t, 0, 0);
            }
        });
    }

    /**
     * Returns the bone ID of this joint.
     */
    public int getBoneID() { return NativePhysicsJoint.getBoneID(getNative()); }

    @Override
    public void onAttach(SXRNode newOwner)
    {
        if (newOwner.getCollider() == null)
        {
            throw new UnsupportedOperationException("You must have a collider attached to the node before attaching the joint component");
        }
        super.onAttach(newOwner);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (getBoneID() == 0)
        {
            removeFromWorld(getWorld());
        }
    }
    @Override
    protected void addToWorld(SXRPhysicsContent world)
    {
        if (world != null)
        {
            if (!world.isMultiBody())
            {
                throw new IllegalArgumentException("SXRPhysicsJoint can only be added if multibody is enabled on the physics world");
            }
            world.addBody(this);
        }
    }

    @Override
    protected void removeFromWorld(SXRPhysicsContent world)
    {
        if (world != null)
        {
            world.removeBody(this);
        }
    }

    public SXRSkeleton getSkeleton()
    {
        if (mSkeleton != null)
        {
            return mSkeleton;
        }
        mSkeleton = (SXRSkeleton) getComponent(SXRSkeleton.getComponentType());
        if (mSkeleton != null)
        {
            return mSkeleton;
        }
        long nativeSkeleton = NativePhysicsJoint.getSkeleton(getNative());
        if (nativeSkeleton != 0)
        {
            mSkeleton = new SXRSkeleton(getSXRContext(), nativeSkeleton);
        }
        return mSkeleton;
    }
}


class NativePhysicsJoint
{
    static native long ctorRoot(float mass, int numBones);

    static native long ctorLink(long parent_joint, int jointType, int boneid, float mass);

    static native long getComponentType();

    static native float getMass(long joint);

    static native int getBoneID(long joint);

    static native void setFriction(long joint, float friction);

    static native float getFriction(long joint);

    static native void setAxis(long joint, float x, float y, float z);

    static native void setPivot(long joint, float x, float y, float z);

    static native void applyTorque(long joint, float x, float y, float z);

    static native long getSkeleton(long joint);
}
