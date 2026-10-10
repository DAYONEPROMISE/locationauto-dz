#!/bin/sh
APP_BASE_NAME=${0##*/}
APP_HOME=$(cd "${APP_HOME:-./}" && pwd -P) || exit
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
