FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S vaultstream && adduser -S vaultstream -G vaultstream
WORKDIR /app
COPY --from=build --chown=vaultstream:vaultstream /workspace/target/vaultstream-1.0.0.jar /app/app.jar

USER vaultstream
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD wget -q -O - http://127.0.0.1:8081/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
