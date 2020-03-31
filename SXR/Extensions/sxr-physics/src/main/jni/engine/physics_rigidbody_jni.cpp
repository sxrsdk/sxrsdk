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

#include "bullet/bullet_rigidbody.h"

namespace sxr {
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativeRigidBody_ctor(JNIEnv *env, jclass obj, jfloat mass)
{
    BulletRigidBody *rb = new BulletRigidBody();
    rb->setMass(mass);
    return reinterpret_cast<jlong>(rb);
}

JNIEXPORT jlong JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getComponentType(JNIEnv *env, jclass obj)
{
    return PhysicsRigidBody::getComponentType();
}

JNIEXPORT jint JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getSimulationType(JNIEnv *env, jclass obj,
                                                             jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);
    return rigid_body->getSimulationType();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setSimulationType(JNIEnv *env, jclass obj,
                                                             jlong jrigid_body, jint jtype)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);
    PhysicsRigidBody::SimulationType type = (PhysicsRigidBody::SimulationType) (int) (jtype);
    rigid_body->setSimulationType(type);
}

JNIEXPORT jfloat JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getMass(JNIEnv *env, jclass obj,
                                                   jlong jmultibody)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jmultibody);

    return rigid_body->getMass();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyCentralForce(JNIEnv *env, jclass obj,
                                                             jlong jrigid_body, jfloat x, jfloat y,
                                                             jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyCentralForce(x, y, z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyForce(JNIEnv *env, jclass obj,
                                                      jlong jrigid_body, jfloat force_x,
                                                      jfloat force_y, jfloat force_z,
                                                      jfloat rel_pos_x, jfloat rel_pos_y,
                                                      jfloat rel_pos_z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyForce(force_x, force_y, force_z, rel_pos_x, rel_pos_y, rel_pos_z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyCentralImpulse(JNIEnv *env, jclass obj,
                                                               jlong jrigid_body, jfloat x,
                                                               jfloat y, jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyCentralImpulse(x, y, z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyImpulse(JNIEnv *env, jclass obj,
                                                        jlong jrigid_body, jfloat impulse_x,
                                                        jfloat impulse_y, jfloat impulse_z,
                                                        jfloat rel_pos_x, jfloat rel_pos_y,
                                                        jfloat rel_pos_z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyImpulse(impulse_x, impulse_y, impulse_z, rel_pos_x, rel_pos_y, rel_pos_z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyTorque(JNIEnv *env, jclass obj,
                                                       jlong jrigid_body, jfloat x, jfloat y,
                                                       jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyTorque(x, y, z);
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_applyTorqueImpulse(JNIEnv *env, jclass obj,
                                                              jlong jrigid_body, jfloat x, jfloat y,
                                                              jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->applyTorqueImpulse(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setGravity(JNIEnv *env, jclass obj,
                                                      jlong jrigid_body, jfloat x, jfloat y,
                                                      jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setGravity(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setDamping(JNIEnv *env, jclass obj,
                                                      jlong jrigid_body, jfloat linear,
                                                      jfloat angular)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setDamping(linear, angular);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setLinearVelocity(JNIEnv *env, jclass obj,
                                                             jlong jrigid_body, jfloat x, jfloat y,
                                                             jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setLinearVelocity(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setAngularVelocity(JNIEnv *env, jclass obj,
                                                              jlong jrigid_body, jfloat x, jfloat y,
                                                              jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setAngularVelocity(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setAngularFactor(JNIEnv *env, jclass obj,
                                                            jlong jrigid_body, jfloat x, jfloat y,
                                                            jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setAngularFactor(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setLinearFactor(JNIEnv *env, jclass obj,
                                                           jlong jrigid_body, jfloat x, jfloat y,
                                                           jfloat z)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setLinearFactor(x, y, z);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setFriction(JNIEnv *env, jclass obj,
                                                       jlong jrigid_body, jfloat n)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setFriction(n);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setRestitution(JNIEnv *env, jclass obj,
                                                          jlong jrigid_body, jfloat n)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setRestitution(n);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setSleepingThresholds(JNIEnv *env, jclass obj,
                                                                 jlong jrigid_body, jfloat linear,
                                                                 jfloat angular)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setSleepingThresholds(linear, angular);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setCcdMotionThreshold(JNIEnv *env, jclass obj,
                                                                 jlong jrigid_body, jfloat n)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setCcdMotionThreshold(n);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setCcdSweptSphereRadius(JNIEnv *env, jclass obj,
                                                                   jlong jrigid_body, jfloat n)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setCcdSweptSphereRadius(n);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setContactProcessingThreshold(JNIEnv *env, jclass obj,
                                                                         jlong jrigid_body,
                                                                         jfloat n)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setContactProcessingThreshold(n);
}

JNIEXPORT void   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_setIgnoreCollisionCheck(JNIEnv *env, jclass obj,
                                                                   jlong jrigid_body,
                                                                   jobject collisionObj,
                                                                   jboolean ignore)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    rigid_body->setIgnoreCollisionCheck(reinterpret_cast<PhysicsRigidBody *>(collisionObj), ignore);
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getGravity(JNIEnv *env, jclass obj,
                                                      jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[3];

    rigid_body->getGravity(temp);

    jfloatArray result = env->NewFloatArray(3);

    env->SetFloatArrayRegion(result, 0, 3, temp);

    return result;
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getLinearVelocity(JNIEnv *env, jclass obj,
                                                             jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[3];

    rigid_body->getLinearVelocity(temp);

    jfloatArray result = env->NewFloatArray(3);

    env->SetFloatArrayRegion(result, 0, 3, temp);

    return result;
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getAngularVelocity(JNIEnv *env, jclass obj,
                                                              jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[3];

    rigid_body->getAngularVelocity(temp);

    jfloatArray result = env->NewFloatArray(3);

    env->SetFloatArrayRegion(result, 0, 3, temp);

    return result;
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getAngularFactor(JNIEnv *env, jclass obj,
                                                            jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[3];

    rigid_body->getAngularFactor(temp);

    jfloatArray result = env->NewFloatArray(3);

    env->SetFloatArrayRegion(result, 0, 3, temp);

    return result;
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getLinearFactor(JNIEnv *env, jclass obj,
                                                           jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[3];

    rigid_body->getLinearFactor(temp);

    jfloatArray result = env->NewFloatArray(3);

    env->SetFloatArrayRegion(result, 0, 3, temp);

    return result;
}

JNIEXPORT jfloatArray   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getDamping(JNIEnv *env, jclass obj,
                                                      jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    jfloat temp[2];

    rigid_body->getDamping(temp[0], temp[1]);

    jfloatArray result = env->NewFloatArray(2);

    env->SetFloatArrayRegion(result, 0, 2, temp);

    return result;
}

JNIEXPORT jfloat   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getFriction(JNIEnv *env, jclass obj,
                                                       jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    return rigid_body->getFriction();
}

JNIEXPORT jfloat   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getRestitution(JNIEnv *env, jclass obj,
                                                          jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    return rigid_body->getRestitution();
}

JNIEXPORT jfloat   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getCcdMotionThreshold(JNIEnv *env, jclass obj,
                                                                 jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    return rigid_body->getCcdMotionThreshold();
}

JNIEXPORT jfloat   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getCcdSweptSphereRadius(JNIEnv *env, jclass obj,
                                                                   jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    return rigid_body->getCcdSweptSphereRadius();
}

JNIEXPORT jfloat   JNICALL
Java_com_samsungxr_physics_NativeRigidBody_getContactProcessingThreshold(JNIEnv *env, jclass obj,
                                                                         jlong jrigid_body)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

    return rigid_body->getContactProcessingThreshold();
}

JNIEXPORT void JNICALL
Java_com_samsungxr_physics_NativeRigidBody_reset(JNIEnv *env, jclass obj,
                                                 jlong jrigid_body, jboolean rebuildCollider)
{
    PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);
    rigid_body->reset(rebuildCollider);
}
}

}
