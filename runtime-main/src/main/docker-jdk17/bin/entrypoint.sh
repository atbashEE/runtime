#!/bin/sh
set -e

if [ ${STATELESS} = 'true' ]
then
   ATBASH_ARGS="${ATBASH_ARGS} --stateless --no-logToFile"
fi
if [ -f "${CONFIG_FILE_LOCATION}" ]
then
   ATBASH_ARGS="${ATBASH_ARGS} -c ${CONFIG_FILE_LOCATION}"
fi

exec java -XX:MaxRAMPercentage=${MEM_MAX_RAM_PERCENTAGE} -Xss${MEM_XSS} -XX:+UseContainerSupport ${JVM_ARGS} -jar atbash-runtime.jar --logToConsole --deploymentdirectory ${DEPLOYMENT_DIR} ${ATBASH_ARGS}
