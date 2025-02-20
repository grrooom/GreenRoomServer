FROM openjdk:17-jdk

WORKDIR /spring-boot

RUN mkdir -p /var/log/greenroom/dev && chmod -R 777 /var/log/greenroom/dev

RUN mkdir -p /var/elasticsearch/certs && chmod -R 777 /var/elasticsearch/certs

COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar","/spring-boot/app.jar", "--spring.profiles.active=dev"]

