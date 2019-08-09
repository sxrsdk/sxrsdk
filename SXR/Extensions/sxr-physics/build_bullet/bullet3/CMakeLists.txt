cmake_minimum_required(VERSION 2.4.3)

string(TOLOWER ${CMAKE_BUILD_TYPE} buildType)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/libs/${buildType}/${CMAKE_ANDROID_ARCH_ABI}")
SUBDIRS( ../../.. )
