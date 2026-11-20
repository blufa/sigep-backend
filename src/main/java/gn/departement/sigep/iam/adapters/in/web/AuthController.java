package gn.departement.sigep.iam.adapters.in.web;

import gn.departement.sigep.iam.adapters.in.web.dto.LoginRequest;
import gn.departement.sigep.iam.adapters.in.web.dto.LoginResponse;
import gn.departement.sigep.iam.application.port.in.LoginUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginUseCase.LoginResult result = loginUseCase.login(
                new LoginUseCase.LoginCommand(request.username(), request.password()));

        return new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresInSeconds(),
                result.userId(),
                result.tenantId(),
                result.fullName(),
                result.role(),
                result.allowedMenus()
        );
    }
}
