# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Copy all module poms
COPY shared shared
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY config-server/pom.xml config-server/
COPY services/order-service/pom.xml services/order-service/
COPY services/product-service/pom.xml services/product-service/

COPY eureka-server/src eureka-server/src
RUN mvn -B -pl eureka-server -am install -DskipTests

# Stage 2: Runtime
FROM gcr.io/distroless/java17-debian12:latest
WORKDIR /app
COPY --from=build /app/eureka-server/target/*.jar app.jar
EXPOSE 8761
USER nonroot:nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
