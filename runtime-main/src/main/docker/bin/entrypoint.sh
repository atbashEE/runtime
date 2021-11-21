#!/bin/sh
set -e

exec java -XX:MaxRAMPercentage=${MEM_MAX_RAM_PERCENTAGE} -Xss${MEM_XSS} -XX:+UseContainerSupport ${JVM_ARGS} -jar atbash-runtime.jar --deploymentdirectory ${DEPLOYMENT_DIR} "$@"
