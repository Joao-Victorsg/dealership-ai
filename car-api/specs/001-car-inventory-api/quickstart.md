# Quickstart: Car Inventory API

**Feature**: 001-car-inventory-api  
**Date**: 2026-04-12

## Prerequisites

- Java 25 (JDK)
- Docker (for Testcontainers and compose services)
- Maven 3.9+ (or use the included `./mvnw` wrapper)

## Local Development

### 1. Start infrastructure via Docker Compose

```bash
docker compose up -d
```

This starts PostgreSQL. For Redis, add the service to `compose.yaml` (see below) or rely on Testcontainers for tests.

### 2. Add Redis to compose.yaml (for local development)

```yaml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - 'POSTGRES_DB=mydatabase'
      - 'POSTGRES_PASSWORD=secret'
      - 'POSTGRES_USER=myuser'
    ports:
      - '5432:5432'
  redis:
    image: 'redis:7-alpine'
    ports:
      - '6379:6379'
```

### 3. Configure application.properties for local development

```properties
spring.application.name=car-api

# Virtual threads
spring.threads.virtual.enabled=true

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.enabled=true

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Security (local dev — use a local Keycloak or mock issuer)
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/dealership

# S3 (local dev — use LocalStack)
aws.s3.bucket-name=car-images
aws.s3.region=us-east-1
aws.s3.endpoint=http://localhost:4566
aws.s3.presigned-url-ttl=PT15M

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Access OpenAPI documentation

Open `http://localhost:8080/swagger-ui.html` in a browser.

## Running Tests

### All tests

```bash
./mvnw verify
```

### Unit tests only

```bash
./mvnw test
```

### Integration tests only (requires Docker)

```bash
./mvnw verify -Dskip.unit.tests=true
```

### With Testcontainers local development runner

```bash
./mvnw spring-boot:test-run
```

This starts the application using `TestCarApiApplication.java` with Testcontainers providing PostgreSQL, Redis, and LocalStack.

## Dependencies to Add

The following dependencies need to be added to `pom.xml`:

```xml
<!-- Redis caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Bean Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- AWS S3 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3-presigner</artifactId>
</dependency>

<!-- Test: Instancio -->
<dependency>
    <groupId>org.instancio</groupId>
    <artifactId>instancio-junit</artifactId>
    <scope>test</scope>
</dependency>

<!-- Test: LocalStack for S3 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-localstack</artifactId>
    <scope>test</scope>
</dependency>

<!-- Test: Security test support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Also add the AWS BOM for version management:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.31.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## API Quick Reference

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/cars` | Staff/Admin | Register car |
| `GET` | `/api/v1/cars` | Public | List/filter/sort cars |
| `GET` | `/api/v1/cars/{id}` | Public | Get car by ID |
| `PATCH` | `/api/v1/cars/{id}` | Staff/Admin | Update mutable fields |
| `POST` | `/api/v1/cars/{id}/image/presigned-url` | Staff/Admin | Get presigned upload URL |

## Example cURL Commands

### Register a car

```bash
curl -X POST http://localhost:8080/api/v1/cars \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{
    "model": "Corolla Cross",
    "manufacturingYear": 2026,
    "manufacturer": "Toyota",
    "externalColor": "Pearl White",
    "internalColor": "Black",
    "vin": "1HGBH41JXMN109186",
    "status": "AVAILABLE",
    "optionalItems": ["Sunroof"],
    "category": "SUV",
    "kilometers": 0,
    "isNew": true,
    "propulsionType": "COMBUSTION",
    "listedValue": 38500.00
  }'
```

### List cars with filters

```bash
curl "http://localhost:8080/api/v1/cars?status=AVAILABLE&category=SUV&sort=listedValue,asc&page=0&size=20"
```

### Update car status

```bash
curl -X PATCH http://localhost:8080/api/v1/cars/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT>" \
  -d '{ "status": "UNAVAILABLE" }'
```

