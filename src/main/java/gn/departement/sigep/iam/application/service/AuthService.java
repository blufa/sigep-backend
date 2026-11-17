package gn.departement.sigep.iam.application.service;

import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.iam.application.port.in.LoginUseCase;
import gn.departement.sigep.iam.application.port.out.TokenIssuer;
import gn.departement.sigep.iam.application.port.out.UserRepository;
import gn.departement.sigep.iam.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .filter(u -> passwordEncoder.matches(command.password(), u.passwordHash()))
                .orElseThrow(() -> new BusinessRuleException(
                        "INVALID_CREDENTIALS", "Identifiant ou mot de passe incorrect", HttpStatus.UNAUTHORIZED));

        if (!user.active()) {
            throw new BusinessRuleException("USER_INACTIVE", "Ce compte est désactivé", HttpStatus.FORBIDDEN);
        }

        TokenIssuer.IssuedToken token = tokenIssuer.issue(user);
        return new LoginResult(
                token.accessToken(),
                token.expiresInSeconds(),
                user.id(),
                user.tenantId(),
                user.fullName(),
                user.role().name()
        );
    }
}
