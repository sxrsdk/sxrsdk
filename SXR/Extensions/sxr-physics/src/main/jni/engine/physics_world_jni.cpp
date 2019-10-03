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

/***************************************************************************
 * Represents a physics 3D world
 ***************************************************************************/
#include <jni.h>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include "bullet/bullet_world.h"
#include "physics_world.h"
#include "physics_joint.h"
#include "glm/glm.hpp"
#include "glm/gtc/type_ptr.hpp"
#include <glm/vec3.hpp>

static char tag[] = "PHYSICS";

namespace sxr
{
    extern "C" {

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_ctor(JNIEnv *env, jclass obj,
                                                         jboolean isMultiBody)
    {
        return reinterpret_cast<jlong>(new BulletWorld(isMultiBody));
    }

    JNIEXPORT jlong JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_getComponentType(JNIEnv *env, jclass obj)
    {
        return PhysicsWorld::getComponentType();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_addConstraint(JNIEnv *env, jclass obj,
                                                                  jlong jworld,
                                                                  jlong jconstraint)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsConstraint *constraint = reinterpret_cast<PhysicsConstraint *>(jconstraint);

        world->addConstraint(constraint);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_removeConstraint(JNIEnv *env, jclass obj,
                                                                     jlong jworld,
                                                                     jlong jconstraint)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsConstraint *constraint = reinterpret_cast<PhysicsConstraint *>(jconstraint);

        world->removeConstraint(constraint);
    }


    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_addJoint(JNIEnv *env, jclass obj, jlong jworld,
                                                             jlong jmulti_body)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsJoint *body = reinterpret_cast<PhysicsJoint *>(jmulti_body);
        world->addJoint(body);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_addJointWithMask(JNIEnv *env, jclass obj,
                                                             jlong jworld,
                                                             jlong jmulti_body,
                                                             jlong collisionType,
                                                             jlong collidesWith)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsJoint *body = reinterpret_cast<PhysicsJoint *>(jmulti_body);
        world->addJointWithMask(body, collisionType, collidesWith);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_removeJoint(JNIEnv *env, jclass obj,
                                                                jlong jworld,
                                                                jlong jmulti_body)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsJoint *body = reinterpret_cast<PhysicsJoint *>(jmulti_body);
        world->removeJoint(body);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_startDrag(JNIEnv *env, jclass obj, jlong jworld,
                                                              jlong jpivot_obj, jlong jtarget,
                                                              jfloat relx, jfloat rely, jfloat relz)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        Node *pivot_obj = reinterpret_cast<Node *>(jpivot_obj);
        PhysicsRigidBody *target = reinterpret_cast<PhysicsRigidBody *>(jtarget);

        world->startDrag(pivot_obj, target, relx, rely, relz);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_stopDrag(JNIEnv *env, jclass obj, jlong jworld)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);

        world->stopDrag();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_addRigidBody(JNIEnv *env, jclass obj,
                                                                 jlong jworld,
                                                                 jlong jrigid_body)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

        world->addRigidBody(rigid_body);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_addRigidBodyWithMask(JNIEnv *env, jclass obj,
                                                                         jlong jworld,
                                                                         jlong jrigid_body,
                                                                         jlong collisionType,
                                                                         jlong collidesWith)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

        world->addRigidBody(rigid_body, collisionType, collidesWith);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_removeRigidBody(JNIEnv *env, jclass obj,
                                                                    jlong jworld, jlong jrigid_body)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);
        PhysicsRigidBody *rigid_body = reinterpret_cast<PhysicsRigidBody *>(jrigid_body);

        world->removeRigidBody(rigid_body);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_step(JNIEnv *env, jclass obj, jlong jworld,
                                                         jfloat jtime_step, int maxSubSteps)
    {
        PhysicsWorld *world = reinterpret_cast<PhysicsWorld *>(jworld);

        world->step((float) jtime_step, maxSubSteps);
    }

    JNIEXPORT jobjectArray JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_listCollisions(JNIEnv *env, jclass obj,
                                                                   jlong jworld)
    {
        jclass collisionInfoClass = env->FindClass("com/samsungxr/physics/SXRCollisionInfo");
        jmethodID
                collisionInfoConstructor = env
                ->GetMethodID(collisionInfoClass, "<init>", "(JJ[FFZ)V");

        PhysicsWorld *world = reinterpret_cast <PhysicsWorld *> (jworld);
        std::list<ContactPoint> contactPoints;

        world->listCollisions(contactPoints);

        int size = contactPoints.size();
        jobjectArray jNewList = env->NewObjectArray(size, collisionInfoClass, NULL);

        int i = 0;
        for (auto it = contactPoints.begin(); it != contactPoints.end(); ++it)
        {
            const ContactPoint &data = *it;

            jfloatArray normal = env->NewFloatArray(3);
            env->SetFloatArrayRegion(normal, 0, 3, data.normal);

            jobject contactObject =
                    env->NewObject(collisionInfoClass, collisionInfoConstructor, (jlong) data.body0,
                                   (jlong) data.body1, (jfloatArray) normal, (jfloat) data.distance,
                                   (jboolean) data.isHit
                    );

            env->SetObjectArrayElement(jNewList, i++, contactObject);
            env->DeleteLocalRef(contactObject);
            env->DeleteLocalRef(normal);
        }

        env->DeleteLocalRef(collisionInfoClass);
        return jNewList;
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_setGravity(JNIEnv *env, jclass obj,
                                                               jlong jworld,
                                                               jfloat gx, jfloat gy, jfloat gz)
    {
        PhysicsWorld *world = reinterpret_cast <PhysicsWorld *> (jworld);
        world->setGravity(gx, gy, gz);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_getGravity(JNIEnv *env, jclass obj,
                                                               jlong jworld,
                                                               jfloatArray jgravity)
    {
        PhysicsWorld *world = reinterpret_cast <PhysicsWorld *> (jworld);
        const glm::vec3 &gravity = world->getGravity();
        env->SetFloatArrayRegion(jgravity, 0, 3, glm::value_ptr(gravity));
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_setupDebugDraw(JNIEnv *env, jclass obj,
                                                                   jlong jworld, jlong jnode)
    {
        PhysicsWorld* world = reinterpret_cast <PhysicsWorld *> (jworld);
        Node* node = reinterpret_cast <Node*> (jnode);
        world->setupDebugDraw(node);
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_debugDrawWorld(JNIEnv *env, jclass obj,
                                                                   jlong jworld)
    {
        PhysicsWorld *world = reinterpret_cast <PhysicsWorld *> (jworld);
        world->debugDrawWorld();
    }

    JNIEXPORT void JNICALL
    Java_com_samsungxr_physics_NativePhysics3DWorld_setDebugMode(JNIEnv *env, jclass obj,
                                                                 jlong jworld, jint mode)
    {
        PhysicsWorld *world = reinterpret_cast <PhysicsWorld *> (jworld);
        world->setDebugMode(mode);
    }
    } // extern C
} // namespace SXR

