include(ExternalProject)

include("cmake/dav1d.cmake")
include("cmake/de265.cmake")

ExternalProject_Add(ep_heif
    GIT_REPOSITORY      https://github.com/strukturag/libheif.git
    GIT_TAG             v1.17.6
    DEPENDS ep_dav1d ep_de265
    CMAKE_ARGS
        -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/fakeroot
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DWITH_GDK_PIXBUF=OFF
        -DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH}
        -DCMAKE_FIND_ROOT_PATH=${CMAKE_FIND_ROOT_PATH}
        -DWITH_DAV1D=ON
        -DWITH_LIBDE265=ON
        -DWITH_RAV1E=OFF
        -DWITH_X265=OFF
        -DWITH_AOM_DECODER=OFF
        -DWITH_AOM_ENCODER=OFF
        -DWITH_SvtEnc=OFF
        -DWITH_FFMPEG_DECODER=OFF
        -DWITH_FFMPEG_ENCODER=OFF
        -DWITH_JPEG_DECODER=OFF
        -DWITH_JPEG_ENCODER=OFF
        -DWITH_KVAZAAR=OFF
        -DWITH_OpenJPEG_DECODER=OFF
        -DWITH_OpenJPEG_ENCODER=OFF
        -DWITH_DAV1D_PLUGIN=OFF
        -DWITH_LIBDE265_PLUGIN=OFF
        --preset=release
)