#
# Container for running Java processes
#

# sha1sum ../SshdContainer/Dockerfile | cut -c 1-12
FROM jenkins/sshd:32edfdd58111

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        software-properties-common \
        openjdk-8-jre-headless \
        openjdk-8-jdk-headless \
        curl \
        ant \
        maven \
        git
