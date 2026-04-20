package ro.atemustard.labrent.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.atemustard.labrent.dto.RegisterRequestDTO;
import ro.atemustard.labrent.dto.UserDTO;
import ro.atemustard.labrent.exception.DuplicateResourceException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.Role;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;
import ro.atemustard.labrent.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + dto.getUsername());
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }

        UserType userType = UserType.valueOf(dto.getUserType().toUpperCase());

        User user = new User(
                dto.getUsername(),
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                Role.USER,
                userType
        );

        User saved = userRepository.save(user);
        return UserDTO.fromEntity(saved);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserDTO.fromEntity(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserDTO.fromEntity(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public void updateReputationScore(Long userId, double delta) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        double newScore = Math.max(0.0, Math.min(200.0, user.getReputationScore() + delta));
        user.setReputationScore(newScore);
        userRepository.save(user);
    }

    public User findEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
