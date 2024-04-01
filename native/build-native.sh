#!/bin/bash
set -e

mkdir -p build
cd build

# export PKG_CONFIG_DIR=""
# export PKG_CONFIG_LIBDIR="$SYSROOT/lib/pkgconfig"
# export PKG_CONFIG_SYSROOT_DIR=$SYSROOT
export PKG_CONFIG_PATH="$(readlink -f .)/fakeroot/lib/pkgconfig"

cmake ..
cmake --build . -j

for lib in fakeroot/lib/*so; do
    strip $lib
done