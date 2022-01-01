##FROM azul/zulu-openjdk:11.0.13
FROM azul/zulu-openjdk-alpine:11.0.13-jre-headless

# Default ports to expose
EXPOSE 8080
# Can be used for debugging within Container.
EXPOSE 5005

ENV HOME_DIR=/opt/atbash
ENV SCRIPT_DIR=${HOME_DIR} \
    DEPLOYMENT_DIR=${HOME_DIR}/deployment \
    JVM_ARGS="" \
    ATBASH_ARGS="" \
    STATELESS="true" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k"

RUN true \
    && mkdir -p "${HOME_DIR}" \
    && mkdir -p "${DEPLOYMENT_DIR}" \
    && addgroup --gid 1000 atbash \
    && adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup atbash atbash \
    && echo atbash:atbash | chpasswd \
    && mkdir -p "${HOME_DIR}" \
    && chown -R atbash:atbash ${HOME_DIR} \
    && true

USER atbash
WORKDIR ${HOME_DIR}

COPY --chown=atbash:atbash maven/bin/* ${SCRIPT_DIR}/
COPY --chown=atbash:atbash maven/atbash-runtime.jar .

ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
