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
// Created by c.bozzetto on 06/06/2017.
//

#include "physics_conetwistconstraint.h"
#include "physics_collidable.h"
#include "bullet/bullet_conetwistconstraint.h"
#include <glm/vec3.hpp>
#include <glm/mat3x3.hpp>
#include "glm/gtc/type_ptr.hpp"

namespace sxr {

    extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DConeTwistConstraint_ctor(JNIEnv *env, jclass obj, jlong bodyA,
                                                                jfloat pivotAx, jfloat pivotAy, jfloat pivotAz,
                                                                jfloat pivotBx, jfloat pivotBy, jfloat pivotBz,
                                                                jfloat axisX, jfloat axisY, jfloat axisZ)
    {
        glm::vec3 pivotA(pivotAx, pivotAy, pivotAz);
        glm::vec3 pivotB(pivotBx, pivotBy, pivotBz);
        glm::vec3 coneAxis(axisX, axisY, axisZ);

        return reinterpret_cast<jlong>(new
                BulletConeTwistConstraint(reinterpret_cast<PhysicsCollidable *>(bodyA),
                                          pivotA, pivotB, coneAxis));
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DConeTwistConstraint_setSwingLimit(JNIEnv *env, jclass obj,
                                                                         jlong jconstraint,
                                                                         jfloat limit)
    {
        reinterpret_cast<PhysicsConeTwistConstraint *>(jconstraint)->setSwingLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DConeTwistConstraint_getSwingLimit(JNIEnv *env, jclass obj,
                                                                         jlong jconstraint)
    {
        return reinterpret_cast<PhysicsConeTwistConstraint *>(jconstraint)->getSwingLimit();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DConeTwistConstraint_setTwistLimit(JNIEnv *env, jclass obj,
                                                                         jlong jconstraint,
                                                                         jfloat limit)
    {
        reinterpret_cast<PhysicsConeTwistConstraint *>(jconstraint)->setTwistLimit(limit);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DConeTwistConstraint_getTwistLimit(JNIEnv *env, jclass obj,
                                                                         jlong jconstraint)
    {
        return reinterpret_cast<PhysicsConeTwistConstraint *>(jconstraint)->getTwistLimit();
    }
    }

}
