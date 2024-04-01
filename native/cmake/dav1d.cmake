include(ExternalProject)

if (MESON_CROSS_FILE)
    set(MESON_CROSS_FILE_ARG --cross-file=${MESON_CROSS_FILE})
endif()

ExternalProject_Add(ep_dav1d
    GIT_REPOSITORY      https://code.videolan.org/videolan/dav1d.git
    GIT_TAG             1.3.0
    CONFIGURE_COMMAND
        ${Meson_EXECUTABLE} setup ${MESON_CROSS_FILE_ARG} --prefix=<INSTALL_DIR> <BINARY_DIR> <SOURCE_DIR>
    BUILD_COMMAND
        ${Ninja_EXECUTABLE} -C <BINARY_DIR>
    INSTALL_COMMAND
        ${Ninja_EXECUTABLE} -C <BINARY_DIR> install
)