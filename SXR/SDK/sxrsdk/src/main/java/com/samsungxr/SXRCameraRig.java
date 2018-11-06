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

package com.samsungxr;

import com.samsungxr.utility.Log;

import static com.samsungxr.utility.Assert.checkFloatNotNaNOrInfinity;
import static com.samsungxr.utility.Assert.checkStringNotNullOrEmpty;

/** Holds the SXRCameras. */
public class SXRCameraRig extends SXRComponent implements PrettyPrint {
    private SXRNode headTransformObject;

    private SXRCamera leftCamera, rightCamera;
    private SXRPerspectiveCamera centerCamera;

    private SXRNode leftCameraObject, rightCameraObject;
    private SXRNode centerCameraObject;

    /** Ways to use the rotation sensor data. */
    public abstract static class SXRCameraRigType {
        /** Rotates freely. Default. */
        public abstract static class Free {
            public static final int ID = 0;
        }

        /** Yaw rotation (naively speaking, rotation by y-axis) only. */
        public abstract static class YawOnly {
            public static final int ID = 1;
        }

        /** No roll rotation (naively speaking, rotation by z-axis). */
        public abstract static class RollFreeze {
            public static final int ID = 2;
        }

        /** No rotation at all. */
        public abstract static class Freeze {
            public static final int ID = 3;
        }

        /** Orbits the pivot. */
        public abstract static class OrbitPivot {
            public static final int ID = 4;
            public static final String DISTANCE = "distance";
            public static final String PIVOT = "pivot";
        }
    };

    static public long getComponentType() {
        long type = NativeCameraRig.getComponentType();
        Log.d("ND", "ND: SXRCameraRig.getComponentType JAVA %d", type);
        return type;
    }

    /**
     * Constructs a camera rig with cameras attached. An owner node is automatically
     * created for the camera rig.
     *
     * Do not try to change the owner object of the camera rig - not supported currently and will
     * lead to native crashes.
     */
    public static SXRCameraRig makeInstance(SXRContext gvrContext) {
        final SXRCameraRig result = gvrContext.getApplication().getDelegate().makeCameraRig(gvrContext);
        result.init(gvrContext);
        return result;
    }

    protected SXRCameraRig(SXRContext gvrContext) {
        super(gvrContext, NativeCameraRig.ctor());
    }

    protected SXRCameraRig(SXRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
    }

    /** Construction helper */
    protected void init(SXRContext gvrContext) {
        /*
         * Create an object hierarchy
         *
         *             [camera rig object]
         *                     |
         *           [head transform object]
         *            /        |          \
         * [left cam obj] [right cam obj] [center cam obj]
         *
         * 1. camera rig object: used for camera rig moving and turning via
         *    CameraRig.getTransform()
         * 2. head transform object: used internally to do sensor-based rotation
         */
        setOwnerObject(new SXRNode(gvrContext));
        getOwnerObject().attachCameraRig(this);

        headTransformObject = new SXRNode(gvrContext);
        addHeadTransformObject();

        leftCameraObject = new SXRNode(gvrContext);
        rightCameraObject = new SXRNode(gvrContext);
        centerCameraObject = new SXRNode(gvrContext);

        headTransformObject.addChildObject(leftCameraObject);
        headTransformObject.addChildObject(rightCameraObject);
        headTransformObject.addChildObject(centerCameraObject);
    }

    protected void addHeadTransformObject() {
        getOwnerObject().addChildObject(getHeadTransformObject());
    }

    public final SXRNode getHeadTransformObject() {
        return headTransformObject;
    }

    /** @return The {@link SXRCameraRigType type} of the camera rig. */
    public int getCameraRigType() {
        return NativeCameraRig.getCameraRigType(getNative());
    }

    /**
     * Set the {@link SXRCameraRigType type} of the camera rig.
     * 
     * @param cameraRigType
     *            The rig {@link SXRCameraRigType type}.
     */
    public void setCameraRigType(int cameraRigType) {
        NativeCameraRig.setCameraRigType(getNative(), cameraRigType);
    }

