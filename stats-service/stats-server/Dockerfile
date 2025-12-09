FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/stats-server.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]