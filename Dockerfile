FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src
COPY application.yml ./application.yml
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /opt/app

RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
