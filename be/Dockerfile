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
# DB 와 같은 시간대로 고정한다. 기본값(UTC)이면 앱이 만든 expired_at 과 DB NOW() 가 9시간
# 어긋나 승인요청이 생성 즉시 만료로 취급된다.
ENV TZ=Asia/Seoul
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/build/libs/pay-with-api.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
