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
#include "physics_hingeconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_hingeconstraint.h"

namespace sxr {
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DHingeConstraint_ctor(JNIEnv *env, jclass obj, jlong bodyA,
                                                            jfloat pivotAX, jfloat pivotAY, jfloat pivotAZ,
                                                            jfloat pivotBX, jfloat pivotBY, jfloat pivotBZ,
                                                            jfloat axisX, jfloat axisY, jfloat axisZ);

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
    Java_com_samsungxr_physics_Native3DHingeConstraint_ctor(JNIEnv * env, jclass obj, jlong jbodyA,
                                                            jfloat pivotAX, jfloat pivotAY, jfloat pivotAZ,
                                                            jfloat pivotBX, jfloat pivotBY, jfloat pivotBZ,
                                                            jfloat axisX, jfloat axisY, jfloat axisZ)
{
        glm::vec3 pivotA(pivotAX, pivotAY, pivotAZ);
        glm::vec3 pivotB(pivotBX, pivotBY, pivotBZ);
        glm::vec3 axis(axisX, axisY, axisZ);
        PhysicsCollidable* bodyA = reinterpret_cast<PhysicsCollidable*>(jbodyA);
        return reinterpret_cast<jlong> (new BulletHingeConstraint(bodyA, pivotA, pivotB, axis));
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