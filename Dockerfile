# Etapa 1: Construcción
FROM maven:3.9.9-eclipse-temurin-22 AS buildstage
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:22-jdk
WORKDIR /app
COPY --from=buildstage /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
