# ==========================================
# TeleBridge Mediation Dashboard - Dockerfile
# Multi-stage build: Maven build -> Tomcat runtime
# ==========================================

# Stage 1: Build the WAR file
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -B

# Stage 2: Runtime image with Tomcat 10.1 (Jakarta EE 10 support)
FROM tomcat:10.1-jdk11-corretto

# Remove default Tomcat apps for security and cleanliness
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the built WAR file to Tomcat's webapps directory as ROOT.war
# so it is served at the root path (/)
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Expose port 8080
EXPOSE 8080

CMD ["catalina.sh", "run"]
