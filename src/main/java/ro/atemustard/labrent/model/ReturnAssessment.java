package ro.atemustard.labrent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ReturnAssessment entity — the post-return verification form.
 *
 * When equipment is returned, an operator (ADMIN) fills in this form:
 * - conditionRating: equipment state (EXCELLENT → DAMAGED)
 * - notes: free-form observations
 * - reputationImpact: the user's reputation delta
 *
 * 1:1 relationship with RentalRequest (each request has at most one assessment).
 *
 * Design Pattern: Builder — same as RentalRequest.
 */
@Entity
@Table(name = "return_assessments")
public class ReturnAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_request_id", nullable = false, unique = true)
    private RentalRequest rentalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionRating conditionRating;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Double reputationImpact;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    @PrePersist
    protected void onCreate() {
        this.assessedAt = LocalDateTime.now();
    }

    public ReturnAssessment() {
    }

    private ReturnAssessment(Builder builder) {
        this.rentalRequest = builder.rentalRequest;
        this.operator = builder.operator;
        this.conditionRating = builder.conditionRating;
        this.notes = builder.notes;
        this.reputationImpact = builder.conditionRating.getReputationImpact();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RentalRequest rentalRequest;
        private User operator;
        private ConditionRating conditionRating;
        private String notes;

        public Builder rentalRequest(RentalRequest rentalRequest) {
            this.rentalRequest = rentalRequest;
            return this;
        }

        public Builder operator(User operator) {
            this.operator = operator;
            return this;
        }

        public Builder conditionRating(ConditionRating conditionRating) {
            this.conditionRating = conditionRating;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public ReturnAssessment build() {
            if (rentalRequest == null || operator == null || conditionRating == null) {
                throw new IllegalStateException("rentalRequest, operator, and conditionRating are required");
            }
            return new ReturnAssessment(this);
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RentalRequest getRentalRequest() {
        return rentalRequest;
    }

    public void setRentalRequest(RentalRequest rentalRequest) {
        this.rentalRequest = rentalRequest;
    }

    public User getOperator() {
        return operator;
    }

    public void setOperator(User operator) {
        this.operator = operator;
    }

    public ConditionRating getConditionRating() {
        return conditionRating;
    }

    public void setConditionRating(ConditionRating conditionRating) {
        this.conditionRating = conditionRating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getReputationImpact() {
        return reputationImpact;
    }

    public void setReputationImpact(Double reputationImpact) {
        this.reputationImpact = reputationImpact;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }
}
