FROM openjdk:8-jre

# https://eos2git.cec.lab.emc.com/NAUT/analytic-images/blob/master/flink_images/1.10/scala_2.12-debian/Dockerfile
ARG FLINK_VERSION=1.11.2-spring1
ARG SCALA_VERSION=2.12

RUN set -ex; \
    apt-get update; \
    apt-get -y install libsnappy1v5 gettext-base wget gnupg zip unzip; \
    rm -rf /var/lib/apt/lists/*;

# Grab gosu for easy step-down from root
ENV GOSU_VERSION 1.11
RUN set -ex; \
    wget -nv -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)"; \
    wget -nv -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc"; \
    export GNUPGHOME="$(mktemp -d)"; \
    for server in ha.pool.sks-keyservers.net $(shuf -e \
                          hkp://p80.pool.sks-keyservers.net:80 \
                          keyserver.ubuntu.com \
                          hkp://keyserver.ubuntu.com:80 \
                          pgp.mit.edu) ; do \
        gpg --batch --keyserver "$server" --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 && break || : ; \
    done && \
    gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu; \
    gpgconf --kill all; \
    rm -rf "$GNUPGHOME" /usr/local/bin/gosu.asc; \
    chmod +x /usr/local/bin/gosu; \
    gosu nobody true

# Prepare environment
ENV FLINK_HOME=/opt/flink
ENV PATH=$FLINK_HOME/bin:$PATH
RUN groupadd --system --gid=9999 flink && \
    useradd --system --home-dir $FLINK_HOME --uid=9999 --gid=flink flink
WORKDIR $FLINK_HOME

ENV FLINK_URL_FILE_PATH=flink-ververica/flink-${FLINK_VERSION}-scala_${SCALA_VERSION}.tgz
ENV FLINK_TGZ_URL=http://asdrepo.isus.emc.com:8081/artifactory/nautilus-build/${FLINK_URL_FILE_PATH}

# Install Flink
RUN set -ex; \
    wget -nv -O flink.tgz "$FLINK_TGZ_URL"; \
    \
    tar -xf flink.tgz --strip-components=1; \
    rm flink.tgz; \
    \
    chown -R flink:flink .;

# Add optional libraries into Flink classpath
RUN mkdir -p ${FLINK_HOME}/plugins/s3-fs-presto \
    && mkdir -p ${FLINK_HOME}/plugins/s3-fs-hadoop \
    && ln -fs ${FLINK_HOME}/opt/flink-s3-fs-presto-${FLINK_VERSION}.jar ${FLINK_HOME}/plugins/s3-fs-presto \
    && ln -fs ${FLINK_HOME}/opt/flink-s3-fs-hadoop-${FLINK_VERSION}.jar ${FLINK_HOME}/plugins/s3-fs-hadoop \
    && ln -fs ${FLINK_HOME}/opt/flink-queryable-state-runtime_${SCALA_VERSION}-${FLINK_VERSION}.jar ${FLINK_HOME}/lib

ARG TF_VERSION=1.15.0
ARG TF_JNI_FILE=https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-cpu-linux-x86_64-${TF_VERSION}.tar.gz
ARG LIB_TF_FILE=https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow-${TF_VERSION}.jar
RUN set -ex; \
    mkdir -p ${FLINK_HOME}/plugins/tensorflow/lib; \
    wget --no-check-certificate -nv -O ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz "$TF_JNI_FILE"; \
    tar -zxvf ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz -C ${FLINK_HOME}/plugins/tensorflow/lib/; \
    rm -f ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz; \
    wget --no-check-certificate -nv -O ${FLINK_HOME}/plugins/tensorflow/libtensorflow-${TF_VERSION}.jar "$LIB_TF_FILE"; \
    ln -fs ${FLINK_HOME}/plugins/tensorflow/libtensorflow-${TF_VERSION}.jar ${FLINK_HOME}/lib;
ENV LD_LIBRARY_PATH ${FLINK_HOME}/plugins/tensorflow/lib:$LD_LIBRARY_PATH

# Configure container
COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 6123 8081
CMD ["help"]
