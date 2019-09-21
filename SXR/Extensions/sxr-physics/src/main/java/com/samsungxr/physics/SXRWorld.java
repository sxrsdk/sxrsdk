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

import android.os.SystemClock;
import android.transition.Scene;
import android.util.LongSparseArray;

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRComponentGroup;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRNode.ComponentVisitor;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderManager;
import com.samsungxr.SXRTransform;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.io.SXRCursorController;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physics world where all {@link SXRNode} with {@link SXRRigidBody} component
 * attached to are simulated.
 * <p>
 * {@link SXRWorld} is a component that must be attached to the scene's root object.
 */
public class SXRWorld extends SXRComponent implements IEventReceiver
{
    private final SXRPhysicsContext mPhysicsContext;
    private SXRWorldTask mWorldTask;
    private static final long DEFAULT_INTERVAL = 15;
    private SXREventReceiver mListeners;
    private boolean mIsMultibody = false;
    private boolean mDoDebugDraw = false;
    private List<SXRPhysicsJoint> mMultiBodies = null;

    private long mNativeLoader;

    static {
        System.loadLibrary("sxr-physics");
        System.loadLibrary("LinearMath");
        System.loadLibrary("BulletCollision");
        System.loadLibrary("BulletDynamics");
        System.loadLibrary("Bullet3Common");
        System.loadLibrary("Bullet3Collision");
        System.loadLibrary("Bullet3Dynamics");
        System.loadLibrary("Bullet3Geometry");
        System.loadLibrary("BulletSoftBody");
        System.loadLibrary("Bullet2FileLoader");
        System.loadLibrary("BulletWorldImporter");
    }

    private final LongSparseArray<SXRPhysicsWorldObject> mPhysicsObject = new LongSparseArray<SXRPhysicsWorldObject>();
    private final SXRCollisionMatrix mCollisionMatrix;

    private PhysicsDragger mPhysicsDragger = null;
    private SXRRigidBody mRigidBodyDragMe = null;

    /**
     * Events generated during physics simulation.
     * These are called from the physics thread.
     */
    public interface IPhysicsEvents extends IEvents
    {
        /**
         * Called when a joint is added to a physics world.
         * @param world  physics world the body is added to.
         * @param joint  joint added.
         */
        public void onAddJoint(final SXRWorld world, final SXRPhysicsJoint joint);

        /**
         * Called when a joint is removed from a physics world.
         * @param world  physics world the body is removed from.
         * @param joint  joint removed.
         */
        public void onRemoveJoint(final SXRWorld world, final SXRPhysicsJoint joint);

        /**
         * Called when a rigid body is added to a physics world.
         * @param world physics world the body is added to.
         * @param body  rigid body added.
         */
        public void onAddRigidBody(final SXRWorld world, final SXRRigidBody body);

        /**
         * Called when a rigid body is removed from a physics world.
         * @param world physics world the body is removed from.
         * @param body  rigid body removed.
         */
        public void onRemoveRigidBody(final SXRWorld world, final SXRRigidBody body);

        /**
         * called when a constraint is added to a physics world.
         * @param constraint {@link SXRConstraint} that was added.
         */
        public void onAddConstraint(final SXRWorld world, final SXRConstraint constraint);

        /**
         * called when a constraint is removed from a physics world.
         * @param constraint {@link SXRConstraint} that was removed.
         */
        public void onRemoveConstraint(final SXRWorld world, final SXRConstraint constraint);

