package gn.departement.sigep.iam.adapters.in.web.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        Long tenantId,
        String fullName,
        String role,
        List<String> allowedMenus
) {
}
