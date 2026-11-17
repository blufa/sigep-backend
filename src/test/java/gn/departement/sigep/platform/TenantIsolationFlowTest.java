package gn.departement.sigep.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie qu'un service (tenant) nouvellement créé par le SUPER_ADMIN ne voit jamais les
 * utilisateurs d'un autre service — c'est la garantie centrale du SaaS multi-service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class TenantIsolationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void newTenantAdminOnlySeesItsOwnTenantUsers() throws Exception {
        String superAdminToken = login("superadmin", "password");

        Long tenantId = createTenant(superAdminToken, "DNFP", "Direction Nationale Formation Professionnelle");
        createTenantAdmin(superAdminToken, tenantId, "dnfp-admin", "password123");

        String dnfpToken = login("dnfp-admin", "password123");
        assertThat(usernamesSeenBy(dnfpToken, null)).containsExactly("dnfp-admin");

        String snepAdminToken = login("admin", "password");
        assertThat(usernamesSeenBy(snepAdminToken, null)).doesNotContain("dnfp-admin");

        // SUPER_ADMIN "opérant en tant que" le service DNFP (X-Tenant-Id) doit voir exactement les
        // mêmes données que l'administrateur réel de ce service, ni plus ni moins.
        assertThat(usernamesSeenBy(superAdminToken, tenantId)).containsExactly("dnfp-admin");
    }

    private java.util.List<String> usernamesSeenBy(String token, Long impersonatedTenantId) throws Exception {
        var request = get("/api/users").header("Authorization", "Bearer " + token);
        if (impersonatedTenantId != null) {
            request = request.header("X-Tenant-Id", String.valueOf(impersonatedTenantId));
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("items").findValuesAsText("username");
    }

    private Long createTenant(String superAdminToken, String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/super-admin/tenants")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "code", code, "ministryName", "MENET-FP", "serviceName", code))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void createTenantAdmin(String superAdminToken, Long tenantId, String username, String password) throws Exception {
        mockMvc.perform(post("/api/super-admin/tenants/" + tenantId + "/users")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "fullName", "Directeur", "password", password, "role", "ADMIN"))))
                .andExpect(status().isOk());
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
