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
    private final SXRWorld mWorld;
    private SXRSkeleton mSkeleton;
    private final Map<String, JSONObject> mTargetBones = new HashMap<String, JSONObject>();
    private SXRMaterial mColliderMtl;
    private float mAngularDamping;
    private float mLinearDamping;

    public PhysicsAVTLoader(SXRNode root)
    {
        mRoot = root;
        mContext = root.getSXRContext();
        mSkeleton = null;
        mColliderMtl = new SXRMaterial(mContext, SXRMaterial.SXRShaderType.Phong.ID);
        mColliderMtl.setDiffuseColor(1, 0, 1, 1);
        mWorld = (SXRWorld) root.getComponent(SXRWorld.getComponentType());
    }

    public SXRNode parse(byte[] inputData) throws IOException
    {
        String s = new String(inputData);
        SXRNode rootNode = null;

        Log.e(TAG, "loading physics file");
        try
        {
            JSONObject start = new JSONObject(s);
            if (mWorld.isMultiBody())
            {
              SXRPhysicsJoint rootJoint = parseMultiBodyPhysics(start);
              return rootJoint.getOwnerObject();
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
        return rootNode;
    }

    private void parsePhysics(JSONObject root) throws JSONException
    {
        JSONObject multibody = root.getJSONObject("Multi Body").getJSONObject("value");
        JSONObject basebone = multibody.getJSONObject("Base Bone");
        JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");

        mAngularDamping = (float) multibody.optDouble("Angular Damping", 0.0f);
        mLinearDamping = (float) multibody.optDouble("Linear Damping", 0.0f);
        mTargetBones.clear();
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
            Vector3f s = new Vector3f();
            Matrix4f m = new Matrix4f();

            s.set((float) scale.getDouble("X"),
                  (float) scale.getDouble("Y"),
                  (float) scale.getDouble("Z"));
            m.translation(-pivotB.x, -pivotB.y, -pivotB.z);
            if (type.equals("dmCapsuleCollider"))
            {
                SXRCapsuleCollider capsule = new SXRCapsuleCollider(ctx);
                String direction = c.getString("Direction");
                float radius = (float) c.getDouble("Radius");
                float height = (float) c.getDouble("Half Height") * 2.0f;
                SXRRenderData rd = new SXRRenderData(mContext, mColliderMtl);
                SXRMesh cubeMesh = null;

                capsule.setRadius(radius * s.x);
                if (direction.equals("X"))
                {
                    height *= s.x;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.X_AXIS);
                    cubeMesh = SXRCubeNode.createCube(mContext, "float3 a_position float3 a_normal",
                                                      true, new Vector3f(height, radius * s.y, radius * s.z));
                }
                else if (direction.equals("Y"))
                {
                    height *= s.y;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Y_AXIS);
                    cubeMesh = SXRCubeNode.createCube(mContext, "float3 a_position float3 a_normal",
                                                      true, new Vector3f(radius * s.x, height, radius * s.z));
                }
                else if (direction.equals("Z"))
                {
                    height *= s.z;
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Z_AXIS);
                    cubeMesh = SXRCubeNode.createCube(mContext, "float3 a_position float3 a_normal",
                                                      true, new Vector3f(radius * s.x, radius * s.y, height));
                }
                capsule.setHeight(height);
                cubeMesh.transform(m, false);
                rd.setMesh(cubeMesh);
                owner.attachComponent(rd);
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
                SXRMesh cubeMesh = SXRCubeNode.createCube(mContext, "float3 a_position float3 a_normal",
                                                      true, new Vector3f(2 * x, 2 * y, 2 * z));
                SXRRenderData rd = new SXRRenderData(mContext, mColliderMtl);

                cubeMesh.transform(m, false);
                rd.setMesh(cubeMesh);
                box.setHalfExtents(x, y, z);
                owner.attachComponent(rd);
                owner.attachComponent(box);
                Log.e(TAG, "box collider %s extents  %f, %f, %f",
                      colliderRoot.getString("Name"), x, y, z);
                return box;
            }
            else if (type.equals("dmSphereCollider"))
            {
                float radius = (float) c.getDouble("Radius");
                SXRSphereCollider sphere = new SXRSphereCollider(ctx);
                SXRSphereNode sp = new SXRSphereNode(ctx, true, mColliderMtl);
                SXRRenderData rd = new SXRRenderData(ctx, mColliderMtl);
                float sf = (float) scale.getDouble("X");

                m.scale(sf, sf, sf);
                m.setTranslation(-pivotB.x, -pivotB.y, -pivotB.z);
                rd.setMesh(sp.getRenderData().getMesh());
                rd.getMesh().transform(m, true);
                sphere.setRadius(radius * s.x);
                owner.attachComponent(rd);
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
        SXRPhysicsJoint joint = new SXRPhysicsJoint(parentJoint, jointType, boneID, mass);
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
                float lim = (float) Math.PI / 2.0f;
                ball.setAngularLowerLimits(-lim, -lim, -lim);
                ball.setAngularUpperLimits(lim, lim, lim);
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
            SXRUniversalConstraint ball = new SXRUniversalConstraint(mContext, parentBody, pivotA,
                                                                     axisA, axisB);
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            float lx = dofx.getBoolean("useLimit") ? (float) Math.toRadians(dofx.getDouble("limitLow")) : (float) -Math.PI;
            float ly = dofy.getBoolean("useLimit") ? (float) Math.toRadians(dofy.getDouble("limitLow")) : (float) -Math.PI;

            ball.setAngularLowerLimits(lx, ly, 0);
            lx = dofx.getBoolean("useLimit") ? (float) Math.toRadians(dofx.getDouble("limitHigh")) : (float) Math.PI;
            ly = dofy.getBoolean("useLimit") ? (float) Math.toRadians(dofy.getDouble("limitHigh")) : (float) Math.PI;
            ball.setAngularUpperLimits(lx, ly, 0);
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

