#!/bin/bash

find . -type f -iname "*.proto" -exec protoc --java_out=$(pwd)/src/main/java {} \;