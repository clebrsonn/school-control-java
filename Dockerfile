# Etapa de build
FROM gradle:8-jdk-21-and-23-jammy AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Etapa de execução
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]