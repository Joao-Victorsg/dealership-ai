# ---- Build Stage --------------------------------------------------------
FROM amazoncorretto:25-alpine AS builder

WORKDIR /app

# Copy wrapper and POM separately so dependency layers are cached
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline --no-transfer-progress

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests --no-transfer-progress

# ---- New Relic Agent Stage ----------------------------------------------
FROM alpine:3.21 AS newrelic

RUN apk add --no-cache curl unzip

WORKDIR /newrelic

RUN curl -fsSL -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip \
    && unzip -j newrelic-java.zip "newrelic/newrelic.jar" -d . \
    && rm newrelic-java.zip

# ---- Runtime Stage ------------------------------------------------------
FROM amazoncorretto:25-alpine

# Run as non-root for security
RUN addgroup -S appgroup && adduser -S -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/target/sales-api-*.jar app.jar

# New Relic agent
RUN mkdir -p /usr/local/newrelic
COPY --from=newrelic /newrelic/newrelic.jar /usr/local/newrelic/newrelic.jar
COPY newrelic/newrelic.yml /usr/local/newrelic/newrelic.yml

RUN chown -R appuser:appgroup /app /usr/local/newrelic

USER appuser

EXPOSE 8080

# JVM flags: honour container CPU/memory limits + New Relic agent
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-javaagent:/usr/local/newrelic/newrelic.jar", \
  "-jar", "app.jar"]
