##FROM azul/zulu-openjdk:17.0.1
FROM azul/zulu-openjdk-alpine:17.0.1-jre-headless

# Default ports to expose
EXPOSE 8080
# Can be used for debugging within Container.
EXPOSE 5005

ENV HOME_DIR=/opt/atbash
ENV DEPLOYMENT_DIR=${HOME_DIR}/deployment \
    CONFIG_FILE_LOCATION=${HOME_DIR}/configfile.boot \
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

COPY --chown=atbash:atbash maven/expanded .

ENTRYPOINT ["/bin/sh", "bin/entrypoint.sh"]
