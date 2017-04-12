#!/bin/bash

rm -r bin
mkdir -p bin
mkdir -p bin/my_peers

echo "Compiling..."
javac $(find src | grep .java) -d bin
