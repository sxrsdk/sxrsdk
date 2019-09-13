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

#include "bullet/bullet_jointmotor.h"

namespace sxr {
extern "C"
{
JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_create(JNIEnv *env, jclass obj,
                                                          jfloat maxImpulse)
{
    return reinterpret_cast<jlong>(new BulletJointMotor(maxImpulse));
}


JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_setVelocityTarget(JNIEnv *env, jclass obj,
                                                                     jlong jjoint, jint dof,
                                                                     jfloat vel)
{
    PhysicsJointMotor *m = reinterpret_cast<PhysicsJointMotor *>(jjoint);
    return m->setVelocityTarget(dof, vel);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_setVelocityTarget3(JNIEnv *env, jclass obj,
                                                                      jlong jjoint,
                                                                      jfloat vx, jfloat vy,
                                                                      jfloat vz)
{
    PhysicsJointMotor *m = reinterpret_cast<PhysicsJointMotor *>(jjoint);
    return m->setVelocityTarget(vx, vy, vz);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_setPositionTarget(JNIEnv *env, jclass obj,
                                                                     jlong jjoint, jint dof,
                                                                     jfloat p)
{
    PhysicsJointMotor *m = reinterpret_cast<PhysicsJointMotor *>(jjoint);
    return m->setPositionTarget(dof, p);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_setPositionTarget3(JNIEnv *env, jclass obj,
                                                                      jlong jjoint,
                                                                      jfloat px, jfloat py,
                                                                      jfloat pz)
{
    PhysicsJointMotor *m = reinterpret_cast<PhysicsJointMotor *>(jjoint);
    return m->setPositionTarget(px, py, pz);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJointMotor_setPositionTarget4(JNIEnv *env, jclass obj,
                                                                      jlong jjoint,
                                                                      jfloat px, jfloat py,
                                                                      jfloat pz, jfloat pw)
{
    PhysicsJointMotor *m = reinterpret_cast<PhysicsJointMotor *>(jjoint);
    return m->setPositionTarget(px, py, pz, pw);
}
}

}