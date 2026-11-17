package gn.departement.sigep.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.common.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie l'alternance automatique ENTREE/SORTIE au scan du badge, sur une
 * base Postgres jetable (Testcontainers) avec les migrations Flyway réelles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class AttendanceScanFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BadgeUseCases badgeUseCases;

    @Test
    void firstScanIsEntreeSecondScanIsSortie() throws Exception {
        String adminToken = login("admin", "password");

        Long positionId = createPosition(adminToken, "Agent de maintenance", "AGT-MAINT");
        Long employeeId = createEmployee(adminToken, positionId);
        String qrToken = issueBadge(employeeId);

        scan(adminToken, qrToken, "ENTREE");
        scan(adminToken, qrToken, "SORTIE");
        scan(adminToken, qrToken, "ENTREE");
    }

    private void scan(String token, String qrToken, String expectedDirection) throws Exception {
        mockMvc.perform(post("/api/attendance/scan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("qrToken", qrToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direction").value(expectedDirection));
    }

    private String issueBadge(Long employeeId) {
        // Hors d'une requête HTTP, le contexte tenant doit être positionné manuellement
        // (en conditions réelles, TenantContextFilter le fait à partir du JWT).
        TenantContext.set(1L);
        try {
            return badgeUseCases.getOrIssue(employeeId).qrToken();
        } finally {
            TenantContext.clear();
        }
    }

    private Long createPosition(String token, String title, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "code", code, "active", true))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createEmployee(String token, Long positionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "matricule", "ETAT-00123",
                                "lastName", "Camara",
                                "firstName", "Mamadou",
                                "hierarchie", "Agent de maîtrise",
                                "genre", "HOMME",
                                "positionId", positionId,
                                "phone", "620000000",
                                "active", true))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
