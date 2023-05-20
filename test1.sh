#! /bin/bash
$(dirname $0)/compile.sh
. $(dirname $0)/env.sh
java -ea -cp $CLASSPATH SetTest1 $*
