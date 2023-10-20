FROM ubuntu:bionic
LABEL maintainder="JoaoErick <jsilva@ecomp.uefs.br>"

WORKDIR /opt

ADD target/virtual-fot-device-1.0-SNAPSHOT-jar-with-dependencies.jar ./device.jar

RUN mvn -Pnative -DskipTests package && ls

RUN apt-get update -y && apt-get upgrade -y && apt-get autoremove -y\ 
	&& apt-get install net-tools -y\
	&& apt-get install iproute2 -y\
	&& apt-get install iputils-ping -y\
	&& apt-get install openjdk-11-jdk -y\
	&& apt-get autoremove -y\
	&& apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ENTRYPOINT ["java", "-jar", "device.jar"]