cmake_minimum_required(VERSION 3.4.1)

project (sxr-daydream C CXX)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fexceptions")
add_library(sxr-daydream SHARED daydream_renderer.cpp daydream_renderer_jni.cpp)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../sxrsdk/src/main/jni)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../../sxrsdk/src/main/jni/contrib)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

string(TOLOWER ${CMAKE_BUILD_TYPE} buildType)

add_library(sxr SHARED IMPORTED)
set_property(TARGET sxr PROPERTY IMPORTED_LOCATION
             ${CMAKE_CURRENT_SOURCE_DIR}/../../../../sxrsdk/build/intermediates/ndkBuild/${buildType}/obj/local/${ANDROID_ABI}/libsxr.so)


add_library(gvr SHARED IMPORTED)
set_property(TARGET gvr PROPERTY IMPORTED_LOCATION
            ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libgvr.so)

target_link_libraries(sxr-daydream gvr sxr log GLESv2 EGL)
