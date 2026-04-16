package integrated.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static integrated.EnvironmentInitializer.NETWORK;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public class LocalStackContainerDefinition {

    private static final String LOCALSTACK_URL = "http://%s:%d";

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("localstack/localstack:4.4");

    private static final GenericContainer<?> LOCALSTACK_CONTAINER = new LocalStackContainer(DOCKER_IMAGE_NAME)
            .withExposedPorts(4566)
            .withServices(S3)
            .withNetwork(NETWORK);

    public static void startLocalStackContainer() {
        LOCALSTACK_CONTAINER.start();
    }

    public static String getLocalStackUrl() {
        return String.format(LOCALSTACK_URL,
                LOCALSTACK_CONTAINER.getHost(), LOCALSTACK_CONTAINER.getFirstMappedPort());
    }

    public static String getRegion() {
        return "us-east-1";
    }
}
