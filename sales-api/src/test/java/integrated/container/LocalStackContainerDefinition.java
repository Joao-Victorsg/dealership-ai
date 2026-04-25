package integrated.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

import static integrated.EnvironmentInitializer.NETWORK;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

public class LocalStackContainerDefinition {

    private static final String LOCALSTACK_URL = "http://%s:%d";

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("localstack/localstack:4.4");

    private static final GenericContainer<?> LOCALSTACK_CONTAINER = new LocalStackContainer(DOCKER_IMAGE_NAME)
            .withExposedPorts(4566)
            .withServices(SNS, SQS)
            .withNetwork(NETWORK);

    public static void startLocalStackContainer() {
        LOCALSTACK_CONTAINER.start();
        createSnsTopic();
    }

    private static void createSnsTopic() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        try (var snsClient = SnsClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .endpointOverride(URI.create(getLocalStackUrl()))
                .build()) {
            snsClient.createTopic(r -> r.name("sale-events"));
        }
    }

    public static String getLocalStackUrl() {
        return String.format(LOCALSTACK_URL,
                LOCALSTACK_CONTAINER.getHost(), LOCALSTACK_CONTAINER.getFirstMappedPort());
    }

    public static String getRegion() {
        return "us-east-1";
    }
}