        /**
         * Called after each iteration of the physics simulation.
         * @param world physics world being simulated
         */
        public void onStepPhysics(final SXRWorld world);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param scene The {@link SXRScene} this world belongs to.
     */
    public SXRWorld(SXRScene scene) {
        this(scene, null, false);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param scene The {@link SXRScene} this world belongs to.
     */
    public SXRWorld(SXRScene scene, boolean isMultiBody) {
        this(scene, null, isMultiBody);
    }


    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param scene The {@link SXRScene} this world belongs to.
     * @param interval interval (in milliseconds) at which the collisions will be updated.
     */
    public SXRWorld(SXRScene scene, long interval) {
        this(scene, null, interval, false);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene. Defaults to a 15ms
     * update interval.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix) {
        this(scene, collisionMatrix, DEFAULT_INTERVAL, false);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene. Defaults to a 15ms
     * update interval.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     * @param isMultiBody     use MultiBody dynamics
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix, boolean isMultiBody) {
        this(scene, collisionMatrix, DEFAULT_INTERVAL, isMultiBody);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene.
     * @param interval        interval (in milliseconds) at which the collisions will be updated.
     * @param isMultiBody     use MultiBody dynamics
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix, long interval, boolean isMultiBody)
    {
        super(scene.getSXRContext(), NativePhysics3DWorld.ctor(isMultiBody));
        mIsEnabled = false;
        mIsMultibody = isMultiBody;
        mListeners = new SXREventReceiver(this);
        mCollisionMatrix = collisionMatrix;
        mWorldTask = new SXRWorldTask(interval);
        mPhysicsContext = SXRPhysicsContext.getInstance();
        if (isMultiBody)
        {
            mMultiBodies = new ArrayList<SXRPhysicsJoint>();
        }
        scene.getRoot().attachComponent(this);
    }

    /**
     * Enables or disabled debug drawing of the physics world.
     * @param mode Bullet debug draw mode
     */
    public void setDebugMode(int mode)
    {
        NativePhysics3DWorld.setDebugMode(getNative(), mode);
        mDoDebugDraw = (mode != 0);
    }

    /***
     * Indicates which controller should be used for dragging.
     * <p>
     * The drag is performed on the physics thread
     * and is synchronized with physics.
     * You can use {@link SXRCursorController#startDrag(SXRNode)} for
     * dragging but it does not run on the physics thread and
     * you may get unexpected results for objects being
     * moved by physics
     * @param controller {@link SXRCursorController} to use for dragging,
     *                   If null, dragging is disabled.
     * @see #startDrag(SXRNode, float, float, float)
     */
    public void setDragController(SXRCursorController controller)
    {
        if (controller != null)
        {
            mPhysicsDragger = new PhysicsDragger(controller);
        }
        else
        {
            mPhysicsDragger = null;
        }
    }

    static public long getComponentType() {
        return NativePhysics3DWorld.getComponentType();
    }

    public boolean isMultiBody() { return mIsMultibody; }

    public SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Add a {@link SXRConstraint} to this physics world.
     *
     * @param gvrConstraint The {@link SXRConstraint} to add.
     */
    public void addConstraint(final SXRConstraint gvrConstraint)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(gvrConstraint))
                {
                    return;
                }
                SXRPhysicsCollidable bodyB = gvrConstraint.mBodyB;
                SXRPhysicsCollidable bodyA = gvrConstraint.mBodyA;

