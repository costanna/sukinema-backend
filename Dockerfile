FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Las dependencias se descargan en una capa aparte para que se reutilice entre despliegues
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S sukinema && adduser -S sukinema -G sukinema
COPY --from=build /app/target/sukinema-backend-*.jar app.jar
USER sukinema

ENV SPRING_PROFILES_ACTIVE=prod
# El plan gratuito de Render da 512 MB: la JVM se limita a una parte de la memoria del contenedor
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

# Render indica el puerto en la variable PORT (10000 por defecto)
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]
