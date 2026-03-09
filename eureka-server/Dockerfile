# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY eureka-server/src eureka-server/src
COPY shared shared
RUN mvn -B -pl eureka-server -am install -DskipTests

# Stage 2: Runtime
FROM gcr.io/distroless/java17-debian12:latest
WORKDIR /app
COPY --from=build /app/eureka-server/target/*.jar app.jar
EXPOSE 8761
USER nonroot:nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
