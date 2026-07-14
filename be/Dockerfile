FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM tomcat:9.0-jdk17-temurin
ENV SPRING_PROFILES_ACTIVE=prod
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/pay-with-api.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
