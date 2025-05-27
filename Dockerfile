FROM gradle:8.13-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean build --no-daemon -x test

FROM openjdk:21-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]