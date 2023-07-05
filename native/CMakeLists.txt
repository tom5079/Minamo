cmake_minimum_required(VERSION 3.26)
project(ssi C)
add_library(ssi SHARED src/ssi.c src/ssi_jni.c)

find_package(JNI REQUIRED)
target_link_libraries(ssi PRIVATE JNI::JNI)