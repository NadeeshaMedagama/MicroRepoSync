FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy all modules (required for Maven reactor build)
COPY common-lib ./common-lib
COPY github-service ./github-service
COPY document-processor-service ./document-processor-service
COPY embedding-service ./embedding-service
COPY milvus-service ./milvus-service
COPY orchestrator-service ./orchestrator-service
COPY monitoring-service ./monitoring-service

# Build with checkstyle skip
RUN mvn clean package -pl monitoring-service -am -DskipTests -Dcheckstyle.skip=true

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/monitoring-service/target/monitoring-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]

