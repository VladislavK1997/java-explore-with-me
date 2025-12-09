FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/stats-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
RUN apk add --no-cache curl
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh
ENTRYPOINT ["java", "-jar", "app.jar"]