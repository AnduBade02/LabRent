package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.User;

import java.time.LocalDateTime;

/**
 * Response DTO for User — does not include the password.
 */
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String userType;
    private Double reputationScore;
    private LocalDateTime createdAt;

    public UserDTO() {
    }

    public UserDTO(Long id, String username, String email, String role,
                   String userType, Double reputationScore, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.userType = userType;
        this.reputationScore = reputationScore;
        this.createdAt = createdAt;
    }

    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setUserType(user.getUserType().name());
        dto.setReputationScore(user.getReputationScore());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
