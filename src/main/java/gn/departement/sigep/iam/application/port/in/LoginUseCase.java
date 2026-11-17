package gn.departement.sigep.iam.application.port.in;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);

    record LoginCommand(String username, String password) {
    }

    record LoginResult(
            String accessToken,
            long expiresInSeconds,
            Long userId,
            Long tenantId,
            String fullName,
            String role
    ) {
    }
}
