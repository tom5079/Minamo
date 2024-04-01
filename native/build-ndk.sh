#!/bin/bash

######
# USAGE: NDK=<PATH_TO_NDK> TOOLCHAIN=<PATH_TO_TOOLCHAIN> ./build-ndk.sh <ARCHITECTURE>
######

set -e

if [ -z ${1} ]; then
    ./$0 armv7a
    ./$0 aarch64
    ./$0 i686
    ./$0 x86_64
    exit
fi

ARCH_LOOKUP=$(cat << EOL
{
    "armv7a": {
        "cpu_family": "arm",
        "target": "armv7a-linux-androideabi",
        "arch_abi": "armeabi-v7a",
        "lib": "arm-linux-androideabi"
    },
    "aarch64": {
        "cpu_family": "aarch64",
        "target": "aarch64-linux-android",
        "arch_abi": "arm64-v8a",
        "lib": "aarch64-linux-android"
    },
    "i686": {
        "cpu_family": "x86",
        "target": "i686-linux-android",
        "arch_abi": "x86",
        "lib": "i686-linux-android"
    },
    "x86_64": {
        "cpu_family": "x86_64",
        "target": "x86_64-linux-android",
        "arch_abi": "x86_64",
        "lib": "x86_64-linux-android"
    }
}
EOL
)

CPU=${1}
CPU_FAMILY=$(echo $ARCH_LOOKUP | jq -r .${1}.cpu_family)

SYSROOT="$(readlink -f .)/build-ndk-${1}/fakeroot"

export TARGET=$(echo $ARCH_LOOKUP | jq -r .${1}.target)
export API=21
export AR=$TOOLCHAIN/bin/llvm-ar
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export AS=$CC
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
export LD=$TOOLCHAIN/bin/ld
export RANLIB=$TOOLCHAIN/bin/llvm-ranlib
export STRIP=$TOOLCHAIN/bin/llvm-strip

cat << EOF > "cmake/cross-file-${1}.txt"
[host_machine]
system = 'android'
cpu_family = '$CPU_FAMILY'
cpu = '$CPU'
endian = 'little'

[built-in options]
c_args = ['-I$SYSROOT/include', '-Wno-error=format-nonliteral']
cpp_args = ['-I$SYSROOT/include', '-Wno-error=format-nonliteral']
c_link_args = ['-L$SYSROOT/lib']
cpp_link_args = ['-L$SYSROOT/lib']

[binaries]
c = '$TOOLCHAIN/bin/$TARGET$API-clang'
cpp = '$TOOLCHAIN/bin/$TARGET$API-clang++'
ar = '$TOOLCHAIN/bin/llvm-ar'
strip = '$TOOLCHAIN/bin/llvm-strip'
ranlib = '$TOOLCHAIN/bin/llvm-ranlib'
c_ld = '$TOOLCHAIN/bin/ld'
pkg-config = '$(which pkg-config)'

[cmake]
CMAKE_BUILD_WITH_INSTALL_RPATH     = 'ON'
CMAKE_FIND_ROOT_PATH_MODE_PROGRAM  = 'NEVER'
CMAKE_FIND_ROOT_PATH_MODE_LIBRARY  = 'ONLY'
CMAKE_FIND_ROOT_PATH_MODE_INCLUDE  = 'ONLY'
CMAKE_FIND_ROOT_PATH_MODE_PACKAGE  = 'ONLY'
EOF

export PKG_CONFIG_DIR=""
export PKG_CONFIG_LIBDIR="$SYSROOT/lib/pkgconfig"
export PKG_CONFIG_SYSROOT_DIR=$SYSROOT

mkdir -p $PKG_CONFIG_LIBDIR
mkdir -p $SYSROOT/include
cd build-ndk-${1}

[[ ! -h $SYSROOT/lib/libz.so ]] && ln -s $TOOLCHAIN/sysroot/usr/lib/$TARGET/$API/libz.so $SYSROOT/lib/libz.so
[[ ! -h $SYSROOT/include/zlib.h ]] && ln -s $TOOLCHAIN/sysroot/usr/include/zlib.h $SYSROOT/include/zlib.h

cat << EOF > "$PKG_CONFIG_LIBDIR/zlib.pc"
prefix=$SYSROOT
exec_prefix=\${prefix}
libdir=\${exec_prefix}/lib
sharedlibdir=\${libdir}
includedir=\${prefix}/include

Name: zlib
Description: zlib compression library
Version: 1.0.0

Requires:
Libs: -L\${libdir} -L\${sharedlibdir} -lz
Cflags: -I\${includedir}
EOF

cmake .. \
    -DCMAKE_SYSTEM_NAME=Android \
    -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
    -DCMAKE_PREFIX_PATH=$(readlink -f fakeroot) \
    -DCMAKE_FIND_ROOT_PATH=$(readlink -f fakeroot) \
    -DANDROID_ABI=$(echo $ARCH_LOOKUP | jq -r .${1}.arch_abi) \
    -DAPI=$ANDROID_PLATFORM \
    -DMESON_CROSS_FILE=$(readlink -f ../cmake/cross-file-${1}.txt)
cmake --build . -j

for lib in fakeroot/lib/*so; do
    [[ ! -f $lib ]] || $TOOLCHAIN/bin/llvm-strip $lib
done
