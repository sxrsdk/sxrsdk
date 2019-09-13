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

//
// Created by c.bozzetto on 09/06/2017.
//

#include "physics_universalconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_universalconstraint.h"
#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include <glm/vec3.hpp>

namespace sxr {

    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_ctor(
            JNIEnv *env, jclass obj, jlong bodyA,
            jfloat anchorX, jfloat anchorY, jfloat anchorZ,
            jfloat axis1X, jfloat axis1Y, jfloat axis1Z,
            jfloat axis2X, jfloat axis2Y, jfloat axis2Z);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setLinearLowerLimits(
            JNIEnv *env, jclass obj, jlong jconstr, jfloat limitX, jfloat limitY, jfloat limitZ);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getLinearLowerLimits(
            JNIEnv *env, jclass obj, jlong jconstr);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setLinearUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr, jfloat limitX, jfloat limitY, jfloat limitZ);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getLinearUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setAngularLowerLimits(
            JNIEnv *env, jclass obj, jlong jconstr, jfloat limitX, jfloat limitY, jfloat limitZ);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getAngularLowerLimits(
            JNIEnv *env, jclass obj, jlong jconstr);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setAngularUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr, jfloat limitX, jfloat limitY, jfloat limitZ);

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getAngularUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_ctor(JNIEnv *env, jclass obj, jlong jbodyA,
                                                              jfloat pivotX, jfloat pivotY,
                                                              jfloat pivotZ,
                                                              jfloat axis1X, jfloat axis1Y,
                                                              jfloat axis1Z,
                                                              jfloat axis2X, jfloat axis2Y,
                                                              jfloat axis2Z)
    {
        PhysicsCollidable *bodyA = reinterpret_cast<PhysicsCollidable *>(jbodyA);
        glm::vec3 pivot(pivotX, pivotY, pivotZ);
        glm::vec3 axis1(axis1X, axis1Y, axis1Z);
        glm::vec3 axis2(axis2X, axis2Y, axis2Z);
        return reinterpret_cast<jlong>(new BulletUniversalConstraint(bodyA, pivot, axis1, axis2));
    }


    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setAngularLowerLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr,
                                                                               jfloat limitX,
                                                                               jfloat limitY,
                                                                               jfloat limitZ)
    {
        reinterpret_cast<PhysicsUniversalConstraint *>(jconstr)
                ->setAngularLowerLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getAngularLowerLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsUniversalConstraint *>(jconstr)
                ->getAngularLowerLimits();
        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_setAngularUpperLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr,
                                                                               jfloat limitX,
                                                                               jfloat limitY,
                                                                               jfloat limitZ)
    {
        reinterpret_cast<PhysicsUniversalConstraint *>(jconstr)
                ->setAngularUpperLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_NativeUniversalConstraint_getAngularUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsUniversalConstraint *>(jconstr)
                ->getAngularUpperLimits();

        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }
    }

}