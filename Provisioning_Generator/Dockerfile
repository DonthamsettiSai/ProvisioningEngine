#From openjdk:17-jdk-slim
#RUN apt-get update && apt-get install -y tzdata && ln -fs /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && dpkg-reconfigure --frontend noninteractive tzdata #&& rm -rf /var/lib/apt/lists/*
#RUN ln -fs /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && echo "Asia/Kolkata" > /etc/timezone
#EXPOSE 8072
#ADD /target/provisioning-generator.jar prov-gen
#ENTRYPOINT ["java","-Dspring.profiles.active=${ACTIVE_PROFILE}","-jar","/prov-gen"]


#=======================

# Use CentOS 8 as base image (with vault repo fix)

FROM centos:8
 
# Fix CentOS 8 repo issue (switch to vault)

RUN sed -i 's|mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-* && \

    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*
 
# Set environment variables

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk

ENV PATH="/usr/lib/jvm/java-17-openjdk/bin:${PATH}"
 
# Install OpenJDK 17, Maven, SSH, fonts, tzdata, and font libraries

# RUN yum -y update && \

#     yum -y install java-17-openjdk java-17-openjdk-devel maven openssh-server tzdata dejavu-sans-fonts freetype fontconfig libXrender && \

#     ln -fs /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && \

#     yum clean all
 
# Copy libs and JAR

COPY libs/ /libs/

RUN yum -y update && \
    yum -y install java-17-openjdk java-17-openjdk-devel openssh-server tzdata dejavu-sans-fonts freetype fontconfig libXrender && \
    ln -fs /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && \
    yum clean all
 

 
# Install local library into Maven repo

#RUN mvn install:install-file \

#   -Dfile=/libs/jwt-library.jar \

#    -DgroupId=com.knot \

#    -DartifactId=jwt-library \

#   -Dversion=1.0.0 \

#   -Dpackaging=jar
 
EXPOSE 8072
ADD /target/provisioning-generator.jar prov-gen

ENTRYPOINT ["java", "-Dloader.path=/libs", "-Dspring.profiles.active=${ACTIVE_PROFILE}","-jar","/prov-gen"]