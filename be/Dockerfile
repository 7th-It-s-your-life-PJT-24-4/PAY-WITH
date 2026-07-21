FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies
COPY src ./src
RUN ./gradlew --no-daemon clean war -x test

FROM tomcat:9.0-jdk17-temurin
ENV SPRING_PROFILES_ACTIVE=prod
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/build/libs/pay-with-api.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
