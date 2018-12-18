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
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRNode;

/**
 * Represents a rigid body that can be static or dynamic. You can set a mass and apply some
 * physics forces.
 * <p>
 * By default it is a static body with infinity mass, value 0, and does not move under simulation.
 * A dynamic body with a mass defined is fully simulated.
 * <p>
 * Every {@linkplain com.samsungxr.SXRNode node} can represent a rigid body since
 * it has a {@link SXRRigidBody} component attached to.
 *
 * You must setup the values of owner's {@link com.samsungxr.SXRTransform}, like initial position,
 * and the mass value of the rigid body before attach it to its owner.
 */
public class SXRRigidBody extends SXRPhysicsWorldObject {
    public static final int DYNAMIC  = 0;
    public static final int STATIC = 1;
    public static final int KINEMATIC = 2;

    static {
        System.loadLibrary("sxr-physics");
    }

    private final int mCollisionGroup;
    private final SXRPhysicsContext mPhysicsContext;

    private final boolean mLoaded;

    /**
     * Constructs new instance to simulate a rigid body in {@link SXRWorld}.
     *
     * @param gvrContext The context of the app.
     */
    public SXRRigidBody(SXRContext gvrContext) {
        this(gvrContext, 0.0f);
    }

    /**
     * Constructs new instance to simulate a rigid body in {@link SXRWorld}.
     *
     * @param gvrContext The context of the app.
     * @param mass The mass of this rigid body.
     */
    public SXRRigidBody(SXRContext gvrContext, float mass) {
        this(gvrContext, mass, -1);
    }

    /**
     * Constructs new instance to simulate a rigid body in {@link SXRWorld}.
     *
     * @param gvrContext The context of the app.
     * @param mass The mass of this rigid body.
     * @param collisionGroup The id of the collision's group that this rigid body belongs to
     *                       in the {@link SXRCollisionMatrix}. The rigid body collides with
     *                       everyone if {#collisionGroup} is out of the range 0...15.
     */
    public SXRRigidBody(SXRContext gvrContext, float mass, int collisionGroup) {
        super(gvrContext, Native3DRigidBody.ctor(mass));
        mCollisionGroup = collisionGroup;
        mPhysicsContext = SXRPhysicsContext.getInstance();
        mLoaded = false;
    }

    /** Used only by {@link SXRPhysicsLoader} */
    SXRRigidBody(SXRContext gvrContext, long nativeRigidBody) {
        super(gvrContext, nativeRigidBody);
        mCollisionGroup = -1;
        mPhysicsContext = SXRPhysicsContext.getInstance();
        mLoaded = true;
    }