    /**
     * @return Get the left {@link SXRCamera camera}, if one has been
     *         {@link #attachLeftCamera(SXRCamera) attached}; {@code null} if
     *         not.
     */
    public SXRCamera getLeftCamera() {
        return leftCamera;
    }

    /**
     * @return Get the right {@link SXRCamera camera}, if one has been
     *         {@link #attachRightCamera(SXRCamera) attached}; {@code null} if
     *         not.
     */
    public SXRCamera getRightCamera() {
        return rightCamera;
    }

    /**
     * @return Get the center {@link SXRPerspectiveCamera camera}, if one has been
     *         {@link #attachCenterCamera(SXRPerspectiveCamera) attached}; {@code null} if
     *         not.
     */
    public SXRPerspectiveCamera getCenterCamera() {
        return centerCamera;
    }

    /**
     * @return The global default distance separating the left and right
     *         cameras.
     */
    public static float getDefaultCameraSeparationDistance() {
        return NativeCameraRig.getDefaultCameraSeparationDistance();
    }

    /**
     * Sets the global default distance separating the left and right cameras.
     * 
     * @param distance
     *            Global default separation.
     */
    public static void setDefaultCameraSeparationDistance(float distance) {
        NativeCameraRig.setDefaultCameraSeparationDistance(distance);
    }

    /**
     * @return The distance separating the left and right cameras of the camera
     *         rig.
     */
    public float getCameraSeparationDistance() {
        return NativeCameraRig.getCameraSeparationDistance(getNative());
    }

    /**
     * Set the distance separating the left and right cameras of the camera rig.
     * 
     * @param distance
     *            Separation distance.
     */
    public void setCameraSeparationDistance(float distance) {
        NativeCameraRig.setCameraSeparationDistance(getNative(), distance);
    }

    /**
     * @param key
     *            Key of the {@code float} to get.
     * @return The {@code float} value associated with {@code key}.
     */
    public float getFloat(String key) {
        return NativeCameraRig.getFloat(getNative(), key);
    }

    /**
     * Map {@code value} to {@code key}.
     * 
     * @param key
     *            Key to map {@code value} to.
     * @param value
     *            The {@code float} value to map.
     */
    public void setFloat(String key, float value) {
        checkStringNotNullOrEmpty("key", key);
        checkFloatNotNaNOrInfinity("value", value);
        NativeCameraRig.setFloat(getNative(), key, value);
    }

    /**
     * @param key
     *            Key of the two-component {@code float} vector to get.
     * @return An two-element array representing the vector mapped to
     *         {@code key}.
     */
    public float[] getVec2(String key) {
        return NativeCameraRig.getVec2(getNative(), key);
    }

