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
import android.util.LongSparseArray;

import com.samsungxr.SXRComponent;
import com.samsungxr.SXRComponentGroup;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRNode.ComponentVisitor;
import com.samsungxr.SXRTransform;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.INodeEvents;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents a physics world where all {@link SXRNode} with {@link SXRRigidBody} component
 * attached to are simulated.
 * <p>
 * {@link SXRWorld} is a component that must be attached to the scene's root object.
 */
public class SXRWorld extends SXRComponent implements IEventReceiver
{
    private boolean mInitialized;
    private final SXRPhysicsContext mPhysicsContext;
    private SXRWorldTask mWorldTask;
    private static final long DEFAULT_INTERVAL = 15;
    private SXREventReceiver mListeners;

    private long mNativeLoader;

    static {
        System.loadLibrary("gvrf-physics");
    }

    private final LongSparseArray<SXRPhysicsWorldObject> mPhysicsObject = new LongSparseArray<SXRPhysicsWorldObject>();
    private final SXRCollisionMatrix mCollisionMatrix;

    private final PhysicsDragger mPhysicsDragger;
    private SXRRigidBody mRigidBodyDragMe = null;

    /**
     * Events generated during physics simulation.
     * These are called from the physics thread.
     */
    public interface IPhysicsEvents extends IEvents
    {
        /**
         * Called when a rigid body is added to a physics world.
         * @param world physics world the body is added to.
         * @param body  rigid body added.
         */
        public void onAddRigidBody(SXRWorld world, SXRRigidBody body);

        /**
         * Called when a rigid body is removed from a physics world.
         * @param world physics world the body is removed from.
         * @param body  rigid body removed.
         */
        public void onRemoveRigidBody(SXRWorld world, SXRRigidBody body);

        /**
         * Called after each iteration of the physics simulation.
         * @param world physics world being simulated
         */
        public void onStepPhysics(SXRWorld world);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param gvrContext The context of the app.
     */
    public SXRWorld(SXRContext gvrContext) {
        this(gvrContext, null);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param gvrContext The context of the app.
     * @param interval interval (in milliseconds) at which the collisions will be updated.
     */
    public SXRWorld(SXRContext gvrContext, long interval) {
        this(gvrContext, null, interval);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene. Defaults to a 15ms
     * update interval.
     *
     * @param gvrContext The context of the app.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     */
    public SXRWorld(SXRContext gvrContext, SXRCollisionMatrix collisionMatrix) {
        this(gvrContext, collisionMatrix, DEFAULT_INTERVAL);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene.
     *
     * @param gvrContext The context of the app.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     * @param interval interval (in milliseconds) at which the collisions will be updated.
     */
    public SXRWorld(SXRContext gvrContext, SXRCollisionMatrix collisionMatrix, long interval) {
        super(gvrContext, NativePhysics3DWorld.ctor());
        mListeners = new SXREventReceiver(this);
        mPhysicsDragger = new PhysicsDragger(gvrContext);
        mInitialized = false;
        mCollisionMatrix = collisionMatrix;
        mWorldTask = new SXRWorldTask(interval);
        mPhysicsContext = SXRPhysicsContext.getInstance();
    }

    static public long getComponentType() {
        return NativePhysics3DWorld.getComponentType();
    }

    public SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Add a {@link SXRConstraint} to this physics world.
     *
     * @param gvrConstraint The {@link SXRConstraint} to add.
     */
    public void addConstraint(final SXRConstraint gvrConstraint) {
        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run() {
                if (contains(gvrConstraint)) {
                    return;
                }

                if (!contains(gvrConstraint.mBodyA)
                        || (gvrConstraint.mBodyB != null && !contains(gvrConstraint.mBodyB))) {
                    throw new UnsupportedOperationException("Rigid body not found in the physics world.");
                }

                NativePhysics3DWorld.addConstraint(getNative(), gvrConstraint.getNative());
                mPhysicsObject.put(gvrConstraint.getNative(), gvrConstraint);
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
                }
            }
        });
    }

    /**
     * Start the drag operation of a scene object with a rigid body.
     *
     * @param sceneObject Scene object with a rigid body attached to it.
     * @param hitX rel position in x-axis.
     * @param hitY rel position in y-axis.
     * @param hitZ rel position in z-axis.
     * @return true if success, otherwise returns false.
     */
    public boolean startDrag(final SXRNode sceneObject,
                             final float hitX, final float hitY, final float hitZ) {
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
     * Stop the drag action.
     */
    public void stopDrag() {
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
        return mPhysicsObject.get(physicsObject.getNative()) != null;
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
    }

    private void doPhysicsAttach(SXRNode rootNode) {
        rootNode.forAllComponents(mRigidBodiesVisitor, SXRRigidBody.getComponentType());
        rootNode.forAllComponents(mConstraintsVisitor, SXRConstraint.getComponentType());

        if (!mInitialized) {
            rootNode.getEventReceiver().addListener(mSceneEventsHandler);
        } else if (isEnabled()){
            startSimulation();
        }
    }

    private void doPhysicsDetach(SXRNode rootNode) {
        rootNode.forAllComponents(mConstraintsVisitor, SXRConstraint.getComponentType());
        rootNode.forAllComponents(mRigidBodiesVisitor, SXRRigidBody.getComponentType());

        if (!mInitialized) {
            rootNode.getEventReceiver().removeListener(mSceneEventsHandler);
        }
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

        if (getOwnerObject() != null && mInitialized) {
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

            generateCollisionEvents();
            getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onStepPhysics", SXRWorld.this);

            lastSimulTime = simulationTime;

            simulationTime = intervalMillis + simulationTime - SystemClock.uptimeMillis();
            if (simulationTime < 0) {
                simulationTime += intervalMillis;
            }
            // Time of the next simulation;
            simulationTime = simulationTime + SystemClock.uptimeMillis();

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

    private INodeEvents mSceneEventsHandler = new INodeEvents() {

        @Override
        public void onInit(SXRContext gvrContext, SXRNode sceneObject) {
            if (mInitialized)
                return;

            mInitialized = true;
            getOwnerObject().getEventReceiver().removeListener(this);

            if (isEnabled()) {
                startSimulation();
            }
        }

        @Override
        public void onLoaded() {}

        @Override
        public void onAfterInit() {}

        @Override
        public void onStep() {}
    };

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
}

class NativePhysics3DWorld {
    static native long ctor();

    static native long getComponentType();

    static native boolean addConstraint(long jphysics_world, long jconstraint);

    static native boolean removeConstraint(long jphysics_world, long jconstraint);

    static native void startDrag(long jphysics_world, long jdragger, long jtarget,
                                 float relX, float relY, float relZ);

    static native void stopDrag(long jphysics_world);

    static native boolean addRigidBody(long jphysics_world, long jrigid_body);

    static native boolean addRigidBodyWithMask(long jphysics_world, long jrigid_body, long collisionType, long collidesWith);

    static native void removeRigidBody(long jphysics_world, long jrigid_body);

    static native void step(long jphysics_world, float jtime_step, int maxSubSteps);

    static native void getGravity(long jworld, float[] array);

    static native void setGravity(long jworld, float x, float y, float z);

    static native SXRCollisionInfo[] listCollisions(long jphysics_world);
}