    static public long getComponentType() {
        return Native3DRigidBody.getComponentType();
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
     * Establishes how this rigid body will behave in the simulation.
     *
     * @param type type of simulation desired for the rigid body:
     * <table>
     * <tr><td>DYNAMIC</td><td>Collides with other objects, moved by simulation</td></tr>
     * <tr><td>STATIC</td><td>Collides with other objects, does not move</td></tr>
     * <tr><td>KINEMATIC</td><td>Collides with other objects, moved by application</td></tr>
     * </table>
     */
    public void setSimulationType(int type)
    {
        Native3DRigidBody.setSimulationType(getNative(), type);
    }

    /**
     * Queries how this rigid body will behave in the simulation.
     *
     * @return type of simulation desired for the rigid body
     * <table>
     * <tr><td>DYNAMIC</td><td>Collides with other objects, moved by simulation</td></tr>
     * <tr><td>STATIC</td><td>Collides with other objects, does not move</td></tr>
     * <tr><td>KINEMATIC</td><td>Collides with other objects, moved by application</td></tr>
     * </table>
     */
    public int getSimulationType()
    {
       return Native3DRigidBody.getSimulationType(getNative());
    }

    /**
     * Returns the mass of the body.
     *
     * @return The mass of the body.
     */
    public float getMass() {
        return Native3DRigidBody.getMass(getNative());
    }

    /**
     * Apply a central force vector [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void applyCentralForce(final float x, final float y, final float z) {
                mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
           public void run() {
                                Native3DRigidBody.applyCentralForce(getNative(), x, y, z);
                           }
        });
    }

    /**
     * Apply a force vector [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     * on relative position
     *
     * @param forceX force on the x-axis.
     * @param forceY force on the y-axis.
     * @param forceZ force on the z-axis.
     * @param relX relative position on x-axis to apply the force.
     * @param relY relative position on y-axis to apply the force.
     * @param relZ relative position on z-axis to apply the force.
     */
    public void applyForce(final float forceX, final float forceY, final float forceZ,
                           final float relX, final float relY, final float relZ) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                Native3DRigidBody.applyForce(getNative(), forceX, forceY, forceZ,
                        relX, relY, relZ);
            }
        });
    }

    /**
     * Apply a central vector vector [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     *
     * @param x impulse factor on the 'X' axis.
     * @param y impulse factor on the 'Y' axis.
     * @param z impulse factor on the 'Z' axis.
     */
    public void applyCentralImpulse(final float x, final float y, final float z) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                Native3DRigidBody.applyCentralImpulse(getNative(), x, y, z);
            }
        });
    }

    /**
     * Apply a impulse vector [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     * on relative position
     *
     * @param impulseX impulse on the x-axis.
     * @param impulseY impulse on the y-axis.
     * @param impulseZ impulse on the z-axis.
     * @param relX relative position on x-axis to apply the force.
     * @param relY relative position on y-axis to apply the force.
     * @param relZ relative position on z-axis to apply the force.
     */
    public void applyImpulse(final float impulseX, final float impulseY, final float impulseZ,
                           final float relX, final float relY, final float relZ) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                Native3DRigidBody.applyImpulse(getNative(), impulseX, impulseY, impulseZ,
                        relX, relY, relZ);
            }
        });
    }

    /**
     * Apply a torque vector [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void applyTorque(final float x, final float y, final float z) {
                mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                                Native3DRigidBody.applyTorque(getNative(), x, y, z);
                            }
        });
    }

    /**
     * Apply a torque impulse [X, Y, Z] to this {@linkplain SXRRigidBody rigid body}
     *
     * @param x impulse factor on the 'X' axis.
     * @param y impulse factor on the 'Y' axis.
     * @param z impulse factor on the 'Z' axis.
     */
    public void applyTorqueImpulse(final float x, final float y, final float z) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                Native3DRigidBody.applyTorqueImpulse(getNative(), x, y, z);
            }
        });
    }

    /**
     * Sets a particular acceleration vector [X, Y, Z] on this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void setGravity(float x, float y, float z) {
        Native3DRigidBody.setGravity(getNative(), x, y, z);
    }

    /**
     * Sets linear and angular damping on this {@linkplain SXRRigidBody rigid body}
     *
     * @param linear factor on how much the rigid body resists translation.
     * @param angular factor on how much the rigid body resists rotation.
     */
    public void setDamping(float linear, float angular) {
        Native3DRigidBody.setDamping(getNative(), linear, angular);
    }

    /**
     * Sets a linear velocity [X, Y, Z] on this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void setLinearVelocity(float x, float y, float z) {
        Native3DRigidBody.setLinearVelocity(getNative(), x, y, z);
    }

    /**
     * Sets an angular velocity [X, Y, Z] on this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void setAngularVelocity(float x, float y, float z) {
        Native3DRigidBody.setAngularVelocity(getNative(), x, y, z);
    }

    /**
     * Sets an angular factor [X, Y, Z] that influences torque on this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void setAngularFactor(float x, float y, float z) {
        Native3DRigidBody.setAngularFactor(getNative(), x, y, z);
    }

    /**
     * Sets an linear factor [X, Y, Z] that influences forces acting on this {@linkplain SXRRigidBody rigid body}
     *
     * @param x factor on the 'X' axis.
     * @param y factor on the 'Y' axis.
     * @param z factor on the 'Z' axis.
     */
    public void setLinearFactor(float x, float y, float z) {
        Native3DRigidBody.setLinearFactor(getNative(), x, y, z);
    }

    /**
     * Sets SleepingTresholds that, when reached, influence the deactivation of this {@linkplain SXRRigidBody rigid body}
     *
     * @param linear factor for the linearVelocity
     * @param angular factor for the angularVelocity
     */
    public void setSleepingThresholds(float linear, float angular) {
        Native3DRigidBody.setSleepingThresholds(getNative(), linear, angular);
    }

    /**
     * Set a {@linkplain SXRRigidBody rigid body} to be ignored (true) or not (false)
     *
     * @param collisionObject rigidbody object on the collision check
     * @param ignore boolean to indicate if the specified object will be ignored or not
     */
    public void setIgnoreCollisionCheck(SXRRigidBody collisionObject, boolean ignore) {
        Native3DRigidBody.setIgnoreCollisionCheck(getNative(), collisionObject.getNative(), ignore);
    }

    /**
     * Returns the gravity acceleration float array [x,y,z] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The gravity acceleration vector as a float array
     */
    public float[] getGravity() {
        return Native3DRigidBody.getGravity(getNative());
    }

    /**
     * Returns the linear velocity float array [x,y,z] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The linear velocity vector as a float array
     */
    public float[] getLinearVelocity() {
        return Native3DRigidBody.getLinearVelocity(getNative());
    }

    /**
     * Returns the angular velocity float array [x,y,z] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The angular velocity vector as a float array
     */
    public float[] getAngularVelocity() {
        return Native3DRigidBody.getAngularVelocity(getNative());
    }

    /**
     * Returns the angular factor float array [x,y,z] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The angular factor vector as a float array
     */
    public float[] getAngularFactor() {
        return Native3DRigidBody.getAngularFactor(getNative());
    }

    /**
     * Returns the linear factor float array [x,y,z] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The linear factor vector as a float array
     */
    public float[] getLinearFactor() {
        return Native3DRigidBody.getLinearFactor(getNative());
    }

    /**
     * Returns the damping factors [angular,linear] on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The damping factors as a float array
     */
    public float[] getDamping() {
        return Native3DRigidBody.getDamping(getNative());
    }

    /**
     * Returns the friction factor on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The friction factor scalar as a float
     */
    public float getFriction() {
        return Native3DRigidBody.getFriction(getNative());
    }

    /**
     * Set the friction factor of this {@linkplain SXRRigidBody rigid body}
     *
     * @param n the friction factor
     */
    public void setFriction(float n) {
        Native3DRigidBody.setFriction(getNative(), n);
    }

    /**
     * Returns the restitution factor on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The restitution factor scalar as a float
     */
    public float getRestitution() {
        return Native3DRigidBody.getRestitution(getNative());
    }

    /**
     * Set the restitution factor of this {@linkplain SXRRigidBody rigid body}
     *
     * @param n the restitution factor
     */
    public void setRestitution(float n) {
        Native3DRigidBody.setRestitution(getNative(), n);
    }

    /**
     * Returns the continous collision detection motion threshold factor on this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The continous collision detection motion threshold factor scalar as a float
     */
    public float getCcdMotionThreshold() {
        return Native3DRigidBody.getCcdMotionThreshold(getNative());
    }

    /**
     * Set the continous collision detection motion threshold factor of this {@linkplain SXRRigidBody rigid body}
     *
     * @param n the continous collision detection motion threshold factor
     */
    public void setCcdMotionThreshold(float n) {
        Native3DRigidBody.setCcdMotionThreshold(getNative(), n);
    }

    /**
     * Returns the contact processing threshold factor for this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The contact processing threshold factor scalar as a float
     */
    public float getContactProcessingThreshold() {
        return Native3DRigidBody.getContactProcessingThreshold(getNative());
    }

    /**
     * Set the  contact processing threshold factor of this {@linkplain SXRRigidBody rigid body}
     *
     * @param n the contact processing threshold factor
     */
    public void setContactProcessingThreshold(float n) {
        Native3DRigidBody.setContactProcessingThreshold(getNative(), n);
    }

    /**
     * Returns the collision group of this {@linkplain SXRRigidBody rigid body}.
     *
     * @return The collision group id as an int
     */
    public int getCollisionGroup() {
        return mCollisionGroup;
    }

    /**
     * Reset the {@linkplain SXRRigidBody rigid body}. This API is intended mainly to adapt the
     * rigid body transform (position, rotation and scale) when the {@linkplain SXRNode
     * owner object} is changed.
     *
     * @param rebuildCollider rebuilds the physics collider if true.
     */
    public void reset(final boolean rebuildCollider) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                Native3DRigidBody.reset(getNative(), rebuildCollider);
            }
        });
    }

    /**
     * Same as {@linkplain SXRRigidBody#reset reset(true)}
     */
    public void reset() {
        reset(true);
    }

    @Override
    public void onAttach(SXRNode newOwner) {
        if (!mLoaded && newOwner.getCollider() == null) {
            throw new UnsupportedOperationException("You must have a collider attached to the node before attaching the rigid body");
        }
        final SXRRenderData renderData = newOwner.getRenderData();
        if (renderData != null && renderData.getMesh() == null) {
            throw new UnsupportedOperationException("You must have a mesh attached to the node before attaching the rigid body");
        }
        super.onAttach(newOwner);
    }

    @Override
    protected void addToWorld(SXRWorld world) {
        if (world != null) {
            world.addBody(this);
        }
    }

    @Override
    protected void removeFromWorld(SXRWorld world) {
        if (world != null) {
            world.removeBody(this);
        }
    }
}

