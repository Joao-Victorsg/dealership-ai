# Quickstart: Sales API

**Branch**: `001-sales-api`  
**Last Updated**: 2026-04-19

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 25 | `java -version` |
| Maven | 3.9+ | `./mvnw -version` |
| Docker | 24+ | Required for Testcontainers and local infra |

---

## 1. Add Missing Dependencies to `pom.xml`

The base `pom.xml` requires the following additions. Insert in the `<dependencies>` block:

```xml
<!-- OAuth2 Resource Server (JWT validation) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- AOP (required by Resilience4j annotations) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Spring Cache + Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Resilience4j -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- AWS SDK v2 SNS -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sns</artifactId>
</dependency>

<!-- ─── TEST SCOPE ────────────────────────────────────────────── -->

<!-- AWS SDK v2 SQS (for LocalStack SNS assertion via SQS subscription) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sqs</artifactId>
    <scope>test</scope>
</dependency>

<!-- Instancio (fixture generation) -->
<dependency>
    <groupId>org.instancio</groupId>
    <artifactId>instancio-junit</artifactId>
    <version>5.3.0</version>
    <scope>test</scope>
</dependency>

<!-- Rest Assured (integration test HTTP layer) -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>6.0.0</version>
    <scope>test</scope>
</dependency>

<!-- WireMock Testcontainers (JWKS endpoint) -->
<dependency>
    <groupId>org.wiremock.integrations</groupId>
    <artifactId>wiremock-testcontainers-module</artifactId>
    <version>1.0-alpha-14</version>
    <scope>test</scope>
</dependency>

<!-- LocalStack Testcontainers (SNS) -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <scope>test</scope>
</dependency>

<!-- Nimbus JOSE+JWT (real RSA JWT generation in tests) -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.48</version>
    <scope>test</scope>
</dependency>
```

Add AWS SDK v2 BOM inside `<dependencyManagement>`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.25.60</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Add build plugins:

```xml
<build>
    <plugins>
        <!-- Surefire: exclude integrated/** from unit test run -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>**/integrated/**</exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Failsafe: run integration tests in integrated/** -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <configuration>
                <includes>
                    <include>**/integrated/**IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- JaCoCo: enforce ≥90% instruction + branch coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.13</version>
            <executions>
                <execution>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
                <execution>
                    <id>check</id>
                    <phase>verify</phase>
                    <goals><goal>check</goal></goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>INSTRUCTION</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.90</minimum>
                                    </limit>
                                    <limit>
                                        <counter>BRANCH</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.90</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- PITest: enforce ≥90% mutation score -->
        <plugin>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-maven</artifactId>
            <version>1.17.0</version>
            <dependencies>
                <dependency>
                    <groupId>org.pitest</groupId>
                    <artifactId>pitest-junit5-plugin</artifactId>
                    <version>1.2.1</version>
                </dependency>
            </dependencies>
            <configuration>
                <targetClasses>
                    <param>br.com.dealership.salesapi.*</param>
                </targetClasses>
                <targetTests>
                    <param>br.com.dealership.salesapi.*</param>
                </targetTests>
                <excludedClasses>
                    <param>br.com.dealership.salesapi.integrated.*</param>
                    <param>br.com.dealership.salesapi.SalesApiApplication</param>
                </excludedClasses>
                <mutationThreshold>90</mutationThreshold>
                <coverageThreshold>90</coverageThreshold>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## 2. Configure `application.properties`

```properties
# ─── Application ─────────────────────────────────────────────────────────────
spring.application.name=sales-api

# ─── Security: OAuth2 Resource Server ────────────────────────────────────────
spring.security.oauth2.resourceserver.jwt.jwks-uri=\
  ${KEYCLOAK_JWKS_URI:http://localhost:8080/realms/dealership/protocol/openid-connect/certs}
spring.security.oauth2.resourceserver.jwt.audiences=dealership

# ─── Database ────────────────────────────────────────────────────────────────
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/sales_db}
spring.datasource.username=${DATABASE_USERNAME:sales}
spring.datasource.password=${DATABASE_PASSWORD:sales}
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# ─── Flyway ──────────────────────────────────────────────────────────────────
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# ─── Redis ───────────────────────────────────────────────────────────────────
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# ─── Jackson ─────────────────────────────────────────────────────────────────
spring.jackson.default-property-inclusion=non_null

