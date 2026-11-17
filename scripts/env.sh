#!/usr/bin/env bash
# EEIP 开发环境变量（便携 JDK17 + Maven，不污染系统环境）
EEIP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export EEIP_HOME
export JAVA_HOME="$EEIP_HOME/.tools/jdk17/Contents/Home"
export PATH="$JAVA_HOME/bin:$EEIP_HOME/.tools/maven/bin:$PATH"
export MAVEN_ARGS="-s $EEIP_HOME/.tools/settings.xml"
export TZ=Asia/Shanghai