class Native3DRigidBody {
    static native long ctor(float mass);

    static native long getComponentType();

    static native float getMass(long jrigid_body);

    static native void applyCentralForce(long jrigid_body, float x, float y, float z);

    static native void applyForce(long jrigid_body, float force_x, float force_y, float force_z,
                                  float rel_pos_x, float rel_pos_y, float rel_pos_z);

    static native void applyCentralImpulse(long jrigid_body, float x, float y, float z);

    static native void applyImpulse(long jrigid_body, float impulse_x, float impulse_y, float impulse_z,
                                  float rel_pos_x, float rel_pos_y, float rel_pos_z);

    static native void applyTorque(long jrigid_body, float x, float y, float z);

    static native void applyTorqueImpulse(long jrigid_body, float x, float y, float z);

    static native void setGravity(long jrigid_body, float x, float y, float z);

    static native void setDamping(long jrigid_body, float linear, float angular);

    static native void setLinearVelocity(long jrigid_body, float x, float y, float z);

    static native void setAngularVelocity(long jrigid_body, float x, float y, float z);

    static native void setAngularFactor(long jrigid_body, float x, float y, float z);

    static native void setLinearFactor(long jrigid_body, float x, float y, float z);

    static native void setFriction(long jrigid_body, float n);

    static native void setRestitution(long jrigid_body, float n);

    static native void setSleepingThresholds(long jrigid_body, float linear, float angular);

    static native void setCcdMotionThreshold(long jrigid_body, float n);

    static native void setContactProcessingThreshold(long jrigid_body, float n);

    static native void setIgnoreCollisionCheck(long jrigid_body, long jcollision_object, boolean ignore);

    static native float[] getGravity(long jrigid_body);

    static native float[] getLinearVelocity(long jrigid_body);

    static native float[] getAngularVelocity(long jrigid_body);

    static native float[] getAngularFactor(long jrigid_body);

    static native float[] getLinearFactor(long jrigid_body);

    static native float[] getDamping(long jrigid_body);

    static native float getFriction(long jrigid_body);

    static native float getRestitution(long jrigid_body);

    static native float getCcdMotionThreshold(long jrigid_body);

    static native float getContactProcessingThreshold(long jrigid_body);

    static native int getSimulationType(long jrigid_body);

    static native void setSimulationType(long jrigid_body, int jtype);

    static native void reset(long jrigid_body, boolean rebuildCollider);
}
