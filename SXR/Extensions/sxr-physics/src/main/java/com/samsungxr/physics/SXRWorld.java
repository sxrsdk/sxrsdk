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

import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventManager;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShaderId;
import com.samsungxr.SXRTransform;
import com.samsungxr.SXRVertexBuffer;
import com.samsungxr.io.SXRCursorController;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_LINES;

/**
 * Represents a physics simulation world where nodes can attach rigid body,
 * joint and constraint components to describe physics properties.
 * <p>
 *  Rigid body dynamics with constraints are supported on top of the
 *  Bullet Physics engine. Articulated bodies, like avatars,
 *  are supported by the Featherstone multi body simulator
 *  inside Bullet.
 * <p>
 * <p>
 * Simulation is performed in a separate thread that is not
 * sychronized with rendering. All physics operations which
 * modify the simulation world are performed on the physics thread.
 * </p>
 * This component is automatically attached to the root of the
 * scene it is simulating. It is an event receiver which can
 * attach listeners for events involving physics (adding or
 * removing components, notification after each simulation step).
 * @see IPhysicsEvents
 * @see SXRPhysicsContent
 * @see SXRRigidBody
 * @See SXRPhysicsJoint
 * @see SXRConstraint
 * @see SXREventReceiver
 */
public class SXRWorld extends SXRPhysicsContent implements IEventReceiver
{
    private final SXRPhysicsContext mPhysicsContext = SXRPhysicsContext.getInstance();
    private SXRWorldTask mWorldTask;
    private static final long DEFAULT_INTERVAL = 15;
    private SXREventReceiver mListeners;
    private boolean mDoDebugDraw = false;
    private final SXRCollisionMatrix mCollisionMatrix;
    private PhysicsDragger mPhysicsDragger = null;
    private SXRRigidBody mRigidBodyDragMe = null;

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
     * Constructs simulation world and attaches it to the
     * root of the specified scene. Multibody support
     * is not enabled and the update4 interval is 15ms by default.
     *
     * @param scene The {@link SXRScene} this world belongs to.
     */
    public SXRWorld(SXRScene scene)
    {
        this(scene, null, false);
    }

    /**
     * Constructs simulation world and attaches it to the
     * root of the specified scene. Defaults to a 15ms update interval.
     *
     * @param scene         The {@link SXRScene} this world belongs to.
     * @param isMultiBody   import articulated bodies as {@link SXRPhysicsJoint} components
     *                      and use Featherstone multibody simulation. The default is to
     *                      import everything as {@link SXRRigidBody} components and
     *                      use discrete dynamics.
     */
    public SXRWorld(SXRScene scene, boolean isMultiBody)
    {
        this(scene, null, isMultiBody);
    }


    /**
     * Constructs simulation world and attaches it to the
     * root of the specified scene. Multibody support is not enabled.
     *
     * @param scene The {@link SXRScene} this world belongs to.
     * @param interval interval (in milliseconds) at which the collisions will be updated.
     */
    public SXRWorld(SXRScene scene, long interval)
    {
        this(scene, null, interval, false);
    }

