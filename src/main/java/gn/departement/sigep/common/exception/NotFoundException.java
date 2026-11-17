package gn.departement.sigep.common.exception;

/** Ressource introuvable (ou hors du périmètre du tenant courant). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException(resource + " introuvable : " + id);
    }
}
