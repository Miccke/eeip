#!/usr/bin/env bash
set -e
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"
cd "$EEIP_HOME"
echo "构建后端..."
mvn -q -f backend/pom.xml $MAVEN_ARGS -B package -DskipTests
echo "启动后端 http://localhost:8080 ..."
exec java -jar backend/target/eeip-boot-0.1.0-SNAPSHOT.jar
