# Multi-stage build for Resource Projection
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml /app/

# Copy all modules (Maven parent POM requires all modules to be present)
COPY common/pom.xml /app/common/
COPY common/src /app/common/src
COPY edge/pom.xml /app/edge/
COPY edge/src /app/edge/src
COPY operational-projection/pom.xml /app/operational-projection/
COPY operational-projection/src /app/operational-projection/src
COPY resource-projection/pom.xml /app/resource-projection/
COPY resource-projection/src /app/resource-projection/src
COPY workforce-projection/pom.xml /app/workforce-projection/
COPY workforce-projection/src /app/workforce-projection/src

# Build the project
RUN mvn clean package -DskipTests -pl resource-projection -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/resource-projection/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
