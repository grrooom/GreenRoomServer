FROM openjdk:17-jdk

WORKDIR /spring-boot

COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/spring-boot/app.jar"]
CMD ["--spring.profiles.active=dev"]


