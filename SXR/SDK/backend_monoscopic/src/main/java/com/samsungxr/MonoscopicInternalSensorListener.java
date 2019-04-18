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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.joml.Quaternionf;

/** A listener for a TYPE_ROTATION_VECTOR type sensor. */
class MonoscopicInternalSensorListener implements SensorEventListener {
    private static final float SQRT_OF_HALF = (float)Math.sqrt(0.5);
    private static final Quaternionf OFFSET_QUATERNION = new Quaternionf(0.0f, SQRT_OF_HALF, 0.0f, SQRT_OF_HALF);
    private static final Quaternionf PORTRAIT_TRANSFORM1 = new Quaternionf(0, 0, -SQRT_OF_HALF, SQRT_OF_HALF);
    private static final Quaternionf PORTRAIT_TRANSFORM2 = new Quaternionf(-SQRT_OF_HALF, 0, 0, SQRT_OF_HALF);

    private final Quaternionf COORDINATE_QUATERNION;
    private final Quaternionf CONSTANT_EXPRESSION;

    private MonoscopicRotationSensor mSensor;
    private final Quaternionf mQuaternion = new Quaternionf();
    private final boolean mRequestedLandscape;

    public MonoscopicInternalSensorListener(MonoscopicRotationSensor sensor, final boolean requestedLandscape) {
        mSensor = sensor;
        mRequestedLandscape = requestedLandscape;

        if (mRequestedLandscape) {
            COORDINATE_QUATERNION = new Quaternionf(0.0f, 0.0f, -SQRT_OF_HALF, SQRT_OF_HALF);
        } else {
            COORDINATE_QUATERNION = new Quaternionf(0.0f, 0.0f, SQRT_OF_HALF, SQRT_OF_HALF);
        }

        CONSTANT_EXPRESSION = new Quaternionf().set(COORDINATE_QUATERNION).invert().mul(OFFSET_QUATERNION);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float w = event.values[3];
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        mQuaternion.set(x, y, z, w);

        if (mRequestedLandscape) {
            CONSTANT_EXPRESSION.mul(mQuaternion, mQuaternion);
            mQuaternion.mul(COORDINATE_QUATERNION);
        } else {
            mQuaternion.premul(PORTRAIT_TRANSFORM1);
            mQuaternion.premul(PORTRAIT_TRANSFORM2);
        }

        mSensor.onInternalRotationSensor(SXRTime.getCurrentTime(), mQuaternion.w, mQuaternion.x, mQuaternion.y,
                mQuaternion.z, 0.0f, 0.0f, 0.0f);
    }
}
