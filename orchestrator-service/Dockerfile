# Multi-stage build for Orchestrator Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom
COPY pom.xml .

# Copy all modules (required for Maven reactor build)
COPY common-lib ./common-lib
COPY github-service ./github-service
COPY document-processor-service ./document-processor-service
COPY embedding-service ./embedding-service
COPY milvus-service ./milvus-service
COPY orchestrator-service ./orchestrator-service
COPY monitoring-service ./monitoring-service

# Build the application
RUN mvn clean package -pl orchestrator-service -am -DskipTests -Dcheckstyle.skip=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Copy the built jar
COPY --from=build /app/orchestrator-service/target/*.jar app.jar

# Expose port
EXPOSE 8086

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

