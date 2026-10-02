#!/bin/sh
# Minimal maven wrapper script
set -e
BASE_DIR=$(dirname "$0")
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    mkdir -p "$BASE_DIR/.mvn/wrapper"
    DOWNLOAD_URL=$(grep "wrapperUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
    if [ -z "$DOWNLOAD_URL" ]; then
        DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    fi
    curl -sSLo "$WRAPPER_JAR" "$DOWNLOAD_URL" || wget -qO "$WRAPPER_JAR" "$DOWNLOAD_URL"
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
