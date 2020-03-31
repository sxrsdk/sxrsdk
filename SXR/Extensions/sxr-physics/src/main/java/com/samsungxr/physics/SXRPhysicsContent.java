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
import com.samsungxr.SXRContext;
import com.samsungxr.SXREventReceiver;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRNode.ComponentVisitor;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRShader;
import com.samsungxr.SXRShaderData;
import com.samsungxr.SXRShaderId;
import com.samsungxr.SXRShaderManager;
import com.samsungxr.SXRTransform;
import com.samsungxr.IEventReceiver;
import com.samsungxr.IEvents;
import com.samsungxr.SXRVertexBuffer;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.io.SXRCursorController;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES20.GL_LINES;

/**
 * Represents a physics world which contains imported physics content.
 * This class is the result of loading physics content.
 * It cannot perform physics simulation, it just contains the objects.
 * <p>
 *  To become dynamic, this content must be added to a simulation world:
 *  <pre>
 *  SXRScene scene = ... // the scene being displayhed
 *  SXRPhysicsContent importedPhysics = SXRPhysicsLoader.loadAvatarFile(...).
 *  SXRWorld simulationWorld = new SXRWorld(scene);
 *  simulationWorld.merge(importedPhysics);
 *  simulationWorld.enable();
 *  </pre>
 * </p>
 * @see SXRWorld
 * @see SXRPhysicsLoader
 */
public class SXRPhysicsContent extends SXRComponent
{
    protected boolean mIsMultibody = false;
    protected final LongSparseArray<SXRPhysicsWorldObject> mPhysicsObject = new LongSparseArray<SXRPhysicsWorldObject>();

    /**
     * Collects physics components and nodes in a hierarchy under the given root node.
     * <p>
     * This version of the constructor is used when importing physics from avatar
     * files - the nodes are not part of the scene yet. To perform simulation,
     * this container must be merged with a simulation world attached to the root of a scene.
     * @param root          Root {@link SXRNode} under which imported content is added.
     * @param isMultiBody   import articulated bodies as {@link SXRPhysicsJoint} components
     *                      and use Featherstone multibody simulation. The default is to
     *                      import everything as {@link SXRRigidBody} components and
     *                      use discrete dynamics.
     * @see SXRWorld#merge(SXRPhysicsContent)
     */
    public SXRPhysicsContent(SXRNode root, boolean isMultiBody)
    {
        super(root.getSXRContext(), NativePhysics3DWorld.ctor(isMultiBody));
        mIsEnabled = false;
        mIsMultibody = isMultiBody;
    }

    static public long getComponentType()
    {
        return NativePhysics3DWorld.getComponentType();
    }

    /**
     * Detect whether this world supports multibody physics.
     * {@link SXRPhysicsJoint} components can only be added to worlds
     * which support multibody physics. Multibody support is established
     * at construction time.
     * @return true if multibody is supported, else false.
     */
    public boolean isMultiBody() { return mIsMultibody; }

    /**
     * Add a {@link SXRConstraint} to this physics world.
     * The rigid bodies or joints reference by the constraint must
     * already have been added to the world.
     * @param constraint The {@link SXRConstraint} to add.
     * @throws UnsupportedOperationException
     *                    if the collidables referenced by
     *                    the constraint have not been added already.
     * @see #addBody(SXRRigidBody)
     */
    public void addConstraint(final SXRConstraint constraint)
    {
        if (!contains(constraint))
        {
            SXRPhysicsCollidable bodyB = constraint.mBodyB;
            SXRPhysicsCollidable bodyA = constraint.mBodyA;

            if (bodyB != null)
            {
                if (!contains(bodyB) || ((bodyA != null) && !contains(bodyA)))
                {
                    throw new UnsupportedOperationException("Collidable used by constraint is not found in the physics world.");
                }
            }
            mPhysicsObject.put(constraint.getNative(), constraint);
        }
    }

    /**
     * Remove a {@link SXRFixedConstraint} from this physics world.
     *
     * @param constraint the {@link SXRFixedConstraint} to remove.
     */
    public void removeConstraint(final SXRConstraint constraint)
    {
        if (contains(constraint))
        {
            mPhysicsObject.remove(constraint.getNative());
        }
    }


    /**
     * Returns true if the physics world contains the the specified physics component.
     *
     * @param physicsObject Physics component to check if it is present in the world.
     *                      This must be a {@link SXRRigidBody}, a {@SXRPhysicsJoint}
     *                      or a {@link SXRConstraint}.
     * @return true if the world contains the specified object, else false.
     */
    protected boolean contains(SXRPhysicsWorldObject physicsObject)
    {
        if (physicsObject != null)
        {
            long nativePtr = physicsObject.getNative();
            return mPhysicsObject.get(nativePtr) != null;
        }
        return false;
    }

