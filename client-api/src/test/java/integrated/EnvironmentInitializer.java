package integrated;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Network;

import java.util.Map;

import static integrated.container.PostgresContainerDefinition.getPostgresPassword;
import static integrated.container.PostgresContainerDefinition.getPostgresUrl;
import static integrated.container.PostgresContainerDefinition.getPostgresUsername;
import static integrated.container.PostgresContainerDefinition.startPostgresContainer;
import static integrated.container.RedisContainerDefinition.getRedisHost;
import static integrated.container.RedisContainerDefinition.getRedisPort;
import static integrated.container.RedisContainerDefinition.startRedisContainer;
import static integrated.container.ViaCepContainerDefinition.getViaCepBaseUrl;
import static integrated.container.ViaCepContainerDefinition.startViaCepContainer;
import static integrated.container.WireMockContainerDefinition.getJwksUri;
import static integrated.container.WireMockContainerDefinition.getWireMockUrl;
import static integrated.container.WireMockContainerDefinition.startWireMockContainer;
import static integrated.utils.JwtTestUtils.setIssuer;

public class EnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final Network NETWORK = Network.newNetwork();

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        startPostgresContainer();
        startRedisContainer();
        startWireMockContainer();
        startViaCepContainer();

        final var issuer = getWireMockUrl();
        setIssuer(issuer);

        final var properties = Map.of(
                "spring.datasource.url", getPostgresUrl(),
                "spring.datasource.username", getPostgresUsername(),
                "spring.datasource.password", getPostgresPassword(),
                "spring.data.redis.host", getRedisHost(),
                "spring.data.redis.port", String.valueOf(getRedisPort()),
                "spring.security.oauth2.resourceserver.jwt.issuer-uri", issuer,
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri", getJwksUri(),
                "app.viacep.base-url", getViaCepBaseUrl()
        );

        TestPropertyValues.of(properties).applyTo(applicationContext);
    }
}
