package ro.atemustard.labrent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entitatea User — reprezintă un utilizator al sistemului LabRent.
 *
 * Câmpuri cheie:
 * - role: USER sau ADMIN (controlează accesul)
 * - userType: STUDENT sau NON_STUDENT (influențează prioritizarea cererilor)
 * - reputationScore: scor de reputație (afectat de calitatea returnărilor, folosit în prioritizare)
 *
 * Tabelul se numește "users" deoarece "user" e cuvânt rezervat în SQL.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    @Column(nullable = false)
    private Double reputationScore = 100.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public User() {
    }

    public User(String username, String email, String password, Role role, UserType userType) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.userType = userType;
        this.reputationScore = 100.0;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public Double getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(Double reputationScore) {
        this.reputationScore = reputationScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
