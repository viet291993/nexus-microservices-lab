FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Cập nhật các gói hệ thống để vá lỗ hổng bảo mật
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
RUN groupadd --system app && useradd --system --gid app --create-home app \
    && chown -R app:app /app
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
