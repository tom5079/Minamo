cmake_minimum_required(VERSION 3.26)

project(minamo C)

find_package(PkgConfig REQUIRED)

add_library(minamo SHARED imagesource/file.c imagesource/descriptor.c minamo_image.c arch.c)

pkg_check_modules(VIPS REQUIRED IMPORTED_TARGET vips)
pkg_check_modules(GLIB REQUIRED IMPORTED_TARGET glib-2.0)
find_package(JNI REQUIRED)

target_link_libraries(minamo PUBLIC JNI::JNI PkgConfig::VIPS PkgConfig::GLIB)
target_include_directories(minamo PRIVATE ${VIPS_INCLUDE_DIRS} ${GLIB_INCLUDE_DIRS} ${JNI_INCLUDE_DIRS})

install(TARGETS minamo LIBRARY)
install(TARGETS minamo PUBLIC_HEADER)