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
// Created by c.bozzetto on 19/06/2017.
//

#include <contrib/glm/ext.hpp>
#include "physics_constraint.h"

namespace sxr {

    extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_getComponentType(JNIEnv *env, jclass obj)
    {
        return PhysicsConstraint::getComponentType();
    }

    JNIEXPORT jint JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_getConstraintType(JNIEnv *env, jclass obj,
                                                                    jlong jconstraint)
    {
        return reinterpret_cast<PhysicsConstraint *>(jconstraint)->getConstraintType();
    }


    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_getPivotA(JNIEnv *env, jclass obj,jlong jp2p_constraint)
    {
        glm::vec3 v = reinterpret_cast<PhysicsConstraint *>(jp2p_constraint)->getPivotA();
        jfloatArray result = env->NewFloatArray(3);
        env->SetFloatArrayRegion(result, 0, 3, glm::value_ptr(v));

        return result;
    }


    JNIEXPORT jfloatArray JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_getPivotB(JNIEnv *env, jclass obj,
                                                                         jlong jp2p_constraint)
    {
        glm::vec3 v = reinterpret_cast<PhysicsConstraint *>(jp2p_constraint)->getPivotB();
        jfloatArray result = env->NewFloatArray(3);
        env->SetFloatArrayRegion(result, 0, 3, glm::value_ptr(v));

        return result;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_setBreakingImpulse(JNIEnv *env, jclass obj,
                                                                     jlong jconstraint,
                                                                     jfloat impulse)
    {
        reinterpret_cast<PhysicsConstraint *>(jconstraint)->setBreakingImpulse(impulse);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_getBreakingImpulse(JNIEnv *env, jclass obj,
                                                                     jlong jconstraint)
    {
        return reinterpret_cast<PhysicsConstraint *>(jconstraint)->getBreakingImpulse();
    }
    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_addChildComponent(JNIEnv *env, jclass obj,
                                                                    jlong jconstraint,
                                                                    jlong jchild)
    {
        PhysicsConstraint *parent = reinterpret_cast<PhysicsConstraint *>(jconstraint);
        PhysicsConstraint *child = reinterpret_cast<PhysicsConstraint *>(jchild);
        parent->addChildComponent(child);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DConstraint_removeChildComponent(JNIEnv *env, jclass obj,
                                                                       jlong jconstraint,
                                                                       jlong jchild)
    {
        PhysicsConstraint *parent = reinterpret_cast<PhysicsConstraint *>(jconstraint);
        PhysicsConstraint *child = reinterpret_cast<PhysicsConstraint *>(jchild);
        parent->removeChildComponent(child);
    }
    }

}