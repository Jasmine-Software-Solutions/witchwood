# ---------------------------------------------------------------------------
# Runtime image for the Witchwood gRPC server.
#
# Build the shadow JAR on the host first:
#   ./gradlew --no-daemon shadowJar
# Then build this image from the project root:
#   docker build .
# ---------------------------------------------------------------------------
FROM eclipse-temurin:26-jre

WORKDIR /app

# Copy the pre-built fat JAR from the host.
COPY build/libs/witchwood-*-all.jar /app/witchwood.jar

# Copy Flyway database migrations.
COPY src/main/resources/db/migration /app/db/migration

# OpenTelemetry Java agent for distributed tracing and logging.
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.30.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

# Server listens on this port by default; override with --port if needed
EXPOSE 50051

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/witchwood.jar", "serve"]
