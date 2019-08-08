cmake_minimum_required(VERSION 3.4.1)

project (sxr-physics C CXX)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fexceptions")
string(TOLOWER ${CMAKE_BUILD_TYPE} buildType)
set(bulletDir ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(strip "${ANDROID_TOOLCHAIN_PREFIX}strip -g -S -d --strip-debug --verbose ${bulletDir}/")
add_library(sxr-physics SHARED
    engine/bullet/bullet_sxr_utils.cpp
    engine/bullet/bullet_rigidbody.cpp
    engine/bullet/bullet_world.cpp
    engine/bullet/bullet_fileloader.cpp
    engine/bullet/bullet_fixedconstraint.cpp
    engine/bullet/bullet_point2pointconstraint.cpp
    engine/bullet/bullet_hingeconstraint.cpp
    engine/bullet/bullet_sliderconstraint.cpp
    engine/bullet/bullet_conetwistconstraint.cpp
    engine/bullet/bullet_generic6dofconstraint.cpp
    engine/physics_world_jni.cpp
    engine/physics_loader_jni.cpp
    engine/physics_rigidbody_jni.cpp
    engine/physics_constraint_jni.cpp
    engine/physics_fixedconstraint_jni.cpp
    engine/physics_point2pointconstraint_jni.cpp
    engine/physics_hingeconstraint_jni.cpp
    engine/physics_sliderconstraint_jni.cpp
    engine/physics_conetwistconstraint_jni.cpp
    engine/physics_genericconstraint_jni.cpp)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/bullet3/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../../SDK/sxrsdk/src/main/jni)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../../SDK/sxrsdk/src/main/jni/contrib)

add_library(sxrsdk SHARED IMPORTED)
set_property(TARGET sxrsdk PROPERTY IMPORTED_LOCATION
             ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../SDK/sxrsdk/build/intermediates/ndkBuild/${buildType}/obj/local/${ANDROID_ABI}/libsxrsdk.so)

add_library(LinearMath SHARED IMPORTED)
set_target_properties(LinearMath PROPERTIES IMPORTED_LOCATION ${bulletDir}/libLinearMath.so)

add_library(Bullet3Common SHARED IMPORTED)
set_target_properties(Bullet3Common PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBullet3Common.so)

add_library(BulletCollision SHARED IMPORTED)
set_target_properties(BulletCollision PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBulletCollision.so)

add_library(Bullet3Collision SHARED IMPORTED)
set_target_properties(Bullet3Collision PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBullet3Collision.so)

add_library(BulletDynamics SHARED IMPORTED)
set_target_properties(BulletDynamics PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBulletDynamics.so)

add_library(Bullet3Dynamics SHARED IMPORTED)
set_target_properties(Bullet3Dynamics PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBullet3Dynamics.so)

add_library(Bullet3Geometry SHARED IMPORTED)
set_target_properties(Bullet3Geometry PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBullet3Geometry.so)

add_library(BulletSoftBody SHARED IMPORTED)
set_target_properties(BulletSoftBody PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBulletSoftBody.so)

add_library(BulletFileLoader SHARED IMPORTED)
set_target_properties(BulletFileLoader PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBulletFileLoader.so)

add_library(Bullet2FileLoader SHARED IMPORTED)
set_target_properties(Bullet2FileLoader PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBullet2FileLoader.so)

add_library(BulletWorldImporter SHARED IMPORTED)
set_target_properties(BulletWorldImporter PROPERTIES IMPORTED_LOCATION ${bulletDir}/libBulletWorldImporter.so)

target_link_libraries(sxr-physics sxrsdk BulletWorldImporter Bullet2FileLoader BulletFileLoader Bullet3Dynamics Bullet3Collision Bullet3Geometry Bullet3Common BulletDynamics BulletCollision LinearMath android log  EGL GLESv3)
