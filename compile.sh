#! /bin/bash
. $(dirname $0)/env.sh
mkdir -p classes
rm -fr classes/*
javac -Xlint:unchecked -d classes -cp $CLASSPATH $(find . -name *.java) $*
