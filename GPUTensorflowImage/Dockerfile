FROM devops-repo.isus.emc.com:8116/nautilus/nautilus-jre:8u265-slim-buster

# https://gitlab.com/nvidia/container-images/cuda/-/blob/master/dist/11.0.3/ubuntu18.04-x86_64/base/Dockerfile

RUN apt-get update && apt-get install -y --no-install-recommends \
    gnupg2 curl ca-certificates && \
    echo 'Acquire::https { Verify-Peer "false"; Verify-Host "false"; }' > /etc/apt/apt.conf.d/allowinsecure && \
    curl -fksSL https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64/7fa2af80.pub | apt-key add - && \
    echo "deb https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64 /" > /etc/apt/sources.list.d/cuda.list && \
    echo "deb https://developer.download.nvidia.com/compute/machine-learning/repos/ubuntu1804/x86_64 /" > /etc/apt/sources.list.d/nvidia-ml.list && \
    apt-get purge --autoremove -y curl \
    && rm -rf /var/lib/apt/lists/*

ENV CUDA_VERSION 10.0.130
ENV CUDA_PKG_VERSION 10-0=$CUDA_VERSION-1

# For libraries in the cuda-compat-* package: https://docs.nvidia.com/cuda/eula/index.html#attachment-a
RUN apt-get update && apt-get install -y --no-install-recommends \
    cuda-cudart-$CUDA_PKG_VERSION \
    cuda-compat-10-0 \
    && ln -s cuda-10.0 /usr/local/cuda && \
    rm -rf /var/lib/apt/lists/*

# Required for nvidia-docker v1
RUN echo "/usr/local/nvidia/lib" >> /etc/ld.so.conf.d/nvidia.conf && \
    echo "/usr/local/nvidia/lib64" >> /etc/ld.so.conf.d/nvidia.conf

ENV PATH /usr/local/nvidia/bin:/usr/local/cuda/bin:${PATH}
ENV LD_LIBRARY_PATH /usr/local/nvidia/lib:/usr/local/nvidia/lib64

# nvidia-container-runtime
ENV NVIDIA_VISIBLE_DEVICES all
ENV NVIDIA_DRIVER_CAPABILITIES compute,utility
ENV NVIDIA_REQUIRE_CUDA "cuda>=10.0 brand=tesla,driver>=384,driver<385 brand=tesla,driver>=410,driver<411"

#https://github.com/tensorflow/tensorflow/blob/master/tensorflow/tools/dockerfiles/dockerfiles/gpu.Dockerfile
ARG CUDA=10.0
ARG CUDNN=7.6.2.24-1

# Needed for string substitution
SHELL ["/bin/bash", "-c"]
# Pick up some TF dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cuda-command-line-tools-${CUDA/./-} \
    cuda-cublas-${CUDA/./-} \
    cuda-cufft-${CUDA/./-} \
    cuda-curand-${CUDA/./-} \
    cuda-cusolver-${CUDA/./-} \
    cuda-cusparse-${CUDA/./-} \
    curl \
    libcudnn7=${CUDNN}+cuda${CUDA} \
    libfreetype6-dev \
    libhdf5-serial-dev \
    libzmq3-dev \
    pkg-config \
    software-properties-common \
    unzip

# Install TensorRT
RUN apt-get install -y --no-install-recommends libnvinfer5=5.1.5-1+cuda${CUDA} \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# For CUDA profiling, TensorFlow requires CUPTI.
ENV LD_LIBRARY_PATH /usr/local/cuda/extras/CUPTI/lib64:/usr/local/cuda/lib64:$LD_LIBRARY_PATH

# Link the libcuda stub to the location where tensorflow is searching for it and reconfigure
# dynamic linker run-time bindings
RUN ln -s /usr/local/cuda/lib64/stubs/libcuda.so /usr/local/cuda/lib64/stubs/libcuda.so.1 \
    && echo "/usr/local/cuda/lib64/stubs" > /etc/ld.so.conf.d/z-cuda-stubs.conf \
    && ldconfig

## See http://bugs.python.org/issue19846
#ENV LANG C.UTF-8
#
#RUN apt-get update && apt-get install -y \
#    python3 \
#    python3-pip
#
#RUN python3 -m pip --no-cache-dir install --upgrade \
#    "pip<20.3" \
#    setuptools
#
## Some TF tools expect a "python" binary
#RUN ln -s $(which python3) /usr/local/bin/python
#
## https://www.tensorflow.org/install/gpu
## Installs tensorflow
## 对于 1.15 及更早版本，CPU 和 GPU 软件包是分开的
#RUN python3 -m pip install --no-cache-dir tensorflow==1.15
#RUN python3 -m pip install --no-cache-dir tensorflow-gpu==1.15
#
#ENV LD_LIBRARY_PATH /usr/local/lib/python3.7/dist-packages/tensorflow_core:$LD_LIBRARY_PATH

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

#COPY ../flinkprocessor/build/libs/video-flinkprocessor-0.1.0.jar ${FLINK_HOME}/lib
ARG TF_VERSION=1.15.0
ARG TF_JNI_FILE=https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow_jni-gpu-linux-x86_64-${TF_VERSION}.tar.gz
ARG LIB_TF_FILE=https://storage.googleapis.com/tensorflow/libtensorflow/libtensorflow-${TF_VERSION}.jar
RUN set -ex; \
    mkdir -p ${FLINK_HOME}/plugins/tensorflow/lib; \
    wget --no-check-certificate -nv -O ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz "$TF_JNI_FILE"; \
    tar -zxvf ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz -C ${FLINK_HOME}/plugins/tensorflow/jni/; \
    rm -f ${FLINK_HOME}/plugins/tensorflow/lib/jni.tar.gz; \
    wget --no-check-certificate -nv -O ${FLINK_HOME}/plugins/tensorflow/libtensorflow-${TF_VERSION}.jar "$LIB_TF_FILE"; \
    ln -fs ${FLINK_HOME}/plugins/tensorflow/libtensorflow-${TF_VERSION}.jar ${FLINK_HOME}/lib;
ENV LD_LIBRARY_PATH ${FLINK_HOME}/plugins/tensorflow/lib:$LD_LIBRARY_PATH

# Configure container
COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 6123 8081
CMD ["help"]
