package gn.departement.sigep.iam.adapters.in.security;

import gn.departement.sigep.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Place le {@code tenant_id} porté par le JWT dans {@link TenantContext} pour la
 * durée de la requête. Doit s'exécuter après l'authentification du Bearer token
 * (le principal JWT est déjà résolu dans le SecurityContext à ce stade).
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Long tenantId = jwtAuth.getToken().getClaim("tenant_id");
                if (tenantId != null) {
                    TenantContext.set(tenantId);
                } else if (isSuperAdmin(authentication)) {
                    // Un SUPER_ADMIN n'a pas de tenant_id dans son JWT : il peut "opérer en tant que"
                    // un service en envoyant son id via X-Tenant-Id (choisi côté front, voir
                    // lib/auth/impersonation.ts) — scope à SUPER_ADMIN uniquement, déjà un rôle
                    // totalement privilégié.
                    String header = request.getHeader("X-Tenant-Id");
                    if (header != null && !header.isBlank()) {
                        try {
                            TenantContext.set(Long.parseLong(header.trim()));
                        } catch (NumberFormatException ignored) {
                            // en-tête invalide → comportement plateforme (sans tenant)
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
