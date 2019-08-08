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
// Created by c.bozzetto on 30/05/2017.
//

#include "physics_hingeconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_hingeconstraint.h"

namespace sxr {
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_ctor(JNIEnv *env, jclass obj,
                                                          jlong bodyA, jfloatArray pivotInA,
                                                          jfloatArray pivotInB, jfloatArray axis);

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getComponentType(JNIEnv *env, jobject obj);

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_setLimits(JNIEnv * env , jclass obj,
                                                               jlong jhinge_constraint,
                                                               jfloat lower , jfloat upper);

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getLowerLimit(JNIEnv * env, jclass obj,
                                                                   jlong jhinge_constraint);

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getUpperLimit(JNIEnv * env, jclass obj,
                                                                   jlong jhinge_constraint);
    }

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_ctor(JNIEnv * env, jclass obj,
                                                          jlong jbodyA, jfloatArray pivotInA,
                                                          jfloatArray pivotInB, jfloatArray axisIn)
    {
        float *pA = env->GetFloatArrayElements(pivotInA, 0);
        float *pB = env->GetFloatArrayElements(pivotInB, 0);
        float *axis = env->GetFloatArrayElements(axisIn, 0);
        glm::vec3 pivotA(pA[0], pA[1], pA[2]);
        glm::vec3 pivotB(pB[0], pB[1], pB[2]);
        glm::vec3 a(axis[0], axis[1], axis[2]);
        env->ReleaseFloatArrayElements(pivotInA, pA, 0);
        env->ReleaseFloatArrayElements(pivotInB, pB, 0);
        env->ReleaseFloatArrayElements(axisIn, axis, 0);

        PhysicsCollidable* bodyA = reinterpret_cast<PhysicsCollidable*>(jbodyA);
        return reinterpret_cast<jlong> (new BulletHingeConstraint(bodyA, pivotA, pivotB, a));
    }

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getComponentType(JNIEnv * env, jobject obj) {
        return PhysicsConstraint::getComponentType();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_setLimits(JNIEnv * env, jclass obj,
                                                               jlong jhinge_constraint,
                                                               jfloat lower, jfloat upper) {
        reinterpret_cast<PhysicsHingeConstraint*>(jhinge_constraint)->setLimits(lower, upper);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getLowerLimit(JNIEnv * env, jclass obj,
                                                                   jlong jhinge_constraint) {
        return reinterpret_cast<PhysicsHingeConstraint*>(jhinge_constraint)->getLowerLimit();
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_getUpperLimit(JNIEnv * env, jclass obj,
                                                                   jlong jhinge_constraint) {
        return reinterpret_cast<PhysicsHingeConstraint*>(jhinge_constraint)->getUpperLimit();
    }

}