#!/bin/sh -

iswindows=0

case "$(uname)" in
	CYGWIN*)	iswindows=1;;
	MINGW*)		iswindows=1;;
esac

jar=$(ls -1t build/libs/*.jar | head -1)

if [ "$iswindows" -eq 1 ]
then
	jar="$(echo $jar | sed "s/\\//\\\\/g")"
fi

echo "Running JAR \"$jar\""
echo

if [ "$iswindows" -eq 1 ]
then
	java $@ -cp "$jar;dependencies\\*" com.jinotrain.badforum.main.Bootstrapper
else
	java $@ -cp "$jar:dependencies/*" com.jinotrain.badforum.main.Bootstrapper
fi
