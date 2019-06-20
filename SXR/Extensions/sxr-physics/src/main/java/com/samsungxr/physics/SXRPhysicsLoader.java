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
import com.samsungxr.SXRCollider;
import com.samsungxr.SXRComponent;
import com.samsungxr.SXRComponentGroup;
import com.samsungxr.SXRContext;
import com.samsungxr.SXRMeshCollider;
import com.samsungxr.SXRResourceVolume;
import com.samsungxr.SXRScene;
import com.samsungxr.SXRNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SXRPhysicsLoader {
    static private final String TAG = SXRPhysicsLoader.class.getSimpleName();

    static {
        System.loadLibrary("sxr-physics");
    }

    /**
     * Loads a physics settings file.
     *
     * @param fileName Physics settings file name.
     * @param scene The scene containing the objects to attach physics components.
     */
    public static void loadPhysicsFile(SXRScene scene, String fileName) throws IOException
    {
        loadPhysicsFile(scene, fileName, false);
    }

    /**
     * Loads a physics settings file.
     *
     * Use this if you want the up-axis information from physics file to be ignored.
     *
     * @param fileName Physics settings file name.
     * @param ignoreUpAxis Set to true if up-axis information from file must be ignored.
     * @param scene The scene containing the objects to attach physics components.
     */
    public static void loadPhysicsFile(SXRScene scene, String fileName, boolean ignoreUpAxis) throws IOException
    {
        SXRAndroidResource resource = toAndroidResource(scene.getSXRContext(), fileName);
        loadPhysicsFile(resource, scene.getRoot(), null, ignoreUpAxis);
    }

    public static void loadPhysicsFile(SXRAndroidResource resource, SXRNode sceneRoot, SXRWorld world, boolean ignoreUpAxis) throws IOException
    {
        String filename = resource.getResourceFilename();
        int i = filename.lastIndexOf('.');
        byte[] inputData = toByteArray(resource);

        if (inputData == null || inputData.length == 0)
        {
            throw new IOException("Failed to load physics file " + filename);
        }
        if (i > 0)
        {
            String ext = filename.substring(i);
            if (ext.toLowerCase().equals(".bullet"))
            {
                loadBulletFile(inputData, sceneRoot, ignoreUpAxis);
            }
            else if (ext.equals(".avt"))
            {
                if (world == null)
                {
                    world = new SXRWorld(sceneRoot.getSXRContext());
                }
                PhysicsAVTLoader loader = new PhysicsAVTLoader(sceneRoot, world);
                loader.parse(inputData);
            }
            else
            {
                throw new IOException(ext + "is not a supported physics file format");
            }
        }
        else
        {
            throw new IOException("Cannot determine file extension");
        }
    }

    private static void loadBulletFile(byte[] inputData, SXRNode sceneRoot, boolean ignoreUpAxis) throws IOException
    {
        long loader = NativePhysics3DLoader.ctor(inputData, inputData.length, ignoreUpAxis);

        if (loader == 0)
        {
            throw new IOException("Failed to parse bullet file");
        }
        SXRContext ctx = sceneRoot.getSXRContext();
        ArrayMap<Long, SXRNode> rbObjects = new ArrayMap<>();

        long nativeRigidBody;
        while ((nativeRigidBody = NativePhysics3DLoader.getNextRigidBody(loader)) != 0) {
            String name = NativePhysics3DLoader.getRigidBodyName(loader, nativeRigidBody);
            SXRNode sceneObject = sceneRoot.getNodeByName(name);
            if (sceneObject == null) {
                Log.w(TAG, "Didn't find node for rigid body '" + name + "'");
                continue;
            }

            if (sceneObject.getComponent(SXRCollider.getComponentType()) == null) {
                SXRMeshCollider collider = new SXRMeshCollider(ctx, true);
                // Collider for picking.
                sceneObject.attachComponent(collider);
            }
            SXRRigidBody rigidBody = new SXRRigidBody(ctx, nativeRigidBody);
            sceneObject.attachComponent(rigidBody);
            rbObjects.put(nativeRigidBody, sceneObject);
        }

        long nativeConstraint;
        long nativeRigidBodyB;

        while ((nativeConstraint = NativePhysics3DLoader.getNextConstraint(loader)) != 0) {
            nativeRigidBody = NativePhysics3DLoader.getConstraintBodyA(loader, nativeConstraint);
            nativeRigidBodyB = NativePhysics3DLoader.getConstraintBodyB(loader, nativeConstraint);
            SXRNode sceneObject = rbObjects.get(nativeRigidBody);
            SXRNode sceneObjectB = rbObjects.get(nativeRigidBodyB);

            if (sceneObject == null || sceneObjectB == null) {
                // There is no node to own this constraint
                Log.w(TAG, "Ignoring constraint with missing rigid body.");
                continue;
            }

            int constraintType = Native3DConstraint.getConstraintType(nativeConstraint);
            SXRConstraint constraint = null;

            if (constraintType == SXRConstraint.fixedConstraintId) {
                constraint = new SXRFixedConstraint(ctx, nativeConstraint);
            } else if (constraintType == SXRConstraint.point2pointConstraintId) {
                constraint = new SXRPoint2PointConstraint(ctx, nativeConstraint);
            } else if (constraintType == SXRConstraint.sliderConstraintId) {
                constraint = new SXRSliderConstraint(ctx, nativeConstraint);
            } else if (constraintType == SXRConstraint.hingeConstraintId) {
                constraint = new SXRHingeConstraint(ctx, nativeConstraint);
            } else if (constraintType == SXRConstraint.coneTwistConstraintId) {
                constraint = new SXRConeTwistConstraint(ctx, nativeConstraint);
            } else if (constraintType == SXRConstraint.genericConstraintId) {
                constraint = new SXRGenericConstraint(ctx, nativeConstraint);
            }

            if (constraint != null) {
                SXRComponentGroup<SXRConstraint> group;
                group = (SXRComponentGroup)sceneObject.getComponent(SXRConstraint.getComponentType());
                if (group == null) {
                    group = new SXRComponentGroup<>(ctx, SXRConstraint.getComponentType());
                    sceneObject.attachComponent(group);
                }
                group.addChildComponent(constraint);
                constraint.setOwnerObject(sceneObject);
            }
        }
        NativePhysics3DLoader.delete(loader);
    }

    private static byte[] toByteArray(SXRAndroidResource resource) throws IOException {
        resource.openStream();
        InputStream is = resource.getStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int read; (read = is.read(buffer, 0, buffer.length)) != -1; ) {
            baos.write(buffer, 0, read);
        }
        baos.flush();
        resource.closeStream();
        return  baos.toByteArray();
    }

    private static SXRAndroidResource toAndroidResource(SXRContext context, String fileName) throws IOException {
        SXRResourceVolume resVol = new SXRResourceVolume(context, fileName);

        final int i = fileName.lastIndexOf("/");
        if (i > 0) {
            fileName = fileName.substring(i + 1);
        }

        return resVol.openResource(fileName);
    }
}

class NativePhysics3DLoader {
    static native long ctor(byte[] bytes, int len, boolean ignoreUpAxis);

    static native long delete(long loader);

    static native long getNextRigidBody(long loader);

    static native String getRigidBodyName(long loader, long rigid_body);

    static native long getNextConstraint(long loader);

    static native long getConstraintBodyA(long loader, long constraint);

    static native long getConstraintBodyB(long loader, long constraint);
}
