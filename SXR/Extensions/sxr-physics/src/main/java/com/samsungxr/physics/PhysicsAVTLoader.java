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



import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRCapsuleCollider;
import com.samsungxr.SXRCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMaterial;
import com.samsungxr.SXRMesh;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRRenderData;
import com.samsungxr.SXRSphereCollider;
import com.samsungxr.SXRTransform;
import com.samsungxr.animation.SXRPose;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.nodes.SXRCubeNode;
import com.samsungxr.nodes.SXRSphereNode;
import com.samsungxr.utility.Log;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class PhysicsAVTLoader
{
    private final String TAG = "AVT";;
    private final SXRNode mRoot;
    private final SXRContext mContext;
    private final SXRPhysicsContent mWorld;
    private SXRSkeleton mSkeleton;
    private final Map<String, JSONObject> mTargetBones = new HashMap<String, JSONObject>();
    private float mAngularDamping;
    private float mLinearDamping;

    public PhysicsAVTLoader(SXRContext ctx, boolean isMultiBody)
    {
        this(new SXRNode(ctx), isMultiBody);
    }

    public PhysicsAVTLoader(SXRNode root, boolean isMultiBody)
    {
        mRoot = root;
        mContext = root.getSXRContext();
        mSkeleton = null;
        mWorld = new SXRPhysicsContent(root, isMultiBody);
        mRoot.attachComponent(mWorld);
    }

    public SXRPhysicsContent parse(byte[] inputData) throws IOException
    {
        String s = new String(inputData);

        Log.e(TAG, "loading physics file");
        try
        {
            JSONObject start = new JSONObject(s);
            if (mWorld.isMultiBody())
            {
                parseMultiBodyPhysics(start);
            }
            else
            {
                parsePhysics(start);
            }
            Log.e(TAG, "loading done");
        }
        catch (JSONException ex)
        {
            throw new IOException(ex);
        }
        return mWorld;
    }

    private void parsePhysics(JSONObject root) throws JSONException
    {
        JSONObject multibody = root.getJSONObject("Multi Body").getJSONObject("value");
        JSONObject basebone = multibody.getJSONObject("Base Bone");
        JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");

        mTargetBones.clear();
        mTargetBones.put(basebone.getString("Name"), basebone);
        mAngularDamping = (float) multibody.optDouble("Angular Damping", 0.0f);
        mLinearDamping = (float) multibody.optDouble("Linear Damping", 0.0f);
        parseSkeleton(basebone, childbones);
        parseRigidBody(basebone);
        for (int i = 0; i < childbones.length(); ++i)
        {
            parseRigidBody(childbones.getJSONObject(i).getJSONObject("value"));
        }
    }

    private SXRSkeleton parseSkeleton(JSONObject basebone, JSONArray bonelist) throws JSONException
    {
        int numbones = bonelist.length() + 1;
        String bonenames[] = new String[numbones];
        int boneparents[] = new int[numbones];
        SXRNode bones[] = new SXRNode[numbones];
        Matrix4f worldMtx = new Matrix4f();

        boneparents[0] = -1;
        mTargetBones.put(basebone.getString("Name"), basebone);

        for (int i = 0; i <  numbones; ++i)
        {
            JSONObject bone = (i == 0) ? basebone : bonelist.getJSONObject(i - 1).getJSONObject("value");
            String parentName = bone.optString("Parent", "");
            String boneName = bone.getString("Target Bone");
            bonenames[i] = boneName;
            boneparents[i] = -1;
            SXRNode node = new SXRNode(mContext);
            bones[i] = node;
            node.setName(bonenames[i]);
            mTargetBones.put(bone.getString("Name"), bone);
            if (!parentName.isEmpty())
            {
                JSONObject parentBone = mTargetBones.get(parentName);
                parentName = parentBone.getString("Target Bone");
                for (int j = 0; j < i; ++j)
                {
                    if (parentName.equals(bonenames[j]))
                    {
                        boneparents[i] = j;
                        bones[j].addChildObject(bones[i]);
                        break;
                    }
                }
            }
        }
        SXRSkeleton skel = new SXRSkeleton(mContext, boneparents);
        SXRPose worldPose = new SXRPose(skel);

        for (int i = 0; i <  numbones; ++i)
        {
            JSONObject bone = (i == 0) ? basebone : bonelist.getJSONObject(i - 1).getJSONObject("value");
            JSONObject xform = bone.getJSONObject("Transform");
            JSONObject position = xform.getJSONObject("Position");
            JSONObject orientation = xform.getJSONObject("Orientation");

            worldMtx.translationRotate((float) position.getDouble("X"),
                                       (float) position.getDouble("Y"),
                                       (float) position.getDouble("Z"),
                                       (float) orientation.getDouble("X"),
                                       (float) orientation.getDouble("Y"),
                                       (float) orientation.getDouble("Z"),
                                       (float) orientation.getDouble("W"));
            worldPose.setWorldMatrix(i, worldMtx);
            skel.setBoneName(i, bonenames[i]);
            skel.setBone(i, bones[i]);
        }
        mSkeleton = skel;
        worldPose.sync();
        skel.setPose(worldPose);
        skel.poseToBones();
        bones[0].attachComponent(skel);
        mRoot.addChildObject(bones[0]);
        return skel;
    }

    private SXRPhysicsJoint parseMultiBodyPhysics(JSONObject root) throws JSONException
    {
        JSONObject multibody = root.getJSONObject("Multi Body").getJSONObject("value");
        JSONObject basebone = multibody.getJSONObject("Base Bone");
        JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");
        float mass = (float) basebone.getDouble("Mass");

        mTargetBones.clear();
        mTargetBones.put(basebone.getString("Name"), basebone);
        SXRPhysicsJoint rootJoint = new SXRPhysicsJoint(mContext, mass, childbones.length() + 1);

        parseSkeleton(basebone, childbones);
        parseBone(basebone, rootJoint);
        for (int i = 0; i < childbones.length(); ++i)
        {
            JSONObject link = childbones.getJSONObject(i).getJSONObject("value");
            String nodeName = link.getString("Target Bone");
            int boneID = mSkeleton.getBoneIndex(nodeName);

            if (boneID < 0)
            {
                throw new IllegalArgumentException("AVT file skeleton missing bone " + nodeName + " referenced by MultiBody physics");
            }
            parseJoint(link);
        }
        rootJoint.getSkeleton();
        return rootJoint;
    }

    private SXRCollider parseCollider(JSONObject link, String targetBone) throws JSONException
    {
        SXRContext ctx = mRoot.getSXRContext();
        JSONObject colliderRoot = link.getJSONObject("Collider").getJSONObject("value");
        JSONArray colliders = colliderRoot.getJSONObject("Child Colliders").getJSONArray("property_value");
        JSONObject pivot = link.optJSONObject("Pivot Pos.");
        JSONObject trans = link.getJSONObject("Transform");
        JSONObject pos = trans.getJSONObject("Position");
        Vector3f pivotB = new Vector3f(0, 0, 0);
        if (pivot != null)
        {
            pivotB.set((float) (pivot.getDouble("X") - pos.getDouble("X")),
                       (float) (pivot.getDouble("Y") - pos.getDouble("Y")),
                       (float) (pivot.getDouble("Z") - pos.getDouble("Z")));
        }
        for (int i = 0; i < colliders.length(); ++i)
        {
            JSONObject c = colliders.getJSONObject(i).getJSONObject("Collider");
            JSONObject xform = colliders.getJSONObject(i).getJSONObject("Transform");
            String type = c.optString("type");
            SXRNode owner;

            c = c.getJSONObject("value");
            if (type == null)
            {
                return null;
            }
            if (mWorld.isMultiBody() && (mSkeleton != null))
            {
                int boneIndex = mSkeleton.getBoneIndex(targetBone);

                if (boneIndex < 0)
                {
                    Log.e(TAG, "Cannot find bone " + targetBone +
                        " needed by " + c.getString("Name") + " collider");
                    return null;
                }
                owner = mSkeleton.getBone(boneIndex);
            }
            else
            {
                owner = mRoot.getNodeByName(targetBone);
            }
            if (owner == null)
            {
                throw new IllegalArgumentException("Target bone " + targetBone + " referenced in AVT file not found in scene");
            }
            JSONObject scale = xform.getJSONObject("Scale");
            Vector3f s = new Vector3f(1, 1, 1);

            s.set((float) scale.getDouble("X"),
                  (float) scale.getDouble("Y"),
                  (float) scale.getDouble("Z"));
            if (type.equals("dmCapsuleCollider"))
            {
                SXRCapsuleCollider capsule = new SXRCapsuleCollider(ctx);
                String direction = c.getString("Direction");
                float radius = (float) c.getDouble("Radius");
                float height = ((float) (c.getDouble("Half Height") * 2) + radius);
                Vector3f dimensions = new Vector3f();

                capsule.setRadius(radius);
                if (direction.equals("X"))
                {
                    radius *= s.z;
                    height *= s.x;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.X_AXIS);
                    dimensions.x = height + radius;
                    dimensions.y = dimensions.z = radius;
                }
                else if (direction.equals("Y"))
                {
                    height *= s.y;
                    radius *= s.x;
                    dimensions.y = height + radius;
                    dimensions.x = dimensions.z = radius;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Y_AXIS);
                }
                else if (direction.equals("Z"))
                {
                    height *= s.z;
                    radius *= s.x;
                    dimensions.z = height + radius;
                    dimensions.x = dimensions.y = radius;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Z_AXIS);
                }
                capsule.setHeight(height);
                capsule.setRadius(radius);
                owner.attachComponent(capsule);
                Log.e(TAG, "capsule collider %s height %f radius %f %s axis",
                      colliderRoot.getString("Name"), height, radius, direction);
                return capsule;
            }
            else if (type.equals("dmBoxCollider"))
            {
                SXRBoxCollider box = new SXRBoxCollider(ctx);
                JSONObject size = c.getJSONObject("Half Size");
                float x = (float) size.getDouble("X") * s.x;
                float y = (float) size.getDouble("Y") * s.y;
                float z = (float) size.getDouble("Z") * s.z;

                box.setHalfExtents(x, y, z);
                owner.attachComponent(box);
                Log.e(TAG, "box collider %s extents  %f, %f, %f",
                      colliderRoot.getString("Name"), x, y, z);
                return box;
            }
            else if (type.equals("dmSphereCollider"))
            {
                float radius = (float) c.getDouble("Radius");
                SXRSphereCollider sphere = new SXRSphereCollider(ctx);

                owner.attachComponent(sphere);
                Log.e(TAG, "sphere collider %s radius %f", colliderRoot.getString("Name"), radius);
                return sphere;
            }
            else
            {
                throw new JSONException(type + " is an unknown collider type");
            }
        }
        return null;
    }

    private SXRPhysicsJoint findParentJoint(String parentName) throws JSONException
    {
        JSONObject parent = mTargetBones.get(parentName);
        String nodeName = parent.getString("Target Bone");
        SXRNode parentNode = mSkeleton.getBone(mSkeleton.getBoneIndex(nodeName));

        if (parentNode != null)
        {
            SXRPhysicsJoint joint = (SXRPhysicsJoint) parentNode.getComponent(SXRPhysicsJoint.getComponentType());
            if (joint != null)
            {
                return joint;
            }
        }
        Log.e(TAG, "Cannot find bone %s referenced by AVT file", nodeName);
        return null;
    }

    private SXRRigidBody findParentBody(String parentName) throws JSONException
    {
        if (parentName == null)
        {
            return null;
        }
        JSONObject parent = mTargetBones.get(parentName);
        String nodeName = parent.getString("Target Bone");
        SXRNode parentNode = mRoot.getNodeByName(nodeName);

        if (parentNode != null)
        {
            SXRRigidBody body = (SXRRigidBody) parentNode.getComponent(SXRRigidBody.getComponentType());
            if (body != null)
            {
                return body;
            }
        }
        Log.e(TAG, "Cannot find bone %s referenced by AVT file", nodeName);
        return null;
    }

    private SXRPhysicsJoint parseJoint(JSONObject link) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        int boneID = mSkeleton.getBoneIndex(nodeName);
        String name = link.getString("Name");
        String parentName = link.optString("Parent", null);
        String type = link.getString("Joint Type");
        int collisionGroup = link.optInt("Collision Layer ID", 0);
        int jointType;
        JSONArray dofdata = link.getJSONArray("DOF Data");
        float mass = (float) link.getDouble("Mass");
        SXRPhysicsJoint parentJoint = findParentJoint(parentName);
        float[] pivotB = new float[] { 0, 0, 0 };
        Vector3f axis = null;
        JSONObject trans = link.getJSONObject("Transform");
        JSONObject pos = trans.getJSONObject("Position");
        JSONObject piv = link.optJSONObject("Pivot Pos.");

        if (parentJoint == null)
        {
            Log.e(TAG, "Parent %s not found for child %s", parentName, name);
            return null;
        }
        if (piv != null)
        {
            pivotB[0] = (float) (piv.getDouble("X") - pos.getDouble("X"));
            pivotB[1] = (float) (piv.getDouble("Y") - pos.getDouble("Y"));
            pivotB[2] = (float) (piv.getDouble("Z") - pos.getDouble("Z"));
        }
        mTargetBones.put(name, link);
        if (type.equals("ball"))
        {
/*
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject dofz = dofdata.getJSONObject(2);

            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        (float) Math.toRadians(dofz.getDouble("limitLow")));
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofz.getDouble("limitHigh")));
*/
            jointType = SXRPhysicsJoint.SPHERICAL;
            Log.e(TAG, "Ball joint between %s and %s joint(%f, %f, %f)",
                  parentName, name, pivotB[0], pivotB[1], pivotB[2]);
        }
        else if (type.equals("universal"))  // TODO: figure out universal joint
        {
/*
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);

            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                    (float) Math.toRadians(dofy.getDouble("limitLow")),
                    0);
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                    (float) Math.toRadians(dofy.getDouble("limitHigh")),
                    0);
*/
            jointType = SXRPhysicsJoint.SPHERICAL;
            Log.e(TAG, "Universal joint between %s and %s joint(%f, %f, %f)",
                    parentName, name, pivotB[0], pivotB[1], pivotB[2]);
        }
        else if (type.equals("hinge"))
        {
            JSONObject v = link.getJSONObject("Axis A");
            axis = new Vector3f((float) v.getDouble("X"),
                                (float) v.getDouble("Y"),
                                (float) v.getDouble("Z"));
/*
            JSONObject dof = dofdata.getJSONObject(0);
            hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")),
                            (float) Math.toRadians(dof.getDouble("limitHigh")));
*/
            jointType = SXRPhysicsJoint.REVOLUTE;
            Log.e(TAG, "Hinge joint between %s  and %s (%f, %f, %f)",
                    parentName, name, pivotB[0], pivotB[1], pivotB[2]);
        }
        else if (type.equals("fixed"))
        {
            Log.e(TAG, "Fixed joint between %s and %s", parentName, name);
            jointType = SXRPhysicsJoint.FIXED;
        }
        else
        {
            throw new JSONException(type + " is an unknown constraint type");
        }
        SXRPhysicsJoint joint = new SXRPhysicsJoint(parentJoint, jointType, boneID, mass, collisionGroup);
        joint.setPivot(pivotB[0], pivotB[1], pivotB[2]);
        if (axis != null)
        {
            joint.setAxis(axis.x, axis.y, axis.z);
        }
        parseBone(link, joint);
        return joint;
    }

    private SXRRigidBody parseRigidBody(JSONObject link) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mRoot.getNodeByName(nodeName);

        if (node == null)
        {
            Log.e(TAG,"Cannot find bone " + nodeName + " referenced by AVT file");
            return null;
        }
        String name = link.getString("Name");
        String parentName = link.optString("Parent", null);
        float mass = (float) link.getDouble("Mass");
        SXRRigidBody parentBody = findParentBody(parentName);
        int collisionGroup = link.optInt("Collision Layer ID", 0);
        SXRRigidBody body = new SXRRigidBody(mContext, mass, collisionGroup);
        JSONObject props = link.getJSONObject("Physic Material");
        JSONObject v;
        float PIover2 = (float) Math.PI / 2;

        body.setFriction((float) props.getDouble("Friction"));
        body.setDamping(mLinearDamping, mAngularDamping);
        mTargetBones.put(name, link);
        if (parentBody == null)
        {
            SXRSkeleton skel = (SXRSkeleton) node.getComponent(SXRSkeleton.getComponentType());
            if (mWorld.isMultiBody() && (skel != null))
            {
                mSkeleton = skel;
            }
            if (parseCollider(link, nodeName) == null)
            {
                return null;
            }
            node.attachComponent(body);
            return body;
        }
        String type = link.getString("Joint Type");
        SXRConstraint constraint;
        JSONArray dofdata = link.getJSONArray("DOF Data");
        if (parseCollider(link, nodeName) == null)
        {
            return null;
        }
        node.attachComponent(body);
        JSONObject trans = link.getJSONObject("Transform");
        JSONObject pos = trans.getJSONObject("Position");
        JSONObject pivot = link.optJSONObject("Pivot Pos.");
        Vector3f pivotB = new Vector3f(0, 0, 0);
        Matrix4f worldMtx = node.getTransform().getModelMatrix4f();

        if (pivot != null)
        {
            pivotB.set((float) (pivot.getDouble("X") - pos.getDouble("X")),
                       (float) (pivot.getDouble("Y") - pos.getDouble("Y")),
                       (float) (pivot.getDouble("Z") - pos.getDouble("Z")));
        }
        JSONObject parentLink = mTargetBones.get(parentName);
        Vector3f pivotA = new Vector3f(0, 0, 0);
        trans = parentLink.getJSONObject("Transform");
        pos = trans.getJSONObject("Position");
        if (pivot != null)
        {
            pivotA.set((float) (pivot.getDouble("X") - pos.getDouble("X")),
                       (float) (pivot.getDouble("Y") - pos.getDouble("Y")),
                       (float) (pivot.getDouble("Z") - pos.getDouble("Z")));
        }
        if (type.equals("ball"))
        {
            if (true)
            {
                SXRGenericConstraint ball = new SXRGenericConstraint(mContext, parentBody, pivotA, pivotB);
                ball.setAngularLowerLimits(-PIover2, -PIover2, -PIover2);
                ball.setAngularUpperLimits(PIover2, PIover2, PIover2);
                ball.setLinearLowerLimits(0, 0, 0);
                ball.setLinearUpperLimits(0, 0, 0);
                constraint = ball;
            }
            else
            {
                SXRPoint2PointConstraint ball = new SXRPoint2PointConstraint(mContext, parentBody, pivotA, pivotB);
                constraint = ball;
            }
            Log.e(TAG, "Ball joint between %s and %s (%f, %f, %f)",
                  parentName, name, pivotB.x, pivotB.y, pivotB.z);
        }
        else if (type.equals("universal"))
        {
            if (pivot != null)
            {
                pivotB.set((float) (pivot.getDouble("X")),
                           (float) (pivot.getDouble("Y")),
                           (float) (pivot.getDouble("Z")));
            }
            else
            {
                pivotB.set(0, 0, 0);
            }
            v = link.getJSONObject("Axis A");
            Vector4f aa = new Vector4f(
                (float) v.getDouble("X"),
                (float) v.getDouble("Y"),
                (float) v.getDouble("Z"),
                    0);
            v = link.getJSONObject("Axis B");
            Vector4f ab = new Vector4f(
                (float) v.getDouble("X"),
                (float) v.getDouble("Y"),
                (float) v.getDouble("Z"),
                    0);
            worldMtx.transform(aa);
            worldMtx.transform(ab);
            Vector3f axisA = new Vector3f(aa.x, aa.y, aa.z);
            Vector3f axisB = new Vector3f(ab.x, ab.y, ab.z);
            axisA.normalize();
            axisB.normalize();

            SXRUniversalConstraint ball = new SXRUniversalConstraint(mContext, parentBody, pivotB, axisA, axisB);
            JSONObject dof0 = dofdata.getJSONObject(0);
            JSONObject dof1 = dofdata.getJSONObject(1);
            float lz = dof0.getBoolean("useLimit") ? (float) Math.toRadians(dof0.getDouble("limitLow")) : -PIover2;
            float ly = dof1.getBoolean("useLimit") ? (float) Math.toRadians(dof1.getDouble("limitLow")) : -PIover2;

            ball.setAngularLowerLimits(0, ly, lz);
            lz = dof0.getBoolean("useLimit") ? (float) Math.toRadians(dof0.getDouble("limitHigh")) : PIover2;
            ly = dof1.getBoolean("useLimit") ? (float) Math.toRadians(dof1.getDouble("limitHigh")) : PIover2;
            ball.setAngularUpperLimits(0, ly, lz);
            constraint = ball;
            Log.e(TAG, "Universal joint between %s and %s (%f, %f, %f)",
                  parentName, name, pivotB.x, pivotB.y, pivotB.z);
        }
        else if (type.equals("hinge"))
        {
            v = link.optJSONObject("Axis A");
            Vector3f axisA = new Vector3f(
                        (float) v.getDouble("X"),
                        (float) v.getDouble("Y"),
                        (float) v.getDouble("Z"));
            SXRHingeConstraint hinge = new SXRHingeConstraint(mContext, parentBody,
                                                              pivotA, pivotB, axisA);
            JSONObject dof = dofdata.getJSONObject(0);

            if (dof.getBoolean("useLimit"))
            {
                hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")), (float) Math.toRadians(dof.getDouble("limitHigh")));
            }
            else
            {
                hinge.setLimits(-PIover2, PIover2);
            }
            constraint = hinge;
            Log.e(TAG, "Hinge joint between %s and %s (%f, %f, %f)",
                  parentName, name, pivotB.x, pivotB.y, pivotB.z);
        }
        else if (type.equals("fixed"))
        {
           SXRFixedConstraint fixed = new SXRFixedConstraint(mContext, parentBody);
//            SXRPoint2PointConstraint fixed = new SXRPoint2PointConstraint(mContext, parentBody, pivotA, pivotB);
            constraint = fixed;
            Log.e(TAG, "Fixed joint between %s and %s", parentName, name);
        }
        else
        {
            throw new JSONException(type + " is an unknown constraint type");
        }
        if (link.has("Breaking Reaction Impulse"))
        {
            constraint.setBreakingImpulse((float) link.getDouble("Breaking Reaction Impulse"));
        }
        node.attachComponent(constraint);
        return body;
    }

    private void parseBone(JSONObject link, SXRPhysicsJoint joint) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mSkeleton.getBone(joint.getBoneID());

        parseCollider(link, nodeName);

        JSONObject props = link.getJSONObject("Physic Material");
        joint.setFriction((float) props.getDouble("Friction"));
        node.attachComponent(joint);
        Log.e(TAG, "link %s bone = %s boneID = %d ",
              link.getString("Name"),
              nodeName,
              joint.getBoneID());
    }

}

