FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="your-email@example.com"
LABEL version="1.0"
LABEL description="Stats Service for Explore With Me"

# Установка переменных окружения
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"
ENV TZ=Europe/Moscow

# Установка временной зоны
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Europe/Moscow /etc/localtime && \
    echo "Europe/Moscow" > /etc/timezone

# Создание не-root пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Рабочая директория
WORKDIR /app

# Копирование JAR файла
# Внимание: путь к JAR зависит от структуры Maven!
COPY target/stats-server-0.0.1-SNAPSHOT.jar app.jar

# Открытие порта
EXPOSE 9090

# Запуск приложения с health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]