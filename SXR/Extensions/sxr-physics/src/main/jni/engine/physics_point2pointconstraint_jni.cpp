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

#include "physics_point2pointconstraint.h"
#include "physics_rigidbody.h"
#include "bullet/bullet_point2pointconstraint.h"
#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include <glm/vec3.hpp>

namespace sxr {
    extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_Native3DPoint2PointConstraint_ctor(JNIEnv *env, jclass obj,
                                                                  jlong bodyA,
                                                                  jfloat pivotAx, jfloat pivotAy, jfloat pivotAz,
                                                                  jfloat pivotBx, jfloat pivotBy, jfloat pivotBz)
    {
        glm::vec3 pA(pivotAx, pivotAy, pivotAz);
        glm::vec3 pB(pivotBx, pivotBy, pivotBz);
        BulletPoint2PointConstraint* c = new BulletPoint2PointConstraint(reinterpret_cast<PhysicsRigidBody*>(bodyA), pA, pB);
        return reinterpret_cast<jlong>(c);
    }


    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_Native3DPoint2PointConstraint_setBreakingImpulse(JNIEnv *env,
                                                                                jclass obj,
                                                                                jlong jp2p_constraint,
                                                                                jfloat impulse)
    {
        reinterpret_cast<PhysicsConstraint*>(jp2p_constraint)->setBreakingImpulse(impulse);
    }

    JNIEXPORT jfloat JNICALL
    Java_com_samsungxr_physics_Native3DPoint2PointConstraint_getBreakingLimit(JNIEnv *env,
                                                                              jclass obj,
                                                                              jlong jp2p_constraint)
    {
        return reinterpret_cast<PhysicsConstraint *>(jp2p_constraint)->getBreakingImpulse();
    }
}

}
