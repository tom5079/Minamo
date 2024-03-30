#!/bin/bash
set -e

mkdir -p build
cd build

cmake ..
cmake --build . -j $(nproc)

for lib in fakeroot/lib/*so; do
    strip $lib
done