FROM maven:3.9-eclipse-temurin-21 AS build

# Build common module first
WORKDIR /common
COPY common/pom.xml ./pom.xml
COPY common/src ./src
RUN mvn clean install -DskipTests

# Build saga-orchestrator
WORKDIR /app
COPY saga-orchestrator/pom.xml ./pom.xml
COPY saga-orchestrator/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

