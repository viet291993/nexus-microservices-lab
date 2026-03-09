# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY shared shared
COPY services/order-service/pom.xml services/order-service/
RUN mvn -B -pl services/order-service -am install -DskipTests

# Stage 2: Runtime
FROM gcr.io/distroless/java17-debian12:latest
WORKDIR /app
COPY --from=build /app/services/order-service/target/*.jar app.jar
EXPOSE 8081
USER nonroot:nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
