# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .

COPY publication-contract/pom.xml publication-contract/
COPY instructors-app/pom.xml instructors-app/
COPY publication-service/pom.xml publication-service/

COPY publication-contract/src publication-contract/src
COPY publication-service/src publication-service/src

RUN --mount=type=cache,target=/root/.m2/repository MAVEN_OPTS="-Xmx512m" \
    mvn -pl publication-service -am package -DskipTest \
    -Dcheckstyle.skip=true -Dspotless.check.skip=true

# Stage 2: run
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY --from=build /app/publication-service/target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]