# ─── Pagination defaults ─────────────────────────────────────────────────────
spring.data.web.pageable.default-page-size=20
spring.data.web.pageable.max-page-size=100

# ─── Actuator ────────────────────────────────────────────────────────────────
management.endpoints.web.exposure.include=health,info
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=when-authorized

# ─── Virtual threads ─────────────────────────────────────────────────────────
spring.threads.virtual.enabled=true

# ─── SNS ─────────────────────────────────────────────────────────────────────
app.sns.topic-arn=${SNS_TOPIC_ARN:arn:aws:sns:us-east-1:000000000000:sale-events}
app.sns.region=${SNS_REGION:us-east-1}
app.sns.endpoint-override=${SNS_ENDPOINT_OVERRIDE:}

# ─── Resilience4j ────────────────────────────────────────────────────────────
resilience4j.retry.retry-aspect-order=2
resilience4j.retry.instances.sns.max-attempts=3
resilience4j.retry.instances.sns.wait-duration=500ms
resilience4j.retry.instances.sns.enable-exponential-backoff=true
resilience4j.retry.instances.sns.exponential-backoff-multiplier=2
resilience4j.retry.instances.sns.retry-exceptions=software.amazon.awssdk.services.sns.model.SnsException

resilience4j.circuitbreaker.circuit-breaker-aspect-order=3
resilience4j.circuitbreaker.instances.sns.sliding-window-size=10
resilience4j.circuitbreaker.instances.sns.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.sns.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.sns.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.sns.register-health-indicator=true
```

---

## 3. Local Infrastructure (Docker Compose)

The existing `compose.yaml` should include:

```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: sales_db
      POSTGRES_USER: sales
      POSTGRES_PASSWORD: sales
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  localstack:
    image: localstack/localstack:4.4
    environment:
      SERVICES: sns,sqs
      DEFAULT_REGION: us-east-1
    ports:
      - "4566:4566"
```

---

## 4. Create SNS Topic (LocalStack — local dev only)

```bash
aws --endpoint-url=http://localhost:4566 \
    sns create-topic \
    --name sale-events \
    --region us-east-1
```

---

## 5. Run the Application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## 6. API Documentation

OpenAPI UI (Swagger): `http://localhost:8080/swagger-ui.html`  
OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

---

## 7. Run Tests

```bash
# Unit tests only
./mvnw test

# Integration tests only (requires Docker)
./mvnw failsafe:integration-test failsafe:verify

# Full build with all tests and coverage check
./mvnw verify

# Mutation testing (PITest)
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

---

## 8. Environment Variables Reference

| Variable | Default (local) | Description |
|----------|-----------------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/sales_db` | JDBC URL |
| `DATABASE_USERNAME` | `sales` | DB username |
| `DATABASE_PASSWORD` | `sales` | DB password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `KEYCLOAK_JWKS_URI` | `http://localhost:8080/realms/dealership/...` | JWKS endpoint |
| `SNS_TOPIC_ARN` | `arn:aws:sns:us-east-1:000000000000:sale-events` | SNS topic ARN |
| `SNS_REGION` | `us-east-1` | AWS region |
| `SNS_ENDPOINT_OVERRIDE` | *(empty — use real AWS)* | LocalStack endpoint for local/test |

---

## 9. New Relic Agent (Production / Staging)

The New Relic Java agent is attached via JVM arguments. No SDK calls appear in
application code. Configure in the JVM startup command:

```bash
java -javaagent:/path/to/newrelic.jar \
     -Dnewrelic.config.file=/path/to/newrelic.yml \
     -jar sales-api.jar
```

The `newrelic.yml` file configures app name, license key, and distributed tracing.
All instrumentation is transparent.
