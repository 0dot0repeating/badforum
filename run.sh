#!/bin/sh -
jar=$(ls -1t build/libs/*.jar | head -1)
printf "Running JAR \"$jar\"\n\n"

java $@ -cp "$jar:dependencies/*" com.jinotrain.badforum.main.Bootstrapper
