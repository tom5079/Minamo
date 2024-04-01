include(ExternalProject)

find_program(Autoreconf_EXECUTABLE autoreconf)
if (NOT Autoreconf_EXECUTABLE)
    message(FATAL_ERROR "Autoreconf is required")
endif()

if (DEFINED ENV{TARGET})
    set(HOST_ARG --host=$ENV{TARGET})
endif()

ExternalProject_Add(ep_exif
    GIT_REPOSITORY https://github.com/libexif/libexif
    GIT_TAG v0.6.24
    BUILD_IN_SOURCE TRUE
    CONFIGURE_COMMAND test -f ./configure || ${Autoreconf_EXECUTABLE} -i COMMAND ./configure ${HOST_ARG} --prefix ${CMAKE_BINARY_DIR}/fakeroot
    BUILD_COMMAND
        ${Make_EXECUTABLE}
    INSTALL_COMMAND
        ${Make_EXECUTABLE} install -j
)