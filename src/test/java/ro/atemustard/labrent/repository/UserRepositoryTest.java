package ro.atemustard.labrent.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ro.atemustard.labrent.model.Role;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    @Test
    void findByUsername_returnsMatchingUser() {
        userRepository.save(new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT));
        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.findByUsername("bob")).isEmpty();
    }

    @Test
    void existsByEmail_caseSensitive() {
        userRepository.save(new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT));
        assertThat(userRepository.existsByEmail("a@x.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@x.com")).isFalse();
    }

    @Test
    void findByUserType_returnsOnlyMatching() {
        userRepository.save(new User("a", "a@x.com", "p", Role.USER, UserType.STUDENT));
        userRepository.save(new User("b", "b@x.com", "p", Role.USER, UserType.NON_STUDENT));
        userRepository.save(new User("c", "c@x.com", "p", Role.ADMIN, UserType.NON_STUDENT));

        assertThat(userRepository.findByUserType(UserType.STUDENT)).hasSize(1);
        assertThat(userRepository.findByRole(Role.ADMIN)).hasSize(1);
    }
}
