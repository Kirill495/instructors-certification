# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY publication-contract/pom.xml publication-contract/
COPY instructors-app/pom.xml instructors-app/
COPY publication-service/pom.xml publication-service/
COPY checkstyle.xml .
COPY suppressions.xml .

COPY publication-contract/src publication-contract/src
COPY instructors-app/src instructors-app/src

RUN --mount=type=cache,target=/root/.m2/repository MAVEN_OPTS="-Xmx512m" \
    mvn -pl instructors-app -am package -DskipTests  \
    -Dcheckstyle.skip=true -Dspotless.check.skip=true

# Stage 2: run
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
COPY --from=build /app/instructors-app/target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]