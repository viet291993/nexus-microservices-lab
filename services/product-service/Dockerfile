# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY services/product-service/pom.xml services/product-service/
COPY services/product-service/src services/product-service/src
COPY shared shared
RUN mvn -B -pl services/product-service -am install -DskipTests

# Stage 2: Runtime
FROM gcr.io/distroless/java17-debian12:latest
WORKDIR /app
COPY --from=build /app/services/product-service/target/*.jar app.jar
EXPOSE 8082
USER nonroot:nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
