package gn.departement.sigep.communication.application.port.out;

/** Port sortant vers le fournisseur SMS (NimbaSMS par défaut, voir
 * {@code gn.departement.sigep.communication.adapters.out.gateway.NimbaSmsGateway}). */
public interface SmsGateway {

    /**
     * @param retryable Faux pour un échec DÉFINITIF (ex. numéro invalide) qui ne deviendra jamais un
     *                  succès en réessayant — distinct d'une vraie panne transitoire (réseau, 5xx).
     */
    record DeliveryResult(boolean delivered, String detail, boolean retryable) {
        public DeliveryResult(boolean delivered, String detail) {
            this(delivered, detail, true);
        }
    }

    DeliveryResult send(String phoneNumber, String message);
}
