#!/bin/bash

mkdir -p bin

echo "Compiling..."
javac $(find src | grep .java) -d bin
