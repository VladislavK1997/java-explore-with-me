FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/stats-server-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]