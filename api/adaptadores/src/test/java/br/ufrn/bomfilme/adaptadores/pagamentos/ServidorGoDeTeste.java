package br.ufrn.bomfilme.adaptadores.pagamentos;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class ServidorGoDeTeste implements QuarkusTestResourceLifecycleManager {

    private static final int PORTA_GRPC = 50051;

    private GenericContainer<?> container;
    private String cenario;

    @Override
    public void init(Map<String, String> args) {
        cenario = args.getOrDefault("cenario", "aprovacao");
    }

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(DockerImageName.parse("bomfilme/pagamentos:teste"))
                .withExposedPorts(PORTA_GRPC)
                .withEnv("BOMFILME_PAGAMENTOS_CENARIO", cenario);
        container.start();

        return Map.of(
                "quarkus.grpc.clients.pagamentos.host", container.getHost(),
                "quarkus.grpc.clients.pagamentos.test-port",
                container.getMappedPort(PORTA_GRPC).toString());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}