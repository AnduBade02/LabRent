package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.atemustard.labrent.model.Role;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByUserType(UserType userType);
}
