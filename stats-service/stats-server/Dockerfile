FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests -pl stats-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/stats-service/stats-server/target/stats-server.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]