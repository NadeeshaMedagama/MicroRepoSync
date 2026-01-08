# Multi-stage build for Embedding Service
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom and common-lib first
COPY pom.xml .
COPY common-lib ./common-lib

# Copy embedding-service
COPY embedding-service ./embedding-service

# Build the application
RUN mvn clean package -pl embedding-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/embedding-service/target/*.jar app.jar

# Expose port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

