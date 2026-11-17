package gn.departement.sigep.iam.adapters.out.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import gn.departement.sigep.iam.application.port.out.TokenIssuer;
import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Émet les JWT (HS256, nimbus-jose-jwt) présentés par le front sur chaque appel API. */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final String secret;
    private final long expirationMinutes;

    public JwtTokenIssuer(
            @Value("${sigep.jwt.secret}") String secret,
            @Value("${sigep.jwt.expiration-minutes}") long expirationMinutes) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public IssuedToken issue(User user) {
        long expiresInSeconds = expirationMinutes * 60;
        Instant now = Instant.now();
        try {
            // Un SUPER_ADMIN porte aussi ROLE_ADMIN : il peut ainsi "opérer en tant que" n'importe
            // quel service (cf. TenantContextFilter/X-Tenant-Id) avec les mêmes droits que
            // l'administrateur de ce service, sans qu'aucun endpoint @PreAuthorize("hasRole('ADMIN')")
            // n'ait besoin d'être dupliqué pour lui.
            List<String> roles = user.role() == Role.SUPER_ADMIN
                    ? List.of(Role.SUPER_ADMIN.name(), Role.ADMIN.name())
                    : List.of(user.role().name());
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.id()))
                    .claim("username", user.username())
                    .claim("roles", roles)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expiresInSeconds)));
            // Un SUPER_ADMIN n'a pas de tenant_id : le claim est omis pour que TenantFilterAspect
            // n'active aucun filtre Hibernate et que ses requêtes portent sur tous les tenants.
            if (user.tenantId() != null) {
                claimsBuilder.claim("tenant_id", user.tenantId());
            }
            JWTClaimsSet claims = claimsBuilder.build();

            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(new MACSigner(secret.getBytes()));

            return new IssuedToken(signedJwt.serialize(), expiresInSeconds);
        } catch (JOSEException e) {
            throw new IllegalStateException("Impossible de signer le jeton JWT", e);
        }
    }
}
