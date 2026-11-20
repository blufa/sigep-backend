package gn.departement.sigep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test : le contexte Spring démarre sans erreur. Comme les autres tests d'intégration
 * (voir AttendanceScanFlowTest, TenantIsolationFlowTest), utilise Testcontainers plutôt que la
 * base locale par défaut (jdbc:postgresql://localhost:5432/sigep de application.yaml) — sans ça
 * ce test ne peut passer que sur un poste où cette base existe déjà, jamais en CI (observé :
 * échec sur GitHub Actions, aucun Postgres sur le runner).
 */
@SpringBootTest
@Testcontainers
class SigepApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void contextLoads() {
	}

}
