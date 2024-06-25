cmake_minimum_required(VERSION 3.26)

project(minamo C)

if(ANDROID)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} -llog")
endif()

add_library(minamo SHARED imagesource/file.c imagesource/descriptor.c minamo_image.c minamo_sink_callback.c)

find_package(PkgConfig REQUIRED)
pkg_check_modules(VIPS REQUIRED IMPORTED_TARGET vips)
find_package(JNI REQUIRED)

message(STATUS "VIPS_FOUND: ${VIPS_FOUND}")
message(STATUS "VIPS_LIBRARIES: ${VIPS_LIBRARIES}")
message(STATUS "VIPS_INCLUDE_DIRS: ${VIPS_INCLUDE_DIRS}")

target_link_libraries(minamo JNI::JNI PkgConfig::VIPS)
target_include_directories(minamo PUBLIC ${JNI_INCLUDE_DIRS} ${VIPS_INCLUDE_DIRS})
target_compile_options(minamo PUBLIC ${VIPS_CFLAGS_OTHER})

install(TARGETS minamo LIBRARY)
install(TARGETS minamo PUBLIC_HEADER)