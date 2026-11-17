package gn.departement.sigep.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Violation d'une règle métier (ex: badge révoqué, employé inactif).
 * Mappée en réponse HTTP structurée par {@link GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessRuleException(String code, String message) {
        this(code, message, HttpStatus.CONFLICT);
    }

    public BusinessRuleException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
