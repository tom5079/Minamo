#!/bin/bash

set -e

mkdir -p build-windows
cd build-windows

export TARGET=x86_64-w64-mingw32
export AR=x86_64-w64-mingw32-ar
export CC=x86_64-w64-mingw32-gcc
export CXX=x86_64-w64-mingw32-g++
export AS=x86_64-w64-mingw32-as
export STRIP=x86_64-w64-mingw32-strip
export RANLIB=x86_64-w64-mingw32-ranlib
export LD=x86_64-w64-mingw32-ld
export WINDRES=x86_64-w64-mingw32-windres

cat << EOF > ../cmake/toolchain-windows.cmake
set(CMAKE_SYSTEM_NAME Windows)

set(CMAKE_SYSTEM_PROCESSOR X86)

set(CMAKE_C_COMPILER x86_64-w64-mingw32-gcc)
set(CMAKE_CXX_COMPILER x86_64-w64-mingw32-g++)

set(CMAKE_FIND_ROOT_PATH /usr/x86_64-w64-mingw32 $(readlink -f .)/fakeroot)
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
EOF

cat << EOF > ../cmake/cross-file-windows.txt
[host_machine]
system = 'windows'
cpu_family = 'x86_64'
cpu = 'x86_64'
endian = 'little'

[binaries]
c = 'x86_64-w64-mingw32-gcc'
cpp = 'x86_64-w64-mingw32-g++'
ar = 'x86_64-w64-mingw32-ar'
strip = 'x86_64-w64-mingw32-strip'
pkgconfig = 'x86_64-pc-linux-gnu-pkg-config'

[cmake]
CMAKE_BUILD_WITH_INSTALL_RPATH     = 'ON'
CMAKE_FIND_ROOT_PATH_MODE_PROGRAM  = 'NEVER'
CMAKE_FIND_ROOT_PATH_MODE_LIBRARY  = 'ONLY'
CMAKE_FIND_ROOT_PATH_MODE_INCLUDE  = 'ONLY'
CMAKE_FIND_ROOT_PATH_MODE_PACKAGE  = 'ONLY'
EOF

export PKG_CONFIG_DIR=""
export PKG_CONFIG_LIBDIR="$(readlink -f .)/fakeroot/lib/pkgconfig"
export PKG_CONFIG_SYSROOT_DIR="$(readlink -f .)/fakeroot"

cmake .. \
    -DCMAKE_TOOLCHAIN_FILE=$(readlink -f ../cmake/toolchain-windows.cmake) \
    -DCMAKE_PREFIX_PATH=$(readlink -f .)/fakeroot \
    -DCMAKE_FIND_ROOT_PATH=$(readlink -f .)/fakeroot \
    -DMESON_CROSS_FILE=$(readlink -f ../cmake/cross-file-windows.txt) \
    -G Ninja

cmake --build .