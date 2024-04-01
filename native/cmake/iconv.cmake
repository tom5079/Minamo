include(ExternalProject)

if (DEFINED ENV{TARGET})
    set(HOST_ARG --host=$ENV{TARGET})
endif()

ExternalProject_Add(ep_iconv
    URL https://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.17.tar.gz
    BUILD_IN_SOURCE 1
    CONFIGURE_COMMAND
        ./configure ${HOST_ARG} --prefix ${CMAKE_BINARY_DIR}/fakeroot
    BUILD_COMMAND
        ${Make_EXECUTABLE}
    INSTALL_COMMAND
        ${Make_EXECUTABLE} install -j
)