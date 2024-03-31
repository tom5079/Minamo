cmake_minimum_required(VERSION 3.26)

project(minamo C)

include(CMakeFindDependencyMacro)

add_library(minamo SHARED imagesource/file.c imagesource/descriptor.c minamo_image.c arch.c)

find_library(VIPS_LIB NAMES vips PATHS ${CUSTOM_LIB_PATH} PATH_SUFFIXES lib REQUIRED NO_DEFAULT_PATH)
find_library(GLIB_LIB NAMES glib-2.0 PATHS ${CUSTOM_LIB_PATH} PATH_SUFFIXES lib REQUIRED NO_DEFAULT_PATH)
find_library(GOBJECT_LIB NAMES gobject-2.0 PATHS ${CUSTOM_LIB_PATH} PATH_SUFFIXES lib REQUIRED NO_DEFAULT_PATH)
find_package(JNI REQUIRED)

target_link_libraries(minamo JNI::JNI ${VIPS_LIB} ${GLIB_LIB} ${GOBJECT_LIB})

target_include_directories(minamo PRIVATE
    ${CUSTOM_LIB_PATH}/include
    ${CUSTOM_LIB_PATH}/include/glib-2.0
    ${CUSTOM_LIB_PATH}/lib/glib-2.0/include
    ${JNI_INCLUDE_DIRS})

install(TARGETS minamo LIBRARY)
install(TARGETS minamo PUBLIC_HEADER)