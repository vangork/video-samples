FROM openjdk:8-jre

# https://gitlab.com/nvidia/container-images/cuda/-/blob/master/dist/11.1.1/ubuntu18.04-x86_64/runtime/cudnn8/Dockerfile
ENV CUDA_VERSION 11.1.1
ENV NCCL_VERSION 2.8.3
ENV CUDNN_VERSION 8.0.5.39

RUN apt-get update && apt-get install -y --no-install-recommends \
    gnupg2 curl ca-certificates && \
    echo 'Acquire::https { Verify-Peer "false"; Verify-Host "false"; }' > /etc/apt/apt.conf.d/allowinsecure && \
    curl -fksSL https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64/7fa2af80.pub | apt-key add - && \
    echo "deb https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64 /" > /etc/apt/sources.list.d/cuda.list && \
    echo "deb https://developer.download.nvidia.com/compute/machine-learning/repos/ubuntu1804/x86_64 /" > /etc/apt/sources.list.d/nvidia-ml.list && \
    apt-get purge --autoremove -y curl \
    && rm -rf /var/lib/apt/lists/*

# For libraries in the cuda-compat-* package: https://docs.nvidia.com/cuda/eula/index.html#attachment-a
RUN apt-get update && apt-get install -y --no-install-recommends \
    cuda-cudart-11-1=11.1.74-1 \
    cuda-compat-11-1 \
    cuda-libraries-11-1=11.1.1-1 \
    libnpp-11-1=11.1.2.301-1 \
    cuda-nvtx-11-1=11.1.74-1 \
    libcublas-11-1=11.3.0.106-1 \
    libnccl2=$NCCL_VERSION-1+cuda11.1 \
    libcudnn8=$CUDNN_VERSION-1+cuda11.1 \
    && ln -s cuda-11.1 /usr/local/cuda && \
    apt-mark hold libcublas-11-1 libnccl2 libcudnn8 && \
    rm -rf /var/lib/apt/lists/*

# Required for nvidia-docker v1
RUN echo "/usr/local/nvidia/lib" >> /etc/ld.so.conf.d/nvidia.conf \
    && echo "/usr/local/nvidia/lib64" >> /etc/ld.so.conf.d/nvidia.conf

ENV PATH /usr/local/nvidia/bin:/usr/local/cuda/bin:${PATH}
ENV LD_LIBRARY_PATH /usr/local/nvidia/lib:/usr/local/nvidia/lib64

# nvidia-container-runtime
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES compute,utility
ENV NVIDIA_REQUIRE_CUDA "cuda>=11.1 brand=tesla,driver>=418,driver<419 brand=tesla,driver>=440,driver<441 driver>=450,driver<451"

# https://eos2git.cec.lab.emc.com/NAUT/analytic-images/blob/master/flink_images/1.10/scala_2.12-debian/Dockerfile
ARG FLINK_VERSION=1.10.1-spring1
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
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-datadog-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-graphite-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-influxdb-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-prometheus-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-slf4j-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-metrics-statsd-${FLINK_VERSION}.jar ${FLINK_HOME}/lib \
    && ln -fs ${FLINK_HOME}/opt/flink-queryable-state-runtime_${SCALA_VERSION}-${FLINK_VERSION}.jar ${FLINK_HOME}/lib

COPY ./libtensorflow_framework.so.1 /usr/lib/x86_64-linux-gnu/

# Configure container
COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 6123 8081
CMD ["help"]
