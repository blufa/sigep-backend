package gn.departement.sigep.iam.application.port.out;

import gn.departement.sigep.iam.domain.User;

public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String accessToken, long expiresInSeconds) {
    }
}
