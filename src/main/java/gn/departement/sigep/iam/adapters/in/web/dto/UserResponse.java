package gn.departement.sigep.iam.adapters.in.web.dto;

import gn.departement.sigep.iam.domain.Role;
import gn.departement.sigep.iam.domain.User;

public record UserResponse(Long id, String username, String fullName, Role role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.fullName(), user.role(), user.active());
    }
}
