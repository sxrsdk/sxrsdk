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

import android.util.ArrayMap;
import android.util.Log;

import com.samsungxr.SXRAndroidResource;
import com.samsungxr.SXRCapsuleCollider;
import com.samsungxr.SXRCollider;
import com.samsungxr.SXRComponentGroup;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class PhysicsAVTLoader
{
    private final SXRScene mScene;
    private final SXRContext mContext;
    private final SXRWorld mWorld;
    private final Map<String, String> mTargetBones = new HashMap<String, String>();

    public PhysicsAVTLoader(SXRScene scene, SXRWorld world)
    {
        mWorld = world;
        mScene = scene;
        mContext = scene.getSXRContext();
    }

    public void parse(byte[] inputData) throws IOException
    {
        mTargetBones.clear();
        String s = new String(inputData);
        try
        {
            JSONObject start = new JSONObject(s);
            JSONObject multibody = start.getJSONObject("MultiBody").getJSONObject("value");
            JSONObject basebone = multibody.getJSONObject("Base Bone");
            JSONArray childbones = multibody.getJSONObject("Child Bones").getJSONArray("property_value");

            mTargetBones.put(basebone.getString("Name"), basebone.getString("Target Bone"));
            parseCollider(basebone.getJSONObject("Collider").getJSONObject("value"));
            for (int i = 0; i < childbones.length(); ++i)
            {
                parseConstraint(childbones.getJSONObject(i).getJSONObject("value"));
            }
        }
        catch (JSONException ex)
        {
            throw new IOException(ex);
        }
    }

    private void parseCollider(JSONObject collider) throws JSONException
    {
        SXRContext ctx = mScene.getSXRContext();
        JSONArray colliders = collider.getJSONObject("Child Colliders").getJSONArray("property_value");

        for (int i = 0; i < colliders.length(); ++i)
        {
            JSONObject c = colliders.getJSONObject(i);
            String type = collider.optString("type");

            if (type == null)
            {
                return;
            }
            if (type.equals("dmCapsuleCollider"))
            {
                String bone = c.getString("Bone");
                SXRNode owner = mScene.getNodeByName(bone);

                if (owner == null)
                {
                    throw new JSONException("Cannot find bone " + bone + " needed by collider");
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
            }
            else
            {
                throw new JSONException(type + " is an unknown collider type");
            }
        }
    }

    private void parseConstraint(JSONObject link) throws JSONException
    {
        String nodeName = link.getString("Target Bone");
        SXRNode node = mScene.getNodeByName(nodeName);

        if (node == null)
        {
            throw new JSONException("Cannot find bone " + nodeName + " referenced by AVT file");
        }
        mTargetBones.put(link.getString("Name"), nodeName);
        String parentName = mTargetBones.get(link.getString("Parent"));
        SXRNode owner = mScene.getNodeByName(parentName);
        String type = link.getString("Joint Type");
        SXRConstraint constraint;
        JSONArray dofdata = link.getJSONArray("DOF Data");
        SXRRigidBody body = parseRigidBody(link);

        node.attachComponent(body);
        if (type.equals("ball"))
        {
            JSONObject dofx = dofdata.getJSONObject(0);
            JSONObject dofy = dofdata.getJSONObject(1);
            JSONObject dofz = dofdata.getJSONObject(2);
            JSONObject p = link.getJSONObject("Pivot Pos.");
            float[] joint = new float[3];
            float[] rotA = new float[9]; // TODO: figure out rotA and rotB
            float[] rotB = new float[9];

            joint[0] = (float) p.getDouble("X");
            joint[1] = (float) p.getDouble("Y");
            joint[2] = (float) p.getDouble("Z");

            SXRGenericConstraint ball = new SXRGenericConstraint(mContext, body,  joint, rotA, rotB);
            ball.setAngularLowerLimits( (float) Math.toRadians(dofx.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")),
                                        (float) Math.toRadians(dofy.getDouble("limitLow")));
            ball.setAngularUpperLimits((float) Math.toRadians(dofx.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofy.getDouble("limitHigh")),
                                        (float) Math.toRadians(dofy.getDouble("limitHigh")));
            constraint = ball;
        }
        else if (type.equals("hinge"))
        {
            float[] axisA = new float[3];
            float[] axisB = new float[3];
            float[] pivotA = new float[3]; // TODO: get pivot point from parent
            float[] pivotB = new float[3];
            JSONObject v = link.getJSONObject("Axis A");
            axisA[0] = (float)  v.getDouble("X");
            axisA[1] = (float)  v.getDouble("Y");
            axisA[2] = (float)  v.getDouble("Z");
            v = link.getJSONObject("Axis B");
            axisB[0] = (float)  v.getDouble("X");
            axisB[1] = (float)  v.getDouble("Y");
            axisB[2] = (float)  v.getDouble("Z");
            v = link.getJSONObject("Pivot Pos.");
            pivotB[0] = (float)  v.getDouble("X");
            pivotB[1] = (float)  v.getDouble("Y");
            pivotB[2] = (float)  v.getDouble("Z");

            SXRHingeConstraint hinge = new SXRHingeConstraint(mContext, body,
                                    pivotA, pivotB, axisA, axisB);
            constraint = hinge;
            JSONObject dof = dofdata.getJSONObject(0);
            hinge.setLimits((float) Math.toRadians(dof.getDouble("limitLow")),
                            (float) Math.toRadians(dof.getDouble("limitHigh")));
        }
        else if (type.equals("fixed"))
        {
            SXRFixedConstraint fixed = new SXRFixedConstraint(mContext, body);
            constraint = fixed;
        }
        else
        {
            throw new JSONException(type + " is an unknown constraint type");
        }
        if (link.has("Breaking Rection Impulse"))
        {
            constraint.setBreakingImpulse((float) link.getDouble("Breaking Reaction Impulse"));
        }
        parseCollider(link.getJSONObject("Collider").getJSONObject("value"));
        owner.attachComponent(constraint);
        mWorld.addConstraint(constraint);
    }

    private SXRRigidBody parseRigidBody(JSONObject link) throws JSONException
    {
        float mass = (float) link.getDouble("Mass");
        int group = link.getInt("Collision Layer ID");
        SXRRigidBody body = new SXRRigidBody(mContext, mass, group);
        JSONObject props = link.getJSONObject("Physic Material");
        body.setFriction((float) props.getDouble("Friction"));
        body.setRestitution((float) props.getDouble("Restitution"));
        mWorld.addBody(body);
        return body;
    }

}

