FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/service-alpha-0.0.1-SNAPSHOT.jar ./application.jar
ENV HOME=/tmp
USER 10001:10001
EXPOSE 8090
ENTRYPOINT ["java", "-Duser.home=/tmp", "-jar", "/app/application.jar"]
