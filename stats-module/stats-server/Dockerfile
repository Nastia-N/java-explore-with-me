FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем JAR файл (он должен быть уже собран)
COPY target/stats-server-*.jar app.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]