package gn.departement.sigep.communication.application.port.in;

/** Port entrant du module communication, exposé aux modules attendance (cron d'absence) et
 * meetings (convocation des réunions). */
public interface SmsUseCases {

    /** Envoie un SMS — ne lève jamais d'exception (échec journalisé, jamais bloquant pour
     * l'appelant : une panne du fournisseur SMS ne doit pas empêcher le fonctionnement de
     * l'attendance ou des réunions). */
    void sendSms(String phoneNumber, String message);
}
