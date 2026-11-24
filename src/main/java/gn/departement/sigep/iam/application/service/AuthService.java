package gn.departement.sigep.iam.application.service;

import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.iam.application.port.in.LoginUseCase;
import gn.departement.sigep.iam.application.port.out.TokenIssuer;
import gn.departement.sigep.iam.application.port.out.UserRepository;
import gn.departement.sigep.iam.domain.User;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            EmployeeRepository employeeRepository,
            PositionRepository positionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
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
                user.role().name(),
                user.allowedMenus(),
                resolvePositionTitle(user)
        );
    }

    /** Poste réel de l'employé lié au compte — affiché à la connexion à la place du rôle
     * administratif attribué à la création du compte (ex. "Directeur Général" plutôt
     * qu'"Administrateur"), pour un compte créé depuis la fiche employé. */
    private String resolvePositionTitle(User user) {
        if (user.employeeId() == null) {
            return null;
        }
        return employeeRepository.findById(user.employeeId())
                .map(Employee::positionId)
                .flatMap(positionRepository::findById)
                .map(Position::title)
                .orElse(null);
    }
}
