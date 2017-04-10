#!/bin/bash

rm -r bin/
mkdir bin

echo "Compiling..."
javac $(find src | grep .java) -d bin
