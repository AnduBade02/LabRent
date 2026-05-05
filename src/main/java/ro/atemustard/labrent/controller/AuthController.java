package ro.atemustard.labrent.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.dto.*;
import ro.atemustard.labrent.service.UserService;
import ro.atemustard.labrent.util.JwtTokenProvider;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        UserDTO user = userService.register(dto);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponseDTO(token, user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        UserDTO user = userService.getUserByUsername(dto.getUsername());
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponseDTO(token, user));
    }
}
