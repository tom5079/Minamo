include(ExternalProject)

include("cmake/iconv.cmake")

if (MESON_CROSS_FILE)
    set(MESON_CROSS_FILE_ARG --cross-file=${MESON_CROSS_FILE})
endif()

ExternalProject_Add(ep_glib
    URL https://download.gnome.org/sources/glib/2.80/glib-2.80.0.tar.xz
    URL_HASH SHA256=8228a92f92a412160b139ae68b6345bd28f24434a7b5af150ebe21ff587a561d
    DEPENDS ep_iconv
    CONFIGURE_COMMAND
        ${Meson_EXECUTABLE} setup ${MESON_CROSS_FILE_ARG} --prefix=<INSTALL_DIR>
        <BINARY_DIR> <SOURCE_DIR>
        -Dlibelf=disabled
        -Dintrospection=disabled
        -Dtests=false
    BUILD_COMMAND
        ${Meson_EXECUTABLE} compile -C <BINARY_DIR>
    INSTALL_COMMAND
        ${Meson_EXECUTABLE} install -C <BINARY_DIR>
)