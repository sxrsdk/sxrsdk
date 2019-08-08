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
    private final SXRNode mRoot;
    private final SXRContext mContext;
    private final Map<String, JSONObject> mTargetBones = new HashMap<String, JSONObject>();

    public PhysicsAVTLoader(SXRNode root)
    {
        mRoot = root;
        mContext = root.getSXRContext();
    }

    public void parse(byte[] inputData) throws IOException
    {
        mTargetBones.clear();
        String s = new String(inputData);
        Log.e("PHYSICS", "loading physics file");
        try
        {
            JSONObject start = new JSONObject(s);
            JSONObject multibody = start.getJSONObject("Multi Body").getJSONObject("value");
            JSONObject basebone = multibody.getJSONObject("Base Bone");
            JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");
            float mass = (float) basebone.getDouble("Mass");
            SXRPhysicsJoint rootJoint = new SXRPhysicsJoint(mContext, mass, childbones.length());

            mTargetBones.put(basebone.getString("Name"), basebone);
            parseBone(basebone, rootJoint);
            for (int i = 0; i < childbones.length(); ++i)
            {
                parseJoint(childbones.getJSONObject(i).getJSONObject("value"), i);
            }
            Log.e("PHYSICS", "loading done");
        }
        catch (JSONException ex)
        {
            throw new IOException(ex);
        }
    }

    private void parsePhysics(JSONObject root) throws JSONException
    {
        JSONObject multibody = root.getJSONObject("Multi Body").getJSONObject("value");
        JSONObject basebone = multibody.getJSONObject("Base Bone");
        JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");
        float mass = (float) basebone.getDouble("Mass");

        mTargetBones.put(basebone.getString("Name"), basebone);
        parseCollider(basebone.getJSONObject("Collider").getJSONObject("value"), basebone.getString("Target Bone"));
        SXRPhysicsJoint rootJoint = new SXRPhysicsJoint(mContext, mass, childbones.length());
        parseBone(basebone, rootJoint);
        for (int i = 0; i < childbones.length(); ++i)
        {
            parseJoint(childbones.getJSONObject(i).getJSONObject("value"), i);
        }
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
            c = c.getJSONObject("value");
            if (type.equals("dmCapsuleCollider"))
            {
                SXRNode owner = mRoot.getNodeByName(targetBone);

                if (owner == null)
                {
                    throw new JSONException("Cannot find bone " + targetBone + " needed by " + c.getString("Name") + " collider");
                }

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
                Log.e("PHYSICS", "capsule collider %s height %f radius %f %s axis",
                        collider.getString("Name"), height, radius, direction);
            }
            else if (type.equals("dmBoxCollider"))
            {
                SXRNode owner = mRoot.getNodeByName(targetBone);

                if (owner == null)
                {
                    throw new JSONException("Cannot find bone " + targetBone + " needed by " + c.getString("Name") + " collider");
                }

                SXRBoxCollider box = new SXRBoxCollider(ctx);
                JSONObject size = c.getJSONObject("Half Size");
                box.setHalfExtents((float) size.getDouble("X"),
                                   (float) size.getDouble("Y"),
                                   (float) size.getDouble("Z"));
                owner.attachComponent(box);
                Log.e("PHYSICS", "box collider %s", collider.getString("Name"));
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
        SXRNode parentNode = mRoot.getNodeByName(nodeName);

        if (parentNode != null)
        {
            SXRPhysicsJoint joint = (SXRPhysicsJoint) parentNode.getComponent(SXRPhysicsJoint.getComponentType());
            if (joint != null)
            {
                return joint;
            }
        }
        Log.e("PHYSICS", "Cannot find bone %s referenced by AVT file", nodeName);
        return null;
    }

    private SXRPhysicsJoint parseJoint(JSONObject link, int boneID) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mRoot.getNodeByName(nodeName);

        if (node == null)
        {
            Log.e("PHYSICS","Cannot find bone " + nodeName + " referenced by AVT file");
            return null;
        }
        String name = link.getString("Name");
        String parentName = link.getString("Parent");
        String type = link.getString("Joint Type");
        SXRConstraint constraint;
        JSONArray dofdata = link.getJSONArray("DOF Data");
        float mass = (float) link.getDouble("Mass");
        SXRPhysicsJoint parentJoint = findParentJoint(parentName);

        if (parentJoint == null)
        {
            return null;
        }
        SXRPhysicsJoint joint = new SXRPhysicsJoint(parentJoint, boneID, mass);
;
        parseBone(link, joint);
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
            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, joint, pivotA);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        (float) Math.toRadians(dofz.getDouble("limitLow")));
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofz.getDouble("limitHigh")));
            constraint = ball;
            Log.e("PHYSICS", "Ball joint between %s and %s joint(%f, %f, %f)",
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
            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, joint,  pivotA);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                    (float) Math.toRadians(dofy.getDouble("limitLow")),
                    0);
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                    (float) Math.toRadians(dofy.getDouble("limitHigh")),
                    0);
            constraint = ball;
            Log.e("PHYSICS", "Universal joint between %s and %s joint(%f, %f, %f)",
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

            SXRHingeConstraint hinge = new SXRHingeConstraint(mContext, joint,
                                    pivotA, pivotB, axisA);
            constraint = hinge;
            JSONObject dof = dofdata.getJSONObject(0);
            hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")),
                            (float) Math.toRadians(dof.getDouble("limitHigh")));
            Log.e("PHYSICS", "Hinge joint between %s (%s, %s, %s) and %s (%f, %f, %f)",
                    parentName, name, pivotA[0], pivotA[1], pivotA[2], pivotB[0], pivotB[1], pivotB[2]);
        }
        else if (type.equals("fixed"))
        {
            SXRFixedConstraint fixed = new SXRFixedConstraint(mContext, joint);
            constraint = fixed;
            Log.e("PHYSICS", "Fixed joint between %s and %s", parentName, name);
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
        node.attachComponent(joint);
        return joint;
    }

    private void parseBone(JSONObject link, SXRPhysicsJoint joint) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mRoot.getNodeByName(nodeName);
        if (node == null)
        {
            throw new JSONException("Cannot find bone " + nodeName + " referenced by AVT file");
        }
        parseCollider(link.getJSONObject("Collider").getJSONObject("value"), nodeName);
        JSONObject props = link.getJSONObject("Physic Material");
        joint.setFriction((float) props.getDouble("Friction"));
        Log.e("PHYSICS", "rigidbody %s bone = %s boneID = %d",
                link.getString("Name"), nodeName, joint.getBoneID());
    }

}