    /**
     * Map a two-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     */
    public void setVec2(String key, float x, float y) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec2(getNative(), key, x, y);
    }

    /**
     * @param key
     *            Key of the three-component {@code float} vector to get.
     * @return An three-element array representing the vector mapped to
     *         {@code key}.
     */
    public float[] getVec3(String key) {
        return NativeCameraRig.getVec3(getNative(), key);
    }

    /**
     * Map a three-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     * @param z
     *            'Z' component of vector.
     */
    public void setVec3(String key, float x, float y, float z) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec3(getNative(), key, x, y, z);
    }

    /**
     * @param key
     *            Key of the four-component {@code float} vector to get.
     * @return An four-element array representing the vector mapped to
     *         {@code key} .
     */
    public float[] getVec4(String key) {
        return NativeCameraRig.getVec4(getNative(), key);
    }

    /**
     * Map a four-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     * @param z
     *            'Z' component of vector.
     * @param w
     *            'W' component of vector.
     */
    public void setVec4(String key, float x, float y, float z, float w) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec4(getNative(), key, x, y, z, w);
    }

    /**
     * Attach a {@link SXRCamera camera} as the left camera of the camera rig.
     * 
     * @param camera
     *            {@link SXRCamera Camera} to attach.
     */
    public void attachLeftCamera(SXRCamera camera) {
        if (camera.hasOwnerObject()) {
            camera.getOwnerObject().detachCamera();
        }

        leftCameraObject.attachCamera(camera);
        leftCamera = camera;
        NativeCameraRig.attachLeftCamera(getNative(), camera.getNative());
    }

    /**
     * Attach a {@link SXRCamera camera} as the right camera of the camera rig.
     * 
     * @param camera
     *            {@link SXRCamera Camera} to attach.
     */
    public void attachRightCamera(SXRCamera camera) {
        if (camera.hasOwnerObject()) {
            camera.getOwnerObject().detachCamera();
        }

        rightCameraObject.attachCamera(camera);
        rightCamera = camera;
        NativeCameraRig.attachRightCamera(getNative(), camera.getNative());
    }

    /**
     * Attach a {@link SXRPerspectiveCamera camera} as the center camera of the camera rig.
     * 
     * @param camera
     *            {@link SXRPerspectiveCamera Camera} to attach.
     */
    public void attachCenterCamera(SXRPerspectiveCamera camera) {
        if (camera.hasOwnerObject()) {
            camera.getOwnerObject().detachCamera();
        }

        centerCameraObject.attachCamera(camera);
        centerCamera = camera;
        NativeCameraRig.attachCenterCamera(getNative(), camera.getNative());
    }

    public void attachToParent(SXRNode parentObject) {
        parentObject.addChildObject(getOwnerObject());
    }

    public void detachFromParent(SXRNode parentObject) {
       parentObject.removeChildObject(getOwnerObject());
    }

    /**
     * Resets the rotation of the camera rig by multiplying further rotations by
     * the inverse of the current rotation.
     * <p>
     * Cancels the effect of prior calls to {@link #resetYaw()} and
     * {@link #resetYawPitch()}.
     */
    public void reset() {
        NativeCameraRig.reset(getNative());
    }

    /**
     * Resets the yaw of the camera rig by multiplying further changes in the
     * rig's yaw by the inverse of the current yaw.
     * <p>
     * Cancels the effect of prior calls to {@link #reset()} and
     * {@link #resetYawPitch()}.
     */
    public void resetYaw() {
        NativeCameraRig.resetYaw(getNative());
    }

    /**
     * Resets the yaw and pitch of the camera rig by multiplying further changes
     * in the rig's yaw and pitch by the inverse of the current yaw and pitch.
     * <p>
     * Cancels the effect of prior calls to {@link #reset()} and
     * {@link #resetYaw()}.
     */
    public void resetYawPitch() {
        NativeCameraRig.resetYawPitch(getNative());
    }

    /**
     * Sets the rotation and angular velocity data for the camera rig. This
     * should only be done in response to
     * {@link OvrRotationSensorListener#onRotationSensor(long, float, float, float, float, float, float, float)
     * OvrRotationSensorListener.onRotationSensor()}.
     * 
     * @param timeStamp
     *            Clock-time when the data was received, in nanoseconds.
     * @param w
     *            The 'W' rotation component.
     * @param x
     *            The 'X' rotation component.
     * @param y
     *            The 'Y' rotation component.
     * @param z
     *            The 'Z' rotation component.
     * @param gyroX
     *            Angular velocity on the 'X' axis.
     * @param gyroY
     *            Angular velocity on the 'Y' axis.
     * @param gyroZ
     *            Angular velocity on the 'Z' axis.
     */
    void setRotationSensorData(long timeStamp, float w, float x, float y,
            float z, float gyroX, float gyroY, float gyroZ) {
        NativeCameraRig.setRotationSensorData(getNative(), timeStamp, w, x, y,
                z, gyroX, gyroY, gyroZ);
    }

    /**
     * The direction the camera rig is looking at. In other words, the direction
     * of the local -z axis.
     * 
     * @return Array with 3 floats corresponding to a normalized direction
     *         vector. ([0] : x, [1] : y, [2] : z)
     */
    public float[] getLookAt() {
        return NativeCameraRig.getLookAt(getNative());
    }

    /**
     * Replace the current {@link SXRTransform transform} for owner object of
     * the camera rig.
     * 
     * @param transform
     *            New transform.
     */
    void attachTransform(SXRTransform transform) {
        if (getOwnerObject() != null) {
            getOwnerObject().attachTransform(transform);
        }
    }

    /**
     * Remove the object's (owner object of camera rig) {@link SXRTransform
     * transform}.
     * 
     */
    void detachTransform() {
        if (getOwnerObject() != null) {
            getOwnerObject().detachTransform();
        }
    }

    /**
     * Get the {@link SXRTransform}.
     * 
     * 
     * @return The current {@link SXRTransform transform} of owner object of
     *         camera rig. Applying transform to owner object of camera rig
     *         moves it. If no transform is currently attached to the object,
     *         returns {@code null}.
     */
    public SXRTransform getTransform() {
        if (getOwnerObject() != null) {
            return getOwnerObject().getTransform();
        }
        return null;
    }

    /**
     * Add {@code child} as a child of this camera rig owner object.
     * 
     * @param child
     *            {@link SXRNode Object} to add as a child of this camera
     *            rig owner object.
     */
    public void addChildObject(SXRNode child) {
        headTransformObject.addChildObject(child);
    }

    /**
     * Remove {@code child} as a child of this camera rig owner object.
     * 
     * @param child
     *            {@link SXRNode Object} to remove as a child of this
     *            camera rig owner object.
     */
    public void removeChildObject(SXRNode child) {
        headTransformObject.removeChildObject(child);
    }

    /**
     * Get the number of child objects that belongs to owner object of this
     * camera rig.
     * 
     * @return Number of {@link SXRNode objects} added as children of
     *         this camera rig owner object.
     */
    public int getChildrenCount() {
        return headTransformObject.getChildrenCount();
    }

    /**
     * Remove all children that have been added to the owner object of this camera rig; except the
     * camera objects.
     */
    public void removeAllChildren() {
        for (final SXRNode so : headTransformObject.getChildren()) {
            final boolean notCamera = (so != leftCameraObject && so != rightCameraObject && so != centerCameraObject);
            if (notCamera) {
                headTransformObject.removeChildObject(so);
            }
        }
    }

    /**
     * Get the head {@link SXRTransform transform} for setting sensor data. In contrast,
     * use {@link #getTransform()} for additional camera positioning, such as the game
     * character moving and turning.
     *
     * @return The head {@link SXRTransform transform} object.
     */
    public SXRTransform getHeadTransform() {
        return getHeadTransformObject().getTransform();
    }

    /**
     * Update the rotation transform from the latest sensor data on file
     */
    void updateRotation() {
        NativeCameraRig.updateRotation(getNative());
    }

    /**
     * @return Distance from the origin to the near clipping plane for the
     *         camera rig.
     */
    public float getNearClippingDistance() {
        if(leftCamera instanceof SXRCameraClippingDistanceInterface) {
            return ((SXRCameraClippingDistanceInterface)leftCamera).getNearClippingDistance();
        }
        return 0.0f;
    }

    /**
     * Sets the distance from the origin to the near clipping plane for the
     * whole camera rig.
     * 
     * @param near
     *            Distance to the near clipping plane.
     */
    public void setNearClippingDistance(float near) {
        if(leftCamera instanceof SXRCameraClippingDistanceInterface &&
           centerCamera instanceof SXRCameraClippingDistanceInterface &&
           rightCamera instanceof SXRCameraClippingDistanceInterface) {
            ((SXRCameraClippingDistanceInterface)leftCamera).setNearClippingDistance(near);
            centerCamera.setNearClippingDistance(near);
            ((SXRCameraClippingDistanceInterface)rightCamera).setNearClippingDistance(near);
        }
    }

    /**
     * @return Distance from the origin to the far clipping plane for the
     *         camera rig.
     */
    public float getFarClippingDistance() {
        if(leftCamera instanceof SXRCameraClippingDistanceInterface) {
            return ((SXRCameraClippingDistanceInterface)leftCamera).getFarClippingDistance();
        }
        return 0.0f;
    }

    /**
     * Sets the distance from the origin to the far clipping plane for the
     * whole camera rig.
     * 
     * @param far
     *            Distance to the far clipping plane.
     */
    public void setFarClippingDistance(float far) {
        if(leftCamera instanceof SXRCameraClippingDistanceInterface &&
           centerCamera instanceof SXRCameraClippingDistanceInterface &&
           rightCamera instanceof SXRCameraClippingDistanceInterface) {
            ((SXRCameraClippingDistanceInterface)leftCamera).setFarClippingDistance(far);
            centerCamera.setFarClippingDistance(far);
            ((SXRCameraClippingDistanceInterface)rightCamera).setFarClippingDistance(far);
        }
    }

    /**
     * Prints the {@link SXRCameraRig} object with indentation.
     *
     * @param sb
     *         The {@code StringBuffer} object to receive the output.
     *
     * @param indent
     *         Size of indentation in number of spaces.
     */
    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        sb.append("type: ");
        sb.append(getCameraRigType());
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        sb.append("lookAt: ");
        float[] lookAt = getLookAt();
        for (float vecElem : lookAt) {
            sb.append(vecElem);
            sb.append(" ");
        }
        sb.append(System.lineSeparator());

        sb.append(Log.getSpaces(indent + 2));
        sb.append("leftCamera: ");
        if (leftCamera == null) {
            sb.append("null");
            sb.append(System.lineSeparator());
        } else {
            sb.append(System.lineSeparator());
            leftCamera.prettyPrint(sb, indent + 4);
        }

        sb.append(Log.getSpaces(indent + 2));
        sb.append("rightCamera: ");
        if (rightCamera == null) {
            sb.append("null");
            sb.append(System.lineSeparator());
        } else {
            sb.append(System.lineSeparator());
            rightCamera.prettyPrint(sb, indent + 4);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }
}

