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
// Created by c.bozzetto on 31/05/2017.
//

#include "physics_sliderconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_sliderconstraint.h"

namespace sxr {

    extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_ctor(JNIEnv *env, jclass obj, jlong bodyA,
                        jfloat pivotAx, jfloat pivotAy, jfloat pivotAz,
                        jfloat pivotBx, jfloat pivotBy, jfloat pivotBz)
    {
        PhysicsRigidBody* bA = reinterpret_cast<PhysicsRigidBody *>(bodyA);
        return reinterpret_cast<jlong>(new BulletSliderConstraint(bA,
                        glm::vec3(pivotAx, pivotAy, pivotAz), glm::vec3(pivotBx, pivotBy, pivotBz)));
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_setAngularLowerLimit(JNIEnv *env,
                                                                             jclass obj,
                                                                             jlong jsliderconstraint,
                                                                             jfloat limit)
    {
        reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)->setAngularLowerLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_getAngularLowerLimit(JNIEnv *env,
                                                                             jclass obj,
                                                                             jlong jsliderconstraint)
    {
        return reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)
                                ->getAngularLowerLimit();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_setAngularUpperLimit(JNIEnv *env,
                                                                             jclass obj,
                                                                             jlong jsliderconstraint,
                                                                             jfloat limit)
    {
        reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)->setAngularUpperLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_getAngularUpperLimit(JNIEnv *env,
                                                                             jclass obj,
                                                                             jlong jsliderconstraint)
    {
        return reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)
                                ->getAngularUpperLimit();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_setLinearLowerLimit(JNIEnv *env, jclass obj,
                                                                            jlong jsliderconstraint,
                                                                            jfloat limit)
    {
        reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)->setLinearLowerLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_getLinearLowerLimit(JNIEnv *env, jclass obj,
                                                                            jlong jsliderconstraint)
    {
        return reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)
                                ->getLinearLowerLimit();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_setLinearUpperLimit(JNIEnv *env, jclass obj,
                                                                            jlong jsliderconstraint,
                                                                            jfloat limit)
    {
        reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)->setLinearUpperLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DSliderConstraint_getLinearUpperLimit(JNIEnv *env, jclass obj,
                                                                            jlong jsliderconstraint)
    {
        return reinterpret_cast<PhysicsSliderConstraint *>(jsliderconstraint)
                                ->getLinearUpperLimit();
    }
}

}