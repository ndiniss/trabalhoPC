#! /bin/bash
. $(dirname $0)/env.sh
java -cp $CLASSPATH SetTest1 $*
read -s -n 1 -p "Press any key to continue . . ."