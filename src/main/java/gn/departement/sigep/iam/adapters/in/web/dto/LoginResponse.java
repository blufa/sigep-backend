package gn.departement.sigep.iam.adapters.in.web.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        Long tenantId,
        String fullName,
        String role
) {
}