class NativeCameraRig {
    static native long ctor();

    static native void updateRotation(long cameraRig);

    static native int getCameraRigType(long cameraRig);

    static native void setCameraRigType(long cameraRig, int cameraRigType);

    static native float getDefaultCameraSeparationDistance();

    static native void setDefaultCameraSeparationDistance(float distance);

    static native float getCameraSeparationDistance(long cameraRig);

    static native void setCameraSeparationDistance(long cameraRig,
            float distance);

    static native float getFloat(long cameraRig, String key);

    static native void setFloat(long cameraRig, String key, float value);

    static native float[] getVec2(long cameraRig, String key);

    static native void setVec2(long cameraRig, String key, float x, float y);

    static native float[] getVec3(long cameraRig, String key);

    static native void setVec3(long cameraRig, String key, float x, float y,
            float z);

    static native float[] getVec4(long cameraRig, String key);

    static native void setVec4(long cameraRig, String key, float x, float y,
            float z, float w);

    static native void attachLeftCamera(long cameraRig, long camera);

    static native void attachRightCamera(long cameraRig, long camera);

    static native void attachCenterCamera(long cameraRig, long camera);

    static native void reset(long cameraRig);

    static native void resetYaw(long cameraRig);

    static native void resetYawPitch(long cameraRig);

    static native void setRotationSensorData(long cameraRig, long timeStamp,
            float w, float x, float y, float z, float gyroX, float gyroY,
            float gyroZ);

    static native float[] getLookAt(long cameraRig);

    static native long getComponentType();
}