                if ((bodyB != null) && (bodyB instanceof SXRRigidBody))
                {
                    if (!contains(bodyB) || ((bodyA != null) && !contains(bodyA)))
                    {
                        throw new UnsupportedOperationException("Rigid body used by constraint is not found in the physics world.");
                    }
                }
                NativePhysics3DWorld.addConstraint(getNative(), gvrConstraint.getNative());
                mPhysicsObject.put(gvrConstraint.getNative(), gvrConstraint);
                getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onAddConstraint", SXRWorld.this, gvrConstraint);
            }
        });
    }

    /**
     * Remove a {@link SXRFixedConstraint} from this physics world.
     *
     * @param gvrConstraint the {@link SXRFixedConstraint} to remove.
     */
    public void removeConstraint(final SXRConstraint gvrConstraint) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (contains(gvrConstraint)) {
                    NativePhysics3DWorld.removeConstraint(getNative(), gvrConstraint.getNative());
                    mPhysicsObject.remove(gvrConstraint.getNative());
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onRemoveConstraint", SXRWorld.this, gvrConstraint);
                }
            }
        });
    }

    /**
     * Start the drag operation of a node with a rigid body.
     * <p>
     * The drag operation is run on the physics thread and is
     * synchronized with physics. It should be used instead of
     * {@link SXRCursorController#startDrag(SXRNode)} for objects
     * which are controlled by physics.
     * <p>
     * To enable dragging, you need to call {@link #setDragController(SXRCursorController)}
     * to indicate which controller to use for dragging.
     * @param sceneObject Scene object with a rigid body attached to it.
     * @param hitX rel position in x-axis.
     * @param hitY rel position in y-axis.
     * @param hitZ rel position in z-axis.
     * @return true if success, otherwise returns false.
     * @throws UnsupportedOperationException if no controller has been specified
     * @see #stopDrag()
     * @see #setDragController(SXRCursorController)
     * @see SXRCursorController#startDrag(SXRNode)
     */
    public boolean startDrag(final SXRNode sceneObject,
                             final float hitX, final float hitY, final float hitZ)
    {
        if (mPhysicsDragger == null)
        {
            throw new UnsupportedOperationException("You must call selectDragController before dragging");
        }
        final SXRRigidBody dragMe = (SXRRigidBody)sceneObject.getComponent(SXRRigidBody.getComponentType());
        if (dragMe == null || dragMe.getSimulationType() != SXRRigidBody.DYNAMIC || !contains(dragMe))
            return false;

        SXRTransform t = sceneObject.getTransform();

        final Vector3f relPos = new Vector3f(hitX, hitY, hitZ);
        relPos.mul(t.getScaleX(), t.getScaleY(), t.getScaleZ());
        relPos.rotate(new Quaternionf(t.getRotationX(), t.getRotationY(), t.getRotationZ(), t.getRotationW()));

        final SXRNode pivotObject = mPhysicsDragger.startDrag(sceneObject,
                relPos.x, relPos.y, relPos.z);
        if (pivotObject == null)
            return false;

        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                mRigidBodyDragMe = dragMe;
                NativePhysics3DWorld.startDrag(getNative(), pivotObject.getNative(), dragMe.getNative(),
                        hitX, hitY, hitZ);
            }
        });

        return true;
    }

    /**
     * Stop dragging the current object being dragged.
     * <p>
     * The drag operation is stopped on the physics thread and is
     * synchronized with physics. It should be used instead of
     * {@link SXRCursorController#stopDrag()} for objects
     * which are controlled by physics.
     * <p>
     * To enable dragging, you need to call {@link #setDragController(SXRCursorController)}
     * to indicate which controller to use for dragging.
     * If dragging is not enabled or nothing is being dragged,
     * this function just returns.
     * @see #startDrag(SXRNode, float, float, float)
     * @see #setDragController(SXRCursorController)
     * @see SXRCursorController#stopDrag()
     */
    public void stopDrag() {
        if (mPhysicsDragger == null)
        {
            return;
        }
        mPhysicsDragger.stopDrag();

        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (mRigidBodyDragMe != null) {
                    NativePhysics3DWorld.stopDrag(getNative());
                    mRigidBodyDragMe = null;
                }
            }
        });
    }

    /**
     * Returns true if the physics world contains the the specified rigid body.
     *
     * @param physicsObject Physics object to check if it is present in the world.
     * @return true if the world contains the specified object.
     */
    private boolean contains(SXRPhysicsWorldObject physicsObject) {
        if (physicsObject != null)
        {
            return mPhysicsObject.get(physicsObject.getNative()) != null;
        }
        return false;
    }

    /**
     * Add a {@link SXRRigidBody} to this physics world.
     *
     * @param gvrBody The {@link SXRRigidBody} to add.
     */
    public void addBody(final SXRRigidBody gvrBody) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (contains(gvrBody)) {
                    return;
                }

                if (gvrBody.getCollisionGroup() < 0 || gvrBody.getCollisionGroup() > 15
                        || mCollisionMatrix == null) {
                    NativePhysics3DWorld.addRigidBody(getNative(), gvrBody.getNative());
                } else {
                    NativePhysics3DWorld.addRigidBodyWithMask(getNative(), gvrBody.getNative(),
                            mCollisionMatrix.getCollisionFilterGroup(gvrBody.getCollisionGroup()),
                            mCollisionMatrix.getCollisionFilterMask(gvrBody.getCollisionGroup()));
                }

                mPhysicsObject.put(gvrBody.getNative(), gvrBody);
                getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onAddRigidBody", SXRWorld.this, gvrBody);
            }
        });
    }

    /**
     * Add a {@link SXRPhysicsJoint} root to this physics world.
     *
     * @param body The {@link SXRPhysicsJoint} to add.
     */
    public void addBody(final SXRPhysicsJoint body)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run()
            {
                if (contains(body))
                {
                    return;
                }
                NativePhysics3DWorld.addJoint(getNative(), body.getNative());
                mPhysicsObject.put(body.getNative(), body);
                if (body.getBoneID() == 0)
                {
                    mMultiBodies.add(body);
                }
                getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onAddJoint", SXRWorld.this, body);
            }
        });
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     *
     * @param gvrBody the {@link SXRRigidBody} to remove.
     */
    public void removeBody(final SXRRigidBody gvrBody) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (contains(gvrBody)) {
                    NativePhysics3DWorld.removeRigidBody(getNative(), gvrBody.getNative());
                    mPhysicsObject.remove(gvrBody.getNative());
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onRemoveRigidBody", SXRWorld.this, gvrBody);
                }
            }
        });
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     *
     * @param joint the {@link SXRPhysicsJoint} to remove.
     */
    public void removeBody(final SXRPhysicsJoint joint) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (contains(joint)) {
                    NativePhysics3DWorld.removeJoint(getNative(), joint.getNative());
                    mPhysicsObject.remove(joint.getNative());
                    if (joint.getBoneID() == 0)
                    {
                        mMultiBodies.remove(joint);
                    }
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onRemoveJoint", SXRWorld.this, joint);
                }
            }
        });
    }

    private void startSimulation() {
        mWorldTask.start();
    }

    private void stopSimulation() {
        mWorldTask.stop();
    }

    private void generateCollisionEvents() {
        SXRCollisionInfo collisionInfos[] = NativePhysics3DWorld.listCollisions(getNative());

        String onEnter = "onEnter";
        String onExit = "onExit";

        for (SXRCollisionInfo info : collisionInfos) {
            if (info.isHit) {
                sendCollisionEvent(info, onEnter);
            } else if (mPhysicsObject.get(info.bodyA) != null
                    && mPhysicsObject.get(info.bodyB) != null) {
                // If both bodies are in the scene.
                sendCollisionEvent(info, onExit);
            }
        }

    }

    private void sendCollisionEvent(SXRCollisionInfo info, String eventName) {
        SXRNode bodyA = mPhysicsObject.get(info.bodyA).getOwnerObject();
        SXRNode bodyB = mPhysicsObject.get(info.bodyB).getOwnerObject();

        getSXRContext().getEventManager().sendEvent(bodyA, ICollisionEvents.class, eventName,
                bodyA, bodyB, info.normal, info.distance);

        getSXRContext().getEventManager().sendEvent(bodyB, ICollisionEvents.class, eventName,
                bodyB, bodyA, info.normal, info.distance);

        getSXRContext().getEventManager().sendEvent(this, ICollisionEvents.class, eventName,
                                                    bodyA, bodyB, info.normal, info.distance);

    }

    private void doPhysicsAttach(SXRNode rootNode) {
        rootNode.forAllComponents(mRigidBodiesVisitor, SXRRigidBody.getComponentType());
        rootNode.forAllComponents(mConstraintsVisitor, SXRConstraint.getComponentType());

        if (isEnabled()){
            startSimulation();
        }
    }

    private void doPhysicsDetach(SXRNode rootNode) {
        rootNode.forAllComponents(mConstraintsVisitor, SXRConstraint.getComponentType());
        rootNode.forAllComponents(mRigidBodiesVisitor, SXRRigidBody.getComponentType());

        if (isEnabled()) {
            stopSimulation();
        }
    }

    @Override
    public void onAttach(SXRNode newOwner) {
        super.onAttach(newOwner);

        //FIXME: Implement a way to check if already exists a SXRWold attached to the scene
        if (newOwner.getParent() != null) {
            throw new RuntimeException("SXRWold must be attached to the scene's root object!");
        }

        doPhysicsAttach(newOwner);
    }

    @Override
    public void onDetach(SXRNode oldOwner) {
        super.onDetach(oldOwner);

        doPhysicsDetach(oldOwner);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (getOwnerObject() != null) {
            startSimulation();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        stopSimulation();
    }

    public void setGravity(final float x, final float y, final float z) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                NativePhysics3DWorld.setGravity(getNative(), x, y, z);
            }
        });
    }

    public void getGravity(float[] gravity) {
        NativePhysics3DWorld.getGravity(getNative(), gravity);
    }


    private class SXRWorldTask implements Runnable {
        private boolean running = false;
        private final long intervalMillis;
        private float timeStep;
        private int maxSubSteps;
        private long simulationTime;
        private long lastSimulTime;
        private Runnable debugDrawTask = new Runnable()
        {
            @Override
            public void run()
            {
                {
                    debugDrawWorld();
                }
            }
        };

        public SXRWorldTask(long milliseconds) {
            intervalMillis = milliseconds;
        }

        @Override
        public void run() {
            if (!running) {
                return;
            }


            simulationTime = SystemClock.uptimeMillis();

            /* To debug physics step
            if (BuildConfig.DEBUG && timeStep != simulationTime - lastSimulTime) {
                Log.v("SXRPhysicsWorld", "onStep " + timeStep + "ms" + ", subSteps " + maxSubSteps);
            }*/

            timeStep  = simulationTime - lastSimulTime;
            maxSubSteps = (int) (timeStep * 60) / 1000 + 1;

            NativePhysics3DWorld.step(getNative(), timeStep, maxSubSteps);

            if (mDoDebugDraw)
            {
                getSXRContext().runOnGlThread(debugDrawTask);
            }
            generateCollisionEvents();
            if (mIsMultibody)
            {
                for (SXRPhysicsJoint joint : mMultiBodies)
                {
                    joint.onStep();
                }
            }
            getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onStepPhysics", SXRWorld.this);
            lastSimulTime = simulationTime;

            simulationTime += intervalMillis - SystemClock.uptimeMillis();
            if (simulationTime < 0) {
                simulationTime += intervalMillis;
            }
            // Time of the next simulation;
            simulationTime += simulationTime +SystemClock.uptimeMillis();

            mPhysicsContext.runAtTimeOnPhysicsThread(this, simulationTime);
        }

        public void start() {
            // To avoid concurrency
            mPhysicsContext.runOnPhysicsThread(new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        running = true;
                        lastSimulTime = SystemClock.uptimeMillis();
                        mPhysicsContext.runDelayedOnPhysicsThread(SXRWorldTask.this,
                                intervalMillis);
                    }
                }
            });
        }

        public void stop() {
            // To avoid concurrency
            mPhysicsContext.runOnPhysicsThread(new Runnable() {
                @Override
                public void run() {
                    if (running) {
                        running = false;
                        mPhysicsContext.removeTask(SXRWorldTask.this);
                    }
                }
            });
        }
    }


    private ComponentVisitor mRigidBodiesVisitor = new ComponentVisitor() {

        @Override
        public boolean visit(SXRComponent gvrComponent) {
            if (!gvrComponent.isEnabled()) {
                return false;
            }

            if (SXRWorld.this.owner != null) {
                addBody((SXRRigidBody) gvrComponent);
            } else {
                removeBody((SXRRigidBody) gvrComponent);
            }
            return true;
        }
    };

    private ComponentVisitor mConstraintsVisitor = new ComponentVisitor() {

        @Override
        public boolean visit(SXRComponent gvrComponent) {
            if (!gvrComponent.isEnabled()) {
                return false;
            }

            SXRComponentGroup<SXRConstraint> group = (SXRComponentGroup) gvrComponent;

            if (group == null) {
                if (SXRWorld.this.owner != null) {
                    addConstraint((SXRConstraint) gvrComponent);
                } else {
                    removeConstraint((SXRConstraint) gvrComponent);
                }
            } else for (SXRConstraint constraint: group) {
                if (SXRWorld.this.owner != null) {
                    addConstraint(constraint);
                } else {
                    removeConstraint(constraint);
                }
            }

            return true;
        }
    };

    public void setupDebugDraw(SXRScene scene, SXRShaderManager shaderManager)
    {
        NativePhysics3DWorld.setupDebugDraw(getNative(), scene.getNative(), shaderManager.getNative());
    }

    public void debugDrawWorld()
    {
        NativePhysics3DWorld.debugDrawWorld(getNative());
    }
}

class NativePhysics3DWorld {
    static native long ctor(boolean isMultiBody);

    static native long getComponentType();

    static native void addConstraint(long jphysics_world, long jconstraint);

    static native void removeConstraint(long jphysics_world, long jconstraint);

    static native void startDrag(long jphysics_world, long jdragger, long jtarget,
                                 float relX, float relY, float relZ);

    static native void stopDrag(long jphysics_world);

    static native void addJoint(long jphysics_world, long jjoint);

    static native void removeJoint(long jphysics_world, long jjoint);

    static native void addRigidBody(long jphysics_world, long jrigid_body);

    static native void addRigidBodyWithMask(long jphysics_world, long jrigid_body, long collisionType, long collidesWith);

    static native void removeRigidBody(long jphysics_world, long jrigid_body);

    static native void step(long jphysics_world, float jtime_step, int maxSubSteps);

    static native void getGravity(long jworld, float[] array);

    static native void setGravity(long jworld, float x, float y, float z);

    static native SXRCollisionInfo[] listCollisions(long jphysics_world);

    static native void setupDebugDraw(long jworld, long jscene, long jshader_manager);

    static native void debugDrawWorld(long jworld);

    static native void setDebugMode(long jworld, int mode);

}
