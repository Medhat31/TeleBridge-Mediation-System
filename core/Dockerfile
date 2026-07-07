# ==========================================
# TeleBridge Mediation Engine - Dockerfile
# Multi-stage build: Maven build -> JRE runtime
# ==========================================

# Stage 1: Build the fat JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image with Docker CLI
FROM eclipse-temurin:21-jre
RUN apt-get update && \
    apt-get install -y --no-install-recommends docker.io && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/core-1.0-SNAPSHOT.jar /app/core.jar

CMD ["java", "-jar", "/app/core.jar"]
