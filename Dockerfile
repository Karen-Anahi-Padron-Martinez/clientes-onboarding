# ==========================================
# Etapa 1: Compilación de la aplicación
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copiar archivos de configuración de Gradle y Wrapper
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Convertir saltos de línea Windows (CRLF a LF) y dar permisos de ejecución
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copiar el código fuente
COPY src src

# Compilar el archivo .jar excluyendo pruebas automáticas durante el build de Render
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# Etapa 2: Imagen final ligera para ejecución
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiar el archivo .jar compilado desde la etapa anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Puerto por defecto (Render asigna dinámicamente el puerto con la variable PORT)
ENV PORT=8081
EXPOSE 8081

# Ejecutar el jar inyectando el puerto de Render
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