    /**
     * Add a {@link SXRRigidBody} to this physics world.
     *
     * @param body The {@link SXRRigidBody} to add.
     */
    public void addBody(final SXRRigidBody body)
    {
         if (!contains(body))
         {
             mPhysicsObject.put(body.getNative(), body);
         }
    }

    /**
     * Add a {@link SXRPhysicsJoint} root to this physics world.
     *
     * @param joint The {@link SXRPhysicsJoint} to add.
     */
    public void addBody(final SXRPhysicsJoint joint)
    {
        if (!contains(joint))
        {
            mPhysicsObject.put(joint.getNative(), joint);
        }
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     *
     * @param body the {@link SXRRigidBody} to remove.
     */
    public void removeBody(final SXRRigidBody body)
    {
        if (contains(body))
        {
            mPhysicsObject.remove(body.getNative());
        }
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     *
     * @param joint the {@link SXRPhysicsJoint} to remove.
     */
    public void removeBody(final SXRPhysicsJoint joint)
    {
        if (contains(joint))
        {
            mPhysicsObject.remove(joint.getNative());
        }
    }

    /**
     * Get a list of all the collidable objects (rigid bodies
     * and joints) in this world.
     * @return list of collidables, may be empty.
     */
    List<SXRPhysicsCollidable> getCollidables()
    {
        List<SXRPhysicsCollidable> collidables = new ArrayList<SXRPhysicsCollidable>();

        for (int i = 0; i < mPhysicsObject.size(); ++i)
        {
            SXRPhysicsWorldObject o = mPhysicsObject.valueAt(i);

            if (o instanceof SXRPhysicsCollidable)
            {
                collidables.add((SXRPhysicsCollidable) o);
            }
        }
        return collidables;
    }

    protected void doPhysicsAttach(SXRNode rootNode)
    {
        rootNode.forAllDescendants(mAttachBodies);
        attachConstraints(getCollidables());
    }

    protected void doPhysicsDetach(SXRNode rootNode)
    {
        rootNode.forAllDescendants(mDetachPhysics);
    }

    @Override
    public void onAttach(SXRNode newOwner)
    {
        super.onAttach(newOwner);
        doPhysicsAttach(newOwner);
    }

    @Override
    public void onDetach(SXRNode oldOwner)
    {
        super.onDetach(oldOwner);
        doPhysicsDetach(oldOwner);
    }

    /**
     * Scans the hierarchy for {@SXRPhysicsCollidable} objects
     * and adds them to the physics world. The bodies added
     * are accumulated in BodiesAttached. Constraints must
     * be added after the
     */
    protected  SXRNode.SceneVisitor mAttachBodies = new SXRNode.SceneVisitor()
    {
        @Override
        public boolean visit(SXRNode obj)
        {
            SXRRigidBody body = (SXRRigidBody) obj.getComponent(SXRRigidBody.getComponentType());

            if (body != null)
            {
                addBody(body);
            }
            else if (mIsMultibody)
            {
                SXRPhysicsJoint joint = (SXRPhysicsJoint) obj.getComponent(SXRPhysicsJoint.getComponentType());
                if (joint != null)
                {
                    addBody(joint);
                }
            }
            return true;
        }
    };

    protected void attachConstraints(List<SXRPhysicsCollidable> bodies)
    {
       for (SXRPhysicsCollidable body : bodies)
       {
            SXRNode node = body.getOwnerObject();
            SXRConstraint constraint = (SXRConstraint) node.getComponent(SXRConstraint.getComponentType());

            if (constraint != null)
            {
                addConstraint(constraint);
            }
        }
    };

    protected SXRNode.SceneVisitor mDetachPhysics = new SXRNode.SceneVisitor()
    {
        @Override
        public boolean visit(SXRNode obj)
        {
            SXRRigidBody body = (SXRRigidBody) obj.getComponent(SXRRigidBody.getComponentType());

            if (body != null)
            {
                removeBody(body);
            }
            else if (mIsMultibody)
            {
                SXRPhysicsJoint joint = (SXRPhysicsJoint) obj.getComponent(SXRPhysicsJoint.getComponentType());
                if (joint != null)
                {
                    removeBody(joint);
                }
            }
            else
            {
                return true;
            }
            SXRConstraint constraint = (SXRConstraint) obj.getComponent(SXRConstraint.getComponentType());
            if (constraint != null)
            {
                removeConstraint(constraint);
            }
            return true;
        }
    };

}