    /**
     * Constructs simulation world and attaches it to the
     * root of the specified scene. Multibody support
     * is not enabled and the update4 interval is 15ms by default.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     * @see SXRCollisionMatrix
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix)
    {
        this(scene, collisionMatrix, DEFAULT_INTERVAL, false);
    }

    /**
     * Constructs new instance to simulate the Physics World of the Scene. Defaults to a 15ms
     * update interval.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene
     * @param isMultiBody     import articulated bodies as {@link SXRPhysicsJoint} components
     *                        and use Featherstone multibody simulation. The default is to
     *                        import everything as {@link SXRRigidBody} components and
     *                        use discrete dynamics.
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix, boolean isMultiBody)
    {
        this(scene, collisionMatrix, DEFAULT_INTERVAL, isMultiBody);
    }

    /**
     * Constructs simulation world and attaches it to the
     * root of the specified scene.
     *
     * @param scene           The {@link SXRScene} this world belongs to.
     * @param collisionMatrix a matrix that represents the collision relations of the bodies on the scene.
     * @param interval        interval (in milliseconds) at which the collisions will be updated.
     * @param isMultiBody     import articulated bodies as {@link SXRPhysicsJoint} components
     *                        and use Featherstone multibody simulation. The default is to
     *                        import everything as {@link SXRRigidBody} components and
     *                        use discrete dynamics.
     */
    public SXRWorld(SXRScene scene, SXRCollisionMatrix collisionMatrix, long interval, boolean isMultiBody)
    {
        super(scene.getRoot(), isMultiBody);
        mListeners = new SXREventReceiver(this);
        mCollisionMatrix = collisionMatrix;
        mWorldTask = new SXRWorldTask(interval);
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

    /**
     * Get the {@link SXREventReceiver} which dispatches physics
     * events to listeners.
     * @return {@link SXREventReceiver} which emits events for this world.
     */
    public SXREventReceiver getEventReceiver() { return mListeners; }

    /**
     * Merge the input physics world with this one.
     * There is only one simulation world but other
     * worlds can be created when files containing
     * physics are imported. This function merges
     * an imported world with the simulation world.
     * <p>
     * All of the physics components are detached from the
     * input world and attached to this simulation world.
     * @see SXRPhysicsContent
     */
    public void merge(SXRPhysicsContent world)
    {
        SXRNode inputRoot = world.getOwnerObject();
        if (inputRoot == null)
        {
            throw new IllegalArgumentException("The input physics world does not have any scene objects");
        }
        inputRoot.detachComponent(SXRWorld.getComponentType());
        doPhysicsAttach(inputRoot);
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
    public void stopDrag()
    {
        if (mPhysicsDragger == null)
        {
            return;
        }
        mPhysicsDragger.stopDrag();

        mPhysicsContext.runOnPhysicsThread(new Runnable() {
            @Override
            public void run()
            {
                if (mRigidBodyDragMe != null)
                {
                    NativePhysics3DWorld.stopDrag(getNative());
                    mRigidBodyDragMe = null;
                }
            }
        });
    }


    /**
     * Add a {@link SXRRigidBody} to this physics world.
     * <p>
     * Calls the {@link IPhysicsEvents#onAddRigidBody(SXRWorld, SXRRigidBody)} function
     * for all listeners from the physics thread.
     * @param body The {@link SXRRigidBody} to add.
     */
    public void addBody(final SXRRigidBody body)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(body))
                {
                    return;
                }

                if ((body.getCollisionGroup() < 0) ||
                    (body.getCollisionGroup() > 15) ||
                    (mCollisionMatrix == null))
                {
                    NativePhysics3DWorld.addRigidBody(getNative(), body.getNative());
                }
                else
                {
                    NativePhysics3DWorld.addRigidBodyWithMask(getNative(), body.getNative(),
                            mCollisionMatrix.getCollisionFilterGroup(body.getCollisionGroup()),
                            mCollisionMatrix.getCollisionFilterMask(body.getCollisionGroup()));
                }
                mPhysicsObject.put(body.getNative(), body);
                getSXRContext().getEventManager().sendEvent(SXRWorld.this,
                        IPhysicsEvents.class,
                       "onAddRigidBody",
                       SXRWorld.this,
                        body);
            }
        });
    }

    /**
     * Add a {@link SXRPhysicsJoint} root to this physics world.
     * <p>
     * Calls the {@link IPhysicsEvents#onAddJoint(SXRWorld, SXRPhysicsJoint)} function
     * for all listeners from the physics thread.
     * @param joint The {@link SXRPhysicsJoint} to add.
     */
    public void addBody(final SXRPhysicsJoint joint)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(joint))
                {
                    return;
                }
                if ((joint.getCollisionGroup() < 0) ||
                    (joint.getCollisionGroup() > 15) ||
                    (mCollisionMatrix == null))
                {
                    NativePhysics3DWorld.addJoint(getNative(), joint.getNative());
                }
                else
                {
                    NativePhysics3DWorld.addJointWithMask(getNative(), joint.getNative(),
                            mCollisionMatrix.getCollisionFilterGroup(joint.getCollisionGroup()),
                            mCollisionMatrix.getCollisionFilterMask(joint.getCollisionGroup()));
                }
                mPhysicsObject.put(joint.getNative(), joint);
                getSXRContext().getEventManager().sendEvent(SXRWorld.this,
                        IPhysicsEvents.class,
                        "onAddJoint",
                        SXRWorld.this, joint);
            }
        });
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     * <p>
     * Calls the {@link IPhysicsEvents#onRemoveRigidBody(SXRWorld, SXRRigidBody)} function
     * for all listeners from the physics thread.
     * @param body the {@link SXRRigidBody} to remove.
     */
    public void removeBody(final SXRRigidBody body)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(body))
                {
                    NativePhysics3DWorld.removeRigidBody(getNative(), body.getNative());
                    mPhysicsObject.remove(body.getNative());
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this, IPhysicsEvents.class, "onRemoveRigidBody", SXRWorld.this, body);
                }
            }
        });
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     * <p>
     * Calls the {@link IPhysicsEvents#onRemoveJoint(SXRWorld, SXRPhysicsJoint)} function
     * for all listeners from the physics thread.
     * @param joint the {@link SXRPhysicsJoint} to remove.
     */
    public void removeBody(final SXRPhysicsJoint joint)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(joint))
                {
                    NativePhysics3DWorld.removeJoint(getNative(), joint.getNative());
                    mPhysicsObject.remove(joint.getNative());
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this,
                            IPhysicsEvents.class,
                            "onRemoveJoint",
                            SXRWorld.this,
                            joint);
                }
            }
        });
    }

    /**
     * Add a {@link SXRConstraint} to this physics world.
     * Calls the {@link IPhysicsEvents#onAddConstraint(SXRWorld, SXRConstraint)} function
     * for all listeners from the physics thread.
     *
     * @param constraint The {@link SXRConstraint} to add.
     */
    @Override
    public void addConstraint(final SXRConstraint constraint)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                SXRPhysicsCollidable bodyB = constraint.mBodyB;
                SXRPhysicsCollidable bodyA = constraint.mBodyA;

                if (bodyB != null)
                {
                    if (!contains(bodyB) || ((bodyA != null) && !contains(bodyA)))
                    {
                        throw new UnsupportedOperationException("Rigid body used by constraint is not found in the physics world.");
                    }
                }
                NativePhysics3DWorld.addConstraint(getNative(), constraint.getNative());
                mPhysicsObject.put(constraint.getNative(), constraint);
                getSXRContext().getEventManager().sendEvent(SXRWorld.this,
                        IPhysicsEvents.class,
                        "onAddConstraint",
                        SXRWorld.this,
                        constraint);
            }
        });
    }

    /**
     * Remove a {@link SXRConstraint} from this physics world.
     * <p>
     * Calls the {@link IPhysicsEvents#onRemoveConstraint(SXRWorld, SXRConstraint)} function
     * for all listeners from the physics thread.
     * @param constraint the {@link SXRConstraint} to remove.
     */
    public void removeConstraint(final SXRConstraint constraint)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (contains(constraint))
                {
                    NativePhysics3DWorld.removeConstraint(getNative(), constraint.getNative());
                    mPhysicsObject.remove(constraint.getNative());
                    getSXRContext().getEventManager().sendEvent(SXRWorld.this,
                            IPhysicsEvents.class,
                            "onRemoveConstraint",
                            SXRWorld.this,
                            constraint);
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

    private void generateCollisionEvents()
    {
        SXRCollisionInfo collisionInfos[] = NativePhysics3DWorld.listCollisions(getNative());

        for (SXRCollisionInfo info : collisionInfos)
        {
            if (info.isHit)
            {
                sendCollisionEvent(info, "onEnter");
            }
            else if ((mPhysicsObject.get(info.bodyA) != null) &&
                     (mPhysicsObject.get(info.bodyB) != null))
            {
                sendCollisionEvent(info, "onExit");
            }
        }
    }

    private void sendCollisionEvent(SXRCollisionInfo info, String eventName)
    {
        SXRNode bodyA = mPhysicsObject.get(info.bodyA).getOwnerObject();
        SXRNode bodyB = mPhysicsObject.get(info.bodyB).getOwnerObject();
        SXREventManager em =  getSXRContext().getEventManager();

        em.sendEvent(bodyA, ICollisionEvents.class, eventName,
                     bodyA, bodyB, info.normal, info.distance);

        em.sendEvent(bodyB, ICollisionEvents.class, eventName,
                     bodyB, bodyA, info.normal, info.distance);

        em.sendEvent(this, ICollisionEvents.class, eventName,
                     bodyA, bodyB, info.normal, info.distance);

    }

    protected void doPhysicsDetach(SXRNode rootNode)
    {
        super.doPhysicsDetach(rootNode);
        if (isEnabled())
        {
            stopSimulation();
        }
    }

    @Override
    public void onAttach(SXRNode newOwner)
    {
        if (newOwner.getParent() != null)
        {
            throw new UnsupportedOperationException("SXRWorld must be attached to the scene's root object");
        }
        super.onAttach(newOwner);
    }

    @Override
    public void onEnable()
    {
        super.onEnable();
        if (getOwnerObject() != null)
        {
            startSimulation();
        }
    }

    @Override
    public void onDisable()
    {
        super.onDisable();
        stopSimulation();
    }

    public void setGravity(final float x, final float y, final float z)
    {
        mPhysicsContext.runOnPhysicsThread(new Runnable()
        {
            @Override
            public void run()
            {
                NativePhysics3DWorld.setGravity(getNative(), x, y, z);
            }
        });
    }

    public void getGravity(float[] gravity)
    {
        NativePhysics3DWorld.getGravity(getNative(), gravity);
    }


    private class SXRWorldTask implements Runnable
    {
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
        public void run()
        {
            if (!running)
            {
                return;
            }
            simulationTime = SystemClock.uptimeMillis();
            timeStep  = simulationTime - lastSimulTime;
            maxSubSteps = (int) (timeStep * 60) / 1000 + 1;
timeStep = 1 / 60.0f;
            NativePhysics3DWorld.step(getNative(), timeStep, maxSubSteps);

            if (mDoDebugDraw)
            {
                getSXRContext().runOnGlThread(debugDrawTask);
            }
            generateCollisionEvents();
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

        public void start()
        {
            // To avoid concurrency
            mPhysicsContext.runOnPhysicsThread(new Runnable() {
                @Override
                public void run()
                {
                    if (!running)
                    {
                        running = true;
                        lastSimulTime = SystemClock.uptimeMillis();
                        mPhysicsContext.runDelayedOnPhysicsThread(SXRWorldTask.this, intervalMillis);
                    }
                }
            });
        }

        public void stop()
        {
            // To avoid concurrency
            mPhysicsContext.runOnPhysicsThread(new Runnable() {
                @Override
                public void run()
                {
                    if (running)
                    {
                        running = false;
                        mPhysicsContext.removeTask(SXRWorldTask.this);
                    }
                }
            });
        }
    }

    public SXRNode setupDebugDraw()
    {
        SXRContext ctx = getSXRContext();
        SXRShaderId debugShader = ctx.getShaderManager().getShaderType(PhysicsDebugShader.class);
        SXRMaterial mtl = new SXRMaterial(ctx, debugShader);
        SXRRenderData rd = new SXRRenderData(ctx, mtl);
        SXRMesh mesh = new SXRMesh(new SXRVertexBuffer(ctx, "float3 a_position float3 a_color", 5000), null);
        SXRNode debugDrawNode = new SXRNode(ctx);

        mtl.setFloat("line_width", 5.0f);
        rd.setMesh(mesh);
        rd.setRenderingOrder(SXRRenderData.SXRRenderingOrder.OVERLAY);
        rd.setDrawMode(GL_LINES);
        debugDrawNode.attachRenderData(rd);
        NativePhysics3DWorld.setupDebugDraw(getNative(), debugDrawNode.getNative());
        return debugDrawNode;
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

    static native void addJointWithMask(long jphysics_world, long jjoint, long collisionType, long collidesWith);

    static native void removeJoint(long jphysics_world, long jjoint);

    static native void addRigidBody(long jphysics_world, long jrigid_body);

    static native void addRigidBodyWithMask(long jphysics_world, long jrigid_body, long collisionGroup, long collidesWith);

    static native void removeRigidBody(long jphysics_world, long jrigid_body);

    static native void step(long jphysics_world, float jtime_step, int maxSubSteps);

    static native void getGravity(long jworld, float[] array);

    static native void setGravity(long jworld, float x, float y, float z);

    static native SXRCollisionInfo[] listCollisions(long jphysics_world);

    static native void setupDebugDraw(long jworld, long jnode);

    static native void debugDrawWorld(long jworld);

    static native void setDebugMode(long jworld, int mode);

}
