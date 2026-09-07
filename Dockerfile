# Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and fetch dependencies first to leverage Docker caching layers
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build jar package
COPY src ./src
RUN mvn package -DskipTests -B

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy packaged jar from build container
COPY --from=build /app/target/costmonitor-0.0.1-SNAPSHOT.jar app.jar

# Expose server port
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
