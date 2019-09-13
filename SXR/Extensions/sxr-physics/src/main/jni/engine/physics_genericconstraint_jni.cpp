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

#include "physics_genericconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_generic6dofconstraint.h"
#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include <glm/vec3.hpp>

namespace sxr {

    extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_ctor(JNIEnv *env, jclass obj, jlong jbodyA,
                                                              jfloat pivotAx, jfloat pivotAy, jfloat pivotAz,
                                                              jfloat pivotBx, jfloat pivotBy, jfloat pivotBz)
    {
        PhysicsRigidBody *bodyA = reinterpret_cast<PhysicsRigidBody *>(jbodyA);
        glm::vec3 pivotA(pivotAx, pivotAy, pivotAz);
        glm::vec3 pivotB(pivotBx, pivotBy, pivotBz);
        return reinterpret_cast<jlong>(new BulletGeneric6dofConstraint(bodyA, pivotA, pivotB));
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_setLinearLowerLimits(JNIEnv *env,
                                                                              jclass obj,
                                                                              jlong jconstr,
                                                                              jfloat limitX,
                                                                              jfloat limitY,
                                                                              jfloat limitZ)
    {
        reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->setLinearLowerLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_getLinearLowerLimits(JNIEnv *env,
                                                                              jclass obj,
                                                                              jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->getLinearLowerLimits();
        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_setLinearUpperLimits(JNIEnv *env,
                                                                              jclass obj,
                                                                              jlong jconstr,
                                                                              jfloat limitX,
                                                                              jfloat limitY,
                                                                              jfloat limitZ)
    {
        reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->setLinearUpperLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_getLinearUpperLimits(JNIEnv *env,
                                                                              jclass obj,
                                                                              jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->getLinearUpperLimits();

        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_setAngularLowerLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr,
                                                                               jfloat limitX,
                                                                               jfloat limitY,
                                                                               jfloat limitZ)
    {
        reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->setAngularLowerLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_getAngularLowerLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->getAngularLowerLimits();
        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_setAngularUpperLimits(JNIEnv *env,
                                                                               jclass obj,
                                                                               jlong jconstr,
                                                                               jfloat limitX,
                                                                               jfloat limitY,
                                                                               jfloat limitZ)
    {
        reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->setAngularUpperLimits(limitX, limitY, limitZ);
    }

    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DGenericConstraint_getAngularUpperLimits(
            JNIEnv *env, jclass obj, jlong jconstr)
    {
        jfloatArray temp = env->NewFloatArray(3);
        const glm::vec3 &l = reinterpret_cast<PhysicsGenericConstraint *>(jconstr)
                ->getAngularUpperLimits();

        env->SetFloatArrayRegion(temp, 0, 3, glm::value_ptr(l));
        return temp;
    }
    }

}