#! /bin/bash
. $(dirname $0)/env.sh
mkdir -p classes
rm -fr classes/*
javac -Xlint:unchecked -d classes -cp $CLASSPATH $(find . -name *.java) $*
read -s -n 1 -p "Press any key to continue . . ."