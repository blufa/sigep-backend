package gn.departement.sigep.iam.adapters.out.persistence;

import gn.departement.sigep.iam.application.port.out.UserRepository;
import gn.departement.sigep.iam.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = user.id() != null
                ? jpaRepository.findById(user.id()).orElseGet(UserJpaEntity::new)
                : new UserJpaEntity();
        entity.setTenantId(user.tenantId());
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setFullName(user.fullName());
        entity.setRole(user.role());
        entity.setActive(user.active());
        entity.setAllowedMenus(joinMenus(user.allowedMenus()));
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getTenantId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getRole(),
                Boolean.TRUE.equals(entity.getActive()),
                splitMenus(entity.getAllowedMenus())
        );
    }

    private String joinMenus(List<String> menus) {
        return menus == null ? null : String.join(",", menus);
    }

    private List<String> splitMenus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return List.of(raw.split(","));
    }
}
