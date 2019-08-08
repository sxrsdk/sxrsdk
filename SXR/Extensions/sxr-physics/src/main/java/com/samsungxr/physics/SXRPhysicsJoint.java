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

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.animation.SXRSkeleton;

/**
 * Represents a joint in a multibody chain like a ragdoll.
 * <p>
 */
public class SXRPhysicsJoint extends SXRPhysicsWorldObject
{
    private final SXRPhysicsContext mPhysicsContext;

    /**
     * Constructs the root joint of a multibody chain.
     *
     * @param ctx   The context of the app.
     * @param mass  mass of the root joint.
     */
    public SXRPhysicsJoint(SXRContext ctx, float mass, int numBones)
    {
        super(ctx, NativePhysicsJoint.ctorRoot(mass, numBones));
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    /**
     * Constructs a multibody joint in a chain.
     * <p>
     * This joint is linked to the parent joint in the physics world.
     * This parent should be consistent with the node hierarchy
     * the joints are attached to.
     * @param parent   The parent joint of this one.
     * @param boneID   0 based bone ID indicating which bone of the skeleton
     *                 this joint belongs to
     */
    public SXRPhysicsJoint(SXRPhysicsJoint parent, int boneID, float mass)
    {
        super(parent.getSXRContext(), NativePhysicsJoint.ctorLink(parent.getNative(), boneID, mass));
        if (boneID < 1)
        {
            throw new IllegalArgumentException("BoneID must be greater than zero");
        }
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRPhysicsJoint(SXRContext ctx, long nativeJoint)
    {
        super(ctx, nativeJoint);
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    static public long getComponentType() {
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
    private static SXRWorld getWorldFromAscendant(SXRNode worldOwner) {
        SXRComponent world = null;

        while (worldOwner != null && world == null) {
            world = worldOwner.getComponent(SXRWorld.getComponentType());
            worldOwner = worldOwner.getParent();
        }

        return (SXRWorld) world;
    }

    /**
     * Returns the mass of the body.
     *
     * @return The mass of the body.
     */
    public float getMass() {
        return NativePhysicsJoint.getMass(getNative());
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
     * Apply a central force vector [X, Y, Z] to this {@linkplain SXRPhysicsJoint joint}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void applyCentralForce(final float x, final float y, final float z)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run()
            {
                NativePhysicsJoint.applyCentralForce(getNative(), x, y, z);
            }
        });
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
    protected void addToWorld(SXRWorld world)
    {
        if (world != null)
        {
            world.addBody(this);
        }
    }

    @Override
    protected void removeFromWorld(SXRWorld world)
    {
        if (world != null)
        {
            world.removeBody(this);
        }
    }

    protected void attachSkeleton(SXRSkeleton skel, float defaultMass)
    {
        SXRNode rootBone = skel.getBone(0);
        SXRPhysicsJoint joint = (SXRPhysicsJoint) rootBone.getComponent(SXRPhysicsJoint.getComponentType());
        if (joint == null)
        {
            joint = new SXRPhysicsJoint(skel.getSXRContext(), defaultMass, skel.getNumBones() - 1);
        }
        SXRPhysicsJoint rootJoint = joint;
        for (int i = 1; i < skel.getNumBones(); ++i)
        {
            SXRNode bone = skel.getBone(i);
            if (bone != null)
            {
                joint = (SXRPhysicsJoint) bone.getComponent(SXRPhysicsJoint.getComponentType());
                if (joint == null)
                {
                    SXRNode parent = skel.getBone(skel.getParentBoneIndex(i));
                    SXRPhysicsJoint parentJoint = this;

                    if (parent != null)
                    {
                        SXRPhysicsJoint pj = (SXRPhysicsJoint) parent.getComponent(SXRPhysicsJoint.getComponentType());

                        if (pj != null)
                        {
                            parentJoint = pj;
                        }
                    }
                    joint = new SXRPhysicsJoint(parentJoint, i, defaultMass);
                    bone.attachComponent(joint);
                }
            }
        }
        rootBone.attachComponent(rootJoint);
    }
}

class NativePhysicsJoint
{
    static native long ctorRoot(float mass, int numBones);

    static native long ctorLink(long parent_joint, int boneid, float mass);

    static native long getComponentType();

    static native float getMass(long joint);

    static native int getBoneID(long joint);

    static native void setFriction(long joint, float friction);

    static native float getFriction(long joint);

    static native void applyCentralForce(long joint, float x, float y, float z);

    static native void applyTorque(long joint, float x, float y, float z);
}
