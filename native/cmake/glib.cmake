include(ExternalProject)

if ($ENV{TARGET} MATCHES ".*android.*")
    set(ENV_PATHS PKG_CONFIG_PATH=${THIRD_PARTY_LIB_PATH}/lib/pkgconfig LIBRARY_PATH=${THIRD_PARTY_LIB_PATH}/lib LD_LIBRARY_PATH=${THIRD_PARTY_LIB_PATH}/lib:$ENV{LD_LIBRARY_PATH}) 
    set(EXTRA_FLAGS -Dlibelf=disabled -Dintrospection=disabled)
endif()

list(APPEND DEPENDENCIES ep_glib)
ExternalProject_Add(ep_glib
    URL https://download.gnome.org/sources/glib/2.80/glib-2.80.0.tar.xz
    URL_HASH SHA256=8228a92f92a412160b139ae68b6345bd28f24434a7b5af150ebe21ff587a561d
    CONFIGURE_COMMAND
        ${ENV_PATHS} ${Meson_EXECUTABLE} setup ${MESON_CROSS_FILE_ARG} --prefix=<INSTALL_DIR>
        <BINARY_DIR> <SOURCE_DIR>
        -Dlibelf=disabled
        -Dintrospection=disabled
    BUILD_COMMAND
        ${Meson_EXECUTABLE} compile -C <BINARY_DIR>
    INSTALL_COMMAND
        ${Meson_EXECUTABLE} install -C <BINARY_DIR>
)
list(APPEND EXTRA_CMAKE_ARGS -DGLIB_INCLUDE_1=${THIRD_PARTY_LIB_PATH}/include/glib-2.0 -DGLIB_INCLUDE_2=${THIRD_PARTY_LIB_PATH}/lib/glib-2.0/include)