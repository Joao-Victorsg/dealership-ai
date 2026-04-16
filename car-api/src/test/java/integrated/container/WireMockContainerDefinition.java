package integrated.container;

import integrated.utils.JwtTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import static integrated.EnvironmentInitializer.NETWORK;

public class WireMockContainerDefinition {

    private static final String WIREMOCK_URL = "http://%s:%d";

    private static final DockerImageName WIREMOCK_IMAGE = DockerImageName.parse("wiremock/wiremock:3.13.0");

    private static final GenericContainer<?> WIREMOCK_CONTAINER = new GenericContainer<>(WIREMOCK_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .withCopyToContainer(
                    Transferable.of(JwtTestUtils.getWireMockMappingJson()),
                    "/home/wiremock/mappings/jwks-mapping.json"
            )
            .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

    public static void startWireMockContainer() {
        WIREMOCK_CONTAINER.start();
    }

    public static String getWireMockUrl() {
        return String.format(WIREMOCK_URL,
                WIREMOCK_CONTAINER.getHost(), WIREMOCK_CONTAINER.getFirstMappedPort());
    }

    public static String getJwksUri() {
        return getWireMockUrl() + "/.well-known/jwks.json";
    }
}
