FROM maven:3.9.8-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:11-jre-alpine
WORKDIR /app

RUN mkdir -p /app/logs && chown -R 1000:1000 /app/logs && chmod 755 /app/logs

COPY --from=build /app/target/credit-service-0.0.1-SNAPSHOT.jar app.jar

RUN addgroup -S spring -g 1000 && adduser -S spring -G spring -u 1000
RUN chown spring:spring app.jar
USER spring

# Variables para Docker
ENV SPRING_PROFILES_ACTIVE=docker
ENV SPRING_APPLICATION_NAME=credit-service
ENV SPRING_CLOUD_CONFIG_FAIL_FAST=false
ENV SPRING_CONFIG_IMPORT=optional:configserver:http://host.docker.internal:8888
ENV SPRING_CLOUD_CONFIG_PROFILE=docker
ENV SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/db_credit
ENV EXTERNAL_SERVICES_CUSTOMER_URL=http://host.docker.internal:8081
ENV EUREKA_SERVER_URI=http://host.docker.internal:8761/eureka

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]