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
 * This class is created during content import. It cannot perform
 * physics simulation, it just contains the objects.
 * Use {@link SXRWorld} for simulation.
 */
public class SXRPhysicsContent extends SXRComponent
{
    protected boolean mIsMultibody = false;
    protected final LongSparseArray<SXRPhysicsWorldObject> mPhysicsObject = new LongSparseArray<SXRPhysicsWorldObject>();

    /**
     * Constructs a Physics World to contain physics components of a hierarchy.
     * This version of the constructor is used when importing physics from
     * files - the nodes are not part of the scene yet. To perform simulation,
     * a world constructed with this method must be merged with a world
     * attached to the root of a scene.
     */
    public SXRPhysicsContent(SXRNode root, boolean isMultiBody)
    {
        super(root.getSXRContext(), NativePhysics3DWorld.ctor(isMultiBody));
        mIsEnabled = false;
        mIsMultibody = isMultiBody;
        root.attachComponent(this);
    }


    static public long getComponentType()
    {
        return NativePhysics3DWorld.getComponentType();
    }

    public boolean isMultiBody() { return mIsMultibody; }

    /**
     * Add a {@link SXRConstraint} to this physics world.
     *
     * @param gvrConstraint The {@link SXRConstraint} to add.
     */
    public void addConstraint(final SXRConstraint gvrConstraint)
    {
        if (!contains(gvrConstraint))
        {
            SXRPhysicsCollidable bodyB = gvrConstraint.mBodyB;
            SXRPhysicsCollidable bodyA = gvrConstraint.mBodyA;

            if ((bodyB != null) && (bodyB instanceof SXRRigidBody))
            {
                if (!contains(bodyB) || ((bodyA != null) && !contains(bodyA)))
                {
                    throw new UnsupportedOperationException("Rigid body used by constraint is not found in the physics world.");
                }
            }
            mPhysicsObject.put(gvrConstraint.getNative(), gvrConstraint);
        }
    }

    /**
     * Remove a {@link SXRFixedConstraint} from this physics world.
     *
     * @param gvrConstraint the {@link SXRFixedConstraint} to remove.
     */
    public void removeConstraint(final SXRConstraint gvrConstraint)
    {
        if (contains(gvrConstraint))
        {
            mPhysicsObject.remove(gvrConstraint.getNative());
        }
    }


    /**
     * Returns true if the physics world contains the the specified rigid body.
     *
     * @param physicsObject Physics object to check if it is present in the world.
     * @return true if the world contains the specified object.
     */
    protected boolean contains(SXRPhysicsWorldObject physicsObject)
    {
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
    public void addBody(final SXRRigidBody gvrBody)
    {
         if (!contains(gvrBody))
         {
             mPhysicsObject.put(gvrBody.getNative(), gvrBody);
         }
    }

    /**
     * Add a {@link SXRPhysicsJoint} root to this physics world.
     *
     * @param body The {@link SXRPhysicsJoint} to add.
     */
    public void addBody(final SXRPhysicsJoint body)
    {
        if (!contains(body))
        {
            mPhysicsObject.put(body.getNative(), body);
        }
    }

    /**
     * Remove a {@link SXRRigidBody} from this physics world.
     *
     * @param gvrBody the {@link SXRRigidBody} to remove.
     */
    public void removeBody(final SXRRigidBody gvrBody)
    {
        if (contains(gvrBody))
        {
            mPhysicsObject.remove(gvrBody.getNative());
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


    protected void doPhysicsAttach(SXRNode rootNode)
    {
        rootNode.forAllDescendants(mAttachPhysics);
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

    protected SXRNode.SceneVisitor mAttachPhysics = new SXRNode.SceneVisitor()
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
            else
            {
                return true;
            }
            SXRConstraint constraint = (SXRConstraint) obj.getComponent(SXRConstraint.getComponentType());
            if (constraint != null)
            {
                addConstraint(constraint);
            }
            return true;
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
