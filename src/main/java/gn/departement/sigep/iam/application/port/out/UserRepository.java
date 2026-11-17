package gn.departement.sigep.iam.application.port.out;

import gn.departement.sigep.iam.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    boolean existsByUsername(String username);

    Page<User> findAll(Pageable pageable);
}
