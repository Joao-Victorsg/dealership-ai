package integrated.container;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static integrated.EnvironmentInitializer.NETWORK;

public class ViaCepContainerDefinition {

    private static final DockerImageName WIREMOCK_IMAGE = DockerImageName.parse("wiremock/wiremock:3.13.0");

    private static final GenericContainer<?> VIACEP_CONTAINER = new GenericContainer<>(WIREMOCK_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("viacep")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

    private static WireMock wireMockClient;

    public static void startViaCepContainer() {
        VIACEP_CONTAINER.start();
        wireMockClient = new WireMock(VIACEP_CONTAINER.getHost(), VIACEP_CONTAINER.getFirstMappedPort());
    }

    public static String getViaCepBaseUrl() {
        return String.format("http://%s:%d", VIACEP_CONTAINER.getHost(), VIACEP_CONTAINER.getFirstMappedPort());
    }

    public static WireMock getWireMockClient() {
        return wireMockClient;
    }

    public static void resetStubs() {
        wireMockClient.resetMappings();
    }
}
