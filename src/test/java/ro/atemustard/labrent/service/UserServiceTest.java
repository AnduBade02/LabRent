package ro.atemustard.labrent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.atemustard.labrent.dto.RegisterRequestDTO;
import ro.atemustard.labrent.dto.UserDTO;
import ro.atemustard.labrent.exception.DuplicateResourceException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.Role;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;
import ro.atemustard.labrent.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private RegisterRequestDTO registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new RegisterRequestDTO();
        registerDto.setUsername("alice");
        registerDto.setEmail("alice@x.com");
        registerDto.setPassword("secret123");
        registerDto.setUserType("STUDENT");
    }

    @Test
    void register_savesUserWithEncodedPasswordAndDefaultReputation() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDTO dto = userService.register(registerDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPassword()).isEqualTo("ENC");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(saved.getReputationScore()).isEqualTo(100.0);
        assertThat(dto.getUsername()).isEqualTo("alice");
    }

    @Test
    void register_acceptsLowercaseUserType() {
        registerDto.setUserType("non_student");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("X");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(registerDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUserType()).isEqualTo(UserType.NON_STUDENT);
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> userService.register(registerDto))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.register(registerDto))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getUserById_missing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReputationScore_clampsBetweenZeroAndTwoHundred() {
        User u = new User("a", "a@x.com", "p", Role.USER, UserType.STUDENT);
        u.setId(1L);
        u.setReputationScore(95.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.updateReputationScore(1L, 200.0);
        assertThat(u.getReputationScore()).isEqualTo(200.0);

        userService.updateReputationScore(1L, -1000.0);
        assertThat(u.getReputationScore()).isEqualTo(0.0);
    }

    @Test
    void updateReputationScore_appliesDeltaWhenWithinBounds() {
        User u = new User("a", "a@x.com", "p", Role.USER, UserType.STUDENT);
        u.setId(1L);
        u.setReputationScore(100.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.updateReputationScore(1L, -15.0);

        assertThat(u.getReputationScore()).isEqualTo(85.0);
        verify(userRepository).save(u);
    }
}
