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

;
import com.samsungxr.SXRBoxCollider;
import com.samsungxr.SXRCapsuleCollider;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.SXRSphereCollider;
import com.samsungxr.SXRTransform;
import com.samsungxr.animation.SXRSkeleton;
import com.samsungxr.utility.Log;

import org.joml.Vector3f;
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

    public PhysicsAVTLoader(SXRNode root)
    {
        mRoot = root;
        mContext = root.getSXRContext();
        mSkeleton = null;
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

        mTargetBones.clear();
        mTargetBones.put(basebone.getString("Name"), basebone);
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
        String parentName = basebone.optString("Parent Name", "");
        String boneName = basebone.getString("Target Bone");
        bonenames[0] = boneName;
        bones[0] = new SXRNode(mContext);
        bones[0].setName(boneName);
        boneparents[0] = -1;

        mRoot.addChildObject(bones[0]);
        mTargetBones.put(basebone.getString("Name"), basebone);

        for (int i = 1; i <  numbones; ++i)
        {
            JSONObject bone = bonelist.getJSONObject(i - 1).getJSONObject("value");
            parentName = bone.optString("Parent", "");
            boneName = bone.getString("Target Bone");
            bonenames[i] = boneName;
            boneparents[i] = -1;
            SXRNode node = new SXRNode(mContext);
            bones[i] = node;
            node.setName(bonenames[i]);
            SXRTransform trans = node.getTransform();
            JSONObject xform = bone.getJSONObject("Transform");
            JSONObject position = xform.getJSONObject("Position");
            JSONObject orientation = xform.getJSONObject("Orientation");
            JSONObject scale = xform.getJSONObject("Scale");

            trans.setRotation((float) orientation.getDouble("W"),
                              (float) orientation.getDouble("X"),
                              (float) orientation.getDouble("Y"),
                              (float) orientation.getDouble("Z"));
            trans.setPosition((float) position.getDouble("X"),
                              (float) position.getDouble("Y"),
                              (float) position.getDouble("Z"));
            trans.setScale((float) scale.getDouble("X"),
                           (float) scale.getDouble("Y"),
                           (float) scale.getDouble("Z"));
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
                    }
                }
            }
        }
        SXRSkeleton skel = new SXRSkeleton(mContext, boneparents);
        for (int i = 0; i <  numbones; ++i)
        {
            skel.setBoneName(i, bonenames[i]);
            skel.setBone(i, bones[i]);
        }
        mSkeleton = skel;
        bones[0].attachComponent(skel);
        skel.poseFromBones();
        skel.getPose().sync();
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
                throw new IllegalArgumentException("AVT file skeleton missing bone " + nodeName + " reference by MultiBody physics");
            }
            parseJoint(link);
        }
        return rootJoint;
    }

    private void parseCollider(JSONObject collider, String targetBone) throws JSONException
    {
        SXRContext ctx = mRoot.getSXRContext();
        JSONArray colliders = collider.getJSONObject("Child Colliders").getJSONArray("property_value");

        for (int i = 0; i < colliders.length(); ++i)
        {
            JSONObject c = colliders.getJSONObject(i).getJSONObject("Collider");
            String type = c.optString("type");

            if (type == null)
            {
                return;
            }
            int boneIndex = mSkeleton.getBoneIndex(targetBone);

            if (boneIndex < 0)
            {
                throw new JSONException("Cannot find bone " + targetBone + " needed by " + c.getString("Name") + " collider");
            }
            SXRNode owner = mSkeleton.getBone(boneIndex);
            c = c.getJSONObject("value");
            if (type.equals("dmCapsuleCollider"))
            {
                SXRCapsuleCollider capsule = new SXRCapsuleCollider(ctx);
                String direction = c.getString("Direction");
                float radius = (float) c.getDouble("Radius");
                float height = (float) c.getDouble("Half Height") * 2.0f;

                capsule.setRadius(radius);
                capsule.setHeight(height);
                if (direction.equals("X"))
                {
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.X_AXIS);
                }
                else if (direction.equals("Y"))
                {
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Y_AXIS);
                }
                else if (direction.equals("Z"))
                {
                    capsule.setDirection(SXRCapsuleCollider.CapsuleDirection.Z_AXIS);
                }
                owner.attachComponent(capsule);
                Log.e(TAG, "capsule collider %s height %f radius %f %s axis",
                        collider.getString("Name"), height, radius, direction);
            }
            else if (type.equals("dmBoxCollider"))
            {
                SXRBoxCollider box = new SXRBoxCollider(ctx);
                JSONObject size = c.getJSONObject("Half Size");
                box.setHalfExtents((float) size.getDouble("X"),
                                   (float) size.getDouble("Y"),
                                   (float) size.getDouble("Z"));
                owner.attachComponent(box);
                Log.e(TAG, "box collider %s", collider.getString("Name"));
            }
            else if (type.equals("dmSphereCollider"))
            {
                float radius = (float) c.getDouble("Radius");
                SXRSphereCollider sphere = new SXRSphereCollider(ctx);
                sphere.setRadius(radius);
                owner.attachComponent(sphere);
                Log.e(TAG, "box collider %s", collider.getString("Name"));
            }
            else
            {
                throw new JSONException(type + " is an unknown collider type");
            }
        }
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

        if (parentJoint == null)
        {
            Log.e(TAG, "Parent %s not found for child %s", parentName, name);
            return null;
        }
        mTargetBones.put(name, link);
        if (type.equals("ball"))
        {
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject dofz = dofdata.getJSONObject(2);
            JSONObject p = link.getJSONObject("Pivot Pos.");
            float[] pivotA = new float[3];

            pivotA[0] = (float) p.getDouble("X");
            pivotA[1] = (float) p.getDouble("Y");
            pivotA[2] = (float) p.getDouble("Z");
/*
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        (float) Math.toRadians(dofz.getDouble("limitLow")));
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofz.getDouble("limitHigh")));
            constraint = ball;
*/
            jointType = SXRPhysicsJoint.SPHERICAL;
            Log.e(TAG, "Ball joint between %s and %s joint(%f, %f, %f)",
                  parentName, name, pivotA[0], pivotA[1], pivotA[2]);
        }
        else if (type.equals("universal"))  // TODO: figure out universal joint
        {
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject p = link.getJSONObject("Pivot Pos.");
            float[] pivotA = new float[3];

            pivotA[0] = (float) p.getDouble("X");
            pivotA[1] = (float) p.getDouble("Y");
            pivotA[2] = (float) p.getDouble("Z");
/*
            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, joint,  pivotA);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                    (float) Math.toRadians(dofy.getDouble("limitLow")),
                    0);
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                    (float) Math.toRadians(dofy.getDouble("limitHigh")),
                    0);
            constraint = ball;
*/
            jointType = SXRPhysicsJoint.SPHERICAL;
            Log.e(TAG, "Universal joint between %s and %s joint(%f, %f, %f)",
                    parentName, name, pivotA[0], pivotA[1], pivotA[2]);
        }
        else if (type.equals("hinge"))
        {
            float[] axisA = new float[3];
            float[] pivotA = new float[3];
            float[] pivotB = new float[3];
            JSONObject v = link.getJSONObject("Axis A");
            JSONObject parent = mTargetBones.get(parentName);

            axisA[0] = (float)  v.getDouble("X");
            axisA[1] = (float)  v.getDouble("Y");
            axisA[2] = (float)  v.getDouble("Z");
            v = parent.getJSONObject("Pivot Pos.");
            pivotB[0] = (float)  v.getDouble("X");
            pivotB[1] = (float)  v.getDouble("Y");
            pivotB[2] = (float)  v.getDouble("Z");
/*
            SXRHingeConstraint hinge = new SXRHingeConstraint(mContext, joint,
                                    pivotA, pivotB, axisA);
            constraint = hinge;
            JSONObject dof = dofdata.getJSONObject(0);
            hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")),
                            (float) Math.toRadians(dof.getDouble("limitHigh")));
            Log.e(TAG, "Hinge joint between %s (%s, %s, %s) and %s (%f, %f, %f)",
                    parentName, name, pivotA[0], pivotA[1], pivotA[2], pivotB[0], pivotB[1], pivotB[2]);
*/
            jointType = SXRPhysicsJoint.REVOLUTE;
        }
        else if (type.equals("fixed"))
        {
/*
            SXRFixedConstraint fixed = new SXRFixedConstraint(mContext, joint);
            constraint = fixed;
*/
            Log.e(TAG, "Fixed joint between %s and %s", parentName, name);
            jointType = SXRPhysicsJoint.FIXED;
        }
        else
        {
            throw new JSONException(type + " is an unknown constraint type");
        }
        SXRPhysicsJoint joint = new SXRPhysicsJoint(parentJoint, jointType, boneID, mass);
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
        SXRRigidBody body = new SXRRigidBody(mContext, mass);

        if (node == null)
        {
            throw new JSONException("Cannot find bone " + nodeName + " referenced by AVT file");
        }
        parseCollider(link.getJSONObject("Collider").getJSONObject("value"), nodeName);
        mTargetBones.put(name, link);
        if (parentBody == null)
        {
            return null;
        }
        String type = link.getString("Joint Type");
        SXRConstraint constraint;
        JSONArray dofdata = link.getJSONArray("DOF Data");
        if (type.equals("ball"))
        {
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject dofz = dofdata.getJSONObject(2);
            JSONObject p = link.getJSONObject("Pivot Pos.");
            float[] pivotA = new float[3];

            pivotA[0] = (float) p.getDouble("X");
            pivotA[1] = (float) p.getDouble("Y");
            pivotA[2] = (float) p.getDouble("Z");
            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, parentBody, pivotA);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        (float) Math.toRadians(dofz.getDouble("limitLow")));
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                       (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                       (float) Math.toRadians(dofz.getDouble("limitHigh")));
            constraint = ball;
            Log.e(TAG, "Ball joint between %s and %s joint(%f, %f, %f)",
                  parentName, name, pivotA[0], pivotA[1], pivotA[2]);
        }
        else if (type.equals("universal"))  // TODO: figure out universal joint
        {
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject p = link.getJSONObject("Pivot Pos.");
            float[] pivotA = new float[3];

            pivotA[0] = (float) p.getDouble("X");
            pivotA[1] = (float) p.getDouble("Y");
            pivotA[2] = (float) p.getDouble("Z");
            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, parentBody,  pivotA);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        0);
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                       (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                       0);
            constraint = ball;
            Log.e(TAG, "Universal joint between %s and %s joint(%f, %f, %f)",
                  parentName, name, pivotA[0], pivotA[1], pivotA[2]);
        }
        else if (type.equals("hinge"))
        {
            float[] axisA = new float[3];
            float[] axisB = new float[3];
            float[] pivotA = new float[3];
            float[] pivotB = new float[3];
            JSONObject v = link.getJSONObject("Axis A");
            JSONObject parent = mTargetBones.get(parentName);

            axisA[0] = (float)  v.getDouble("X");
            axisA[1] = (float)  v.getDouble("Y");
            axisA[2] = (float)  v.getDouble("Z");
            v = parent.getJSONObject("Pivot Pos.");
            pivotB[0] = (float)  v.getDouble("X");
            pivotB[1] = (float)  v.getDouble("Y");
            pivotB[2] = (float)  v.getDouble("Z");

            SXRHingeConstraint hinge = new SXRHingeConstraint(mContext, parentBody,
                                                              pivotA, pivotB, axisA);
            constraint = hinge;
            JSONObject dof = dofdata.getJSONObject(0);
            hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")),
                            (float) Math.toRadians(dof.getDouble("limitHigh")));
            Log.e(TAG, "Hinge joint between %s (%s, %s, %s) and %s (%f, %f, %f)",
                  parentName, name, pivotA[0], pivotA[1], pivotA[2], pivotB[0], pivotB[1], pivotB[2]);
        }
        else if (type.equals("fixed"))
        {
            SXRFixedConstraint fixed = new SXRFixedConstraint(mContext, parentBody);
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
        node.attachComponent(body);
        node.attachComponent(constraint);
        return body;
    }

    private void parseBone(JSONObject link, SXRPhysicsJoint joint) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mSkeleton.getBone(joint.getBoneID());
        parseCollider(link.getJSONObject("Collider").getJSONObject("value"), nodeName);
        JSONObject props = link.getJSONObject("Physic Material");

        joint.setFriction((float) props.getDouble("Friction"));
        node.attachComponent(joint);
        Log.e(TAG, "link %s bone = %s boneID = %d ",
              link.getString("Name"),
              nodeName,
              joint.getBoneID());
    }

}

