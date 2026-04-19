package integrated.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static integrated.EnvironmentInitializer.NETWORK;

public class PostgresContainerDefinition {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    private static final String POSTGRES_URL = "jdbc:postgresql://%s:%d/dealershipdb";

    private static final GenericContainer<?> POSTGRES_CONTAINER = new GenericContainer<>(POSTGRES_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres")
            .withEnv("POSTGRES_USER", "dealership")
            .withEnv("POSTGRES_PASSWORD", "12345678")
            .withEnv("POSTGRES_DB", "dealershipdb")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    public static void startPostgresContainer() {
        POSTGRES_CONTAINER.start();
    }

    public static String getPostgresUrl() {
        return String.format(POSTGRES_URL,
                POSTGRES_CONTAINER.getHost(), POSTGRES_CONTAINER.getFirstMappedPort());
    }

    public static String getPostgresUsername() {
        return "dealership";
    }

    public static String getPostgresPassword() {
        return "12345678";
    }
}
