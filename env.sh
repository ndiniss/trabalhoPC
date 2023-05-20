#! /bin/bash

CLASSPATH=''

LIB_DIR="$(dirname $0)/lib"
for file in $(ls $LIB_DIR); do
  # echo $file
  CLASSPATH+="$LIB_DIR/$file:"
done

CLASSPATH+="$(dirname $0)/classes"
