package gn.departement.sigep.communication.adapters.out.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gn.departement.sigep.communication.application.port.out.SmsGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Gateway SMS via l'API NimbaSMS (fournisseur guinéen), porté depuis lacaeducation
 * ({@code gn.lacaeducation.communication.adapters.out.gateway.NimbaSmsGateway}) — même logique
 * d'authentification/format éprouvée en production, simplifiée pour sigep (pas de solde de compte,
 * pas de file de reprise : les échecs sont journalisés, pas retentés).
 *
 * <p>Flux : Basic auth ({@code base64(service_id:secret_token)}) → POST {@code /v1/messages} en JSON
 * {@code {sender_name, to:[numéro...], message}}. Tant que les clés ne sont pas renseignées
 * ({@code nimba.sms.enabled=false}), l'envoi se replie sur la journalisation sans échouer (comportement
 * dev).</p>
 */
@Component
@Slf4j
public class NimbaSmsGateway implements SmsGateway {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final NimbaSmsProperties props;
    private final RestClient http;

    public NimbaSmsGateway(NimbaSmsProperties props) {
        this.props = props;
        // RestClient.Builder n'est pas auto-configuré dans ce projet (spring-boot-webmvc seul, pas
        // spring-boot-starter-web/restclient-autoconfigure) — on construit directement plutôt que
        // d'injecter un bean qui n'existe pas.
        this.http = RestClient.builder().baseUrl(props.baseUrlOrDefault()).build();
        logConfigurationStatus();
    }

    /** Diagnostic visible au démarrage : permet de vérifier dans les logs de déploiement si les
     * variables NIMBA_SMS_* sont bien arrivées jusqu'au conteneur, sans déclencher d'envoi réel. */
    private void logConfigurationStatus() {
        if (props.isConfigured()) {
            log.info("[SMS] Nimba CONFIGURÉ — sender=\"{}\", base_url={}", props.senderName(), props.baseUrlOrDefault());
            return;
        }
        List<String> missing = new java.util.ArrayList<>();
        if (!props.enabled()) missing.add("NIMBA_SMS_ENABLED=false (ou absent)");
        if (props.serviceId() == null || props.serviceId().isBlank()) missing.add("NIMBA_SMS_SERVICE_ID manquant");
        if (props.secretToken() == null || props.secretToken().isBlank()) missing.add("NIMBA_SMS_SECRET_TOKEN manquant");
        if (props.senderName() == null || props.senderName().isBlank()) missing.add("NIMBA_SMS_SENDER_NAME manquant");
        log.warn("[SMS] Nimba NON CONFIGURÉ au démarrage — les SMS seront seulement journalisés, jamais "
                + "envoyés, tant que ceci n'est pas corrigé : {}", String.join(" ; ", missing));
    }

    @Override
    public DeliveryResult send(String destination, String message) {
        if (destination == null || destination.isBlank()) {
            return new DeliveryResult(false, "aucun destinataire", false);
        }
        if (message == null || message.isBlank()) {
            return new DeliveryResult(false, "message vide", false);
        }
        if (!props.isConfigured()) {
            log.warn("[SMS] Nimba NON CONFIGURÉ — SMS NON envoyé (pas d'appel réseau) vers {} : {}", destination, message);
            return new DeliveryResult(false, "nimba non configuré (clés manquantes) : SMS non envoyé", false);
        }
        try {
            MessageRequest body = new MessageRequest(
                    props.senderName().trim(), List.of(normalizeNumber(destination, props.defaultDialingCode())), message);
            String rawJson = JSON.writeValueAsString(body);
            log.info("[SMS] Nimba -> corps envoyé : {}", rawJson);
            String rawResponse = http.post()
                    .uri("/v1/messages")
                    .header("Authorization", "Basic " + basicToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rawJson)
                    .retrieve()
                    .body(String.class);
            log.info("[SMS] Nimba -> réponse brute : {}", rawResponse);
            MessageResponse response = rawResponse != null && !rawResponse.isBlank()
                    ? JSON.readValue(rawResponse, MessageResponse.class) : null;
            boolean ok = response != null && response.messageId() != null && !response.messageId().isBlank();
            log.info("[SMS] Nimba -> {} : {}", destination, ok ? response.messageId() : "réponse invalide");
            return new DeliveryResult(ok, ok ? "nimba " + response.messageId() : "nimba: réponse invalide");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx (ex. "numéro invalide") : erreur de fond, jamais transitoire.
            log.error("[SMS] échec envoi Nimba vers {} (définitif, {}) : {}", destination, e.getStatusCode(), e.getResponseBodyAsString());
            return new DeliveryResult(false, e.getResponseBodyAsString(), false);
        } catch (RuntimeException e) {
            log.error("[SMS] échec envoi Nimba vers {} : {}", destination, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }

    private String basicToken() {
        return Base64.getEncoder().encodeToString(
                (props.serviceId().trim() + ":" + props.secretToken().trim()).getBytes(StandardCharsets.UTF_8));
    }

    /** Nettoie un numéro pour NimbaSMS et force le format E.164 complet ({@code +224623XXXXXX}) —
     * voir l'équivalent lacaeducation : un SMS sans {@code +} a déjà été accepté mais jamais livré. */
    static String normalizeNumber(String raw, String defaultDialingCode) {
        if (raw == null) return null;
        String digits = raw.trim().replaceAll("\\D", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (!digits.startsWith(defaultDialingCode)) digits = defaultDialingCode + digits;
        return "+" + digits;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageRequest(
            @JsonProperty("sender_name") String senderName,
            List<String> to,
            String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageResponse(
            @JsonProperty("messageid") String messageId,
            @JsonProperty("message_cost") Integer messageCost,
            String url) {}
}
