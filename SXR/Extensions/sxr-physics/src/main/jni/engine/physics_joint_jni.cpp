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
#include <string>
#include "bullet/bullet_joint.h"

namespace sxr {
extern "C"
{
JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_ctorRoot(JNIEnv *env, jclass obj, jfloat mass, jint numBones)
{
    return reinterpret_cast<jlong>(new BulletRootJoint(mass, numBones));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_ctorLink(JNIEnv *env, jclass obj, jobject jparent,
                                                       jint jointType, jint jointIndex, jfloat mass)
{
    BulletJoint *parent = reinterpret_cast<BulletJoint *>(jparent);
    return reinterpret_cast<jlong>(new BulletJoint(parent, PhysicsJoint::JointType(jointType),
                                                   jointIndex, mass));
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_getComponentType(JNIEnv *env, jclass obj)
{
    return PhysicsJoint::getComponentType();
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_getMass(JNIEnv *env, jclass obj, jlong jjoint)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    return mb->getMass();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_setMass(JNIEnv *env, jclass obj, jlong jjoint,
                                                      jfloat mass)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    mb->setMass(mass);
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_getFriction(JNIEnv *env, jclass obj, jlong jjoint)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    return mb->getFriction();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_setFriction(JNIEnv *env, jclass obj, jlong jjoint,
                                                          jfloat friction)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    mb->setFriction(friction);
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_getJointIndex(JNIEnv *env, jclass obj, jlong jjoint)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    return mb->getJointIndex();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_applyCentralForce(JNIEnv *env, jclass obj, jlong jjoint,
                                                                jfloat x, jfloat y, jfloat z)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    mb->applyCentralForce(x, y, z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_applyTorque(JNIEnv *env, jclass obj,
                                                          jlong jjoint, jfloat x, jfloat y,
                                                          jfloat z)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    mb->applyTorque(x, y, z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_setAxis(JNIEnv *env, jclass obj, jlong jjoint,
                                                      jfloat x, jfloat y, jfloat z)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    glm::vec3 axis(x, y, z);
    mb->setAxis(axis);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_getSkeleton(JNIEnv *env, jclass obj, jlong jjoint)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    jlong nativePtr = reinterpret_cast<jlong>(mb->getSkeleton());
    return nativePtr;
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativePhysicsJoint_setPivot(JNIEnv *env, jclass obj, jlong jjoint,
                                                       jfloat x, jfloat y, jfloat z)
{
    PhysicsJoint *mb = reinterpret_cast<PhysicsJoint *>(jjoint);
    glm::vec3 pivot(x, y, z);
    mb->setPivot(pivot);
}

}
}