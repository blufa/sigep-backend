package gn.departement.sigep.iam.adapters.in.web.dto;

import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;

import java.util.List;

public record UserResponse(
        Long id, String username, String fullName, Role role, boolean active, List<String> allowedMenus) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(), user.username(), user.fullName(), user.role(), user.active(), user.allowedMenus());
    }
}
