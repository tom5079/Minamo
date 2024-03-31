cmake_minimum_required(VERSION 3.26)

project(minamo_test C)

include(CTest)
include(CMakeFindDependencyMacro)

find_library(VIPS_LIB NAMES vips PATHS ${CUSTOM_LIB_PATH} PATH_SUFFIXES lib REQUIRED NO_DEFAULT_PATH)
find_library(MINAMO_LIB NAMES minamo PATHS ${CUSTOM_LIB_PATH} PATH_SUFFIXES lib REQUIRED NO_DEFAULT_PATH)

set(TEST_INCLUDES ${CUSTOM_LIB_PATH}/include)

add_executable(minamo_test_jpg test_jpg.c)
target_link_libraries(minamo_test_jpg ${MINAMO_LIB})
target_include_directories(minamo_test_jpg PRIVATE ${TEST_INCLUDES})

enable_testing()
add_test(NAME minamo_test_jpg COMMAND minamo_test_jpg ${CMAKE_CURRENT_LIST_DIR}/image.jpg)