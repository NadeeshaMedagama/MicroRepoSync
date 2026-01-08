# Multi-stage build for Document Processor Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom and common-lib first
COPY pom.xml .
COPY common-lib ./common-lib

# Copy document-processor-service
COPY document-processor-service ./document-processor-service

# Build the application
RUN mvn clean package -pl document-processor-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/document-processor-service/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

