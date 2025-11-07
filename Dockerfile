# Build mais enxuto utilizando cache Maven
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:MinRAMPercentage=20 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:+UseStringDeduplication -Xss256k -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=postgres
WORKDIR /app
COPY --from=builder /workspace/target/Gomech-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
