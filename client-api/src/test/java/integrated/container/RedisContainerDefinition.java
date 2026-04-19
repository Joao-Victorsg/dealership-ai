package integrated.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static integrated.EnvironmentInitializer.NETWORK;

public class RedisContainerDefinition {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    public static void startRedisContainer() {
        REDIS_CONTAINER.start();
    }

    public static String getRedisHost() {
        return REDIS_CONTAINER.getHost();
    }

    public static int getRedisPort() {
        return REDIS_CONTAINER.getFirstMappedPort();
    }
}
