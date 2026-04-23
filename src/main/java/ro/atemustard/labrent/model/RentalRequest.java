package ro.atemustard.labrent.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RentalRequest entity — a rental request.
 *
 * Links a User to an Equipment. Includes fields for:
 * - Academic urgency (isForExam, examDate, justification) — students only
 * - Priority score (computed by PrioritizationService)
 * - Actual return date (returnedAt) — used for overdue penalty calculation
 *
 * Design Pattern: Builder — inner Builder class for fluent construction,
 * since the entity has many optional fields.
 */
@Entity
@Table(name = "rental_requests")
public class RentalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String projectDescription;

    @Column
    private Double priorityScore;

    @Column
    private Boolean isForExam = false;

    @Column
    private LocalDate examDate;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDate returnedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public RentalRequest() {
    }

    // Private constructor used by Builder
    private RentalRequest(Builder builder) {
        this.user = builder.user;
        this.equipment = builder.equipment;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.projectDescription = builder.projectDescription;
        this.isForExam = builder.isForExam;
        this.examDate = builder.examDate;
        this.justification = builder.justification;
        this.status = RequestStatus.PENDING;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern — allows step-by-step construction of a RentalRequest
     * with many optional fields, avoiding a constructor with 10+ parameters.
     *
     * Usage:
     *   RentalRequest request = RentalRequest.builder()
     *       .user(user)
     *       .equipment(equipment)
     *       .startDate(LocalDate.of(2026, 5, 1))
     *       .endDate(LocalDate.of(2026, 5, 15))
     *       .isForExam(true)
     *       .examDate(LocalDate.of(2026, 5, 10))
     *       .build();
     */
    public static class Builder {
        private User user;
        private Equipment equipment;
        private LocalDate startDate;
        private LocalDate endDate;
        private String projectDescription;
        private Boolean isForExam = false;
        private LocalDate examDate;
        private String justification;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder equipment(Equipment equipment) {
            this.equipment = equipment;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder projectDescription(String projectDescription) {
            this.projectDescription = projectDescription;
            return this;
        }

        public Builder isForExam(Boolean isForExam) {
            this.isForExam = isForExam;
            return this;
        }

        public Builder examDate(LocalDate examDate) {
            this.examDate = examDate;
            return this;
        }

        public Builder justification(String justification) {
            this.justification = justification;
            return this;
        }

        public RentalRequest build() {
            if (user == null || equipment == null || startDate == null || endDate == null) {
                throw new IllegalStateException("user, equipment, startDate, and endDate are required");
            }
            return new RentalRequest(this);
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public Boolean getIsForExam() {
        return isForExam;
    }

    public void setIsForExam(Boolean isForExam) {
        this.isForExam = isForExam;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDate returnedAt) {
        this.returnedAt = returnedAt;
    }
}
