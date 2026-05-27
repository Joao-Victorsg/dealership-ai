FROM maven:3.9.10-eclipse-temurin-21 AS self-registration-listener-builder

WORKDIR /workspace

COPY keycloak-self-registration-listener/pom.xml ./
COPY keycloak-self-registration-listener/src ./src

RUN mvn -q -DskipTests package

FROM quay.io/keycloak/keycloak:26.2

COPY --from=self-registration-listener-builder \
    /workspace/target/keycloak-self-registration-listener-*.jar \
    /opt/keycloak/providers/
