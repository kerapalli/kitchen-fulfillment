# syntax=docker/dockerfile:1
FROM gradle:8.4-jdk21
WORKDIR /app

COPY settings.gradle build.gradle ./
COPY gradlew ./
COPY gradle ./gradle
COPY src ./src

# Run tests before setting the entrypoint
RUN ./gradlew test --no-daemon

ENTRYPOINT ["/usr/bin/gradle", "run"]