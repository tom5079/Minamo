include(ExternalProject)

ExternalProject_Add(ep_jxl
    GIT_REPOSITORY      https://github.com/libjxl/libjxl
    GIT_TAG             v0.10.2
    CMAKE_ARGS
        -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/fakeroot
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH}
        -DCMAKE_FIND_ROOT_PATH=${CMAKE_FIND_ROOT_PATH}
        # -DJPEGXL_ENABLE_JPEGLI=false
        -DJPEGXL_ENABLE_OPENEXR=false
        -DJPEGXL_ENABLE_TOOLS=false
        -DJPEGXL_BUNDLE_LIBPNG=false
        -DJPEGXL_ENABLE_JNI=false
        -DJPEGXL_ENABLE_SJPEG=false
        -DBUILD_TESTING=false
)