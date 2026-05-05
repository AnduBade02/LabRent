package ro.atemustard.labrent.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RentalRequest — abstract base of the rental request hierarchy.
 *
 * Generalization (UML): two concrete subclasses extend this class —
 * {@link StandardRentalRequest} and {@link AcademicRentalRequest}. The
 * subclass is chosen by the Builder based on whether academic urgency
 * (exam-related fields) is provided.
 *
 * Composition (UML): each RentalRequest owns at most one ReturnAssessment.
 * The lifecycle is bound — deleting a request deletes its assessment
 * (cascade=ALL, orphanRemoval=true).
 *
 * Persistence: SINGLE_TABLE inheritance with a {@code request_type}
 * discriminator column distinguishing STANDARD from ACADEMIC rows.
 *
 * Builder pattern: see {@link Builder} below — produces the correct concrete
 * subclass depending on the inputs.
 */
@Entity
@Table(name = "rental_requests")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "request_type", discriminatorType = DiscriminatorType.STRING)
public abstract class RentalRequest {

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDate returnedAt;

    /**
     * Composition with ReturnAssessment: when this request is removed,
     * the associated assessment is removed too. The FK is owned by
     * ReturnAssessment (see {@code rentalRequest} field there) so the
     * physical schema does not change.
     */
    @OneToOne(mappedBy = "rentalRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private ReturnAssessment returnAssessment;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    protected RentalRequest() {
    }

    protected RentalRequest(User user, Equipment equipment,
                            LocalDate startDate, LocalDate endDate,
                            String projectDescription) {
        this.user = user;
        this.equipment = equipment;
        this.startDate = startDate;
        this.endDate = endDate;
        this.projectDescription = projectDescription;
        this.status = RequestStatus.PENDING;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern — produces the correct concrete subclass based on
     * whether exam-related fields are provided. Setting {@code isForExam(true)}
     * causes {@link #build()} to instantiate an {@link AcademicRentalRequest};
     * otherwise a {@link StandardRentalRequest}.
     */
    public static class Builder {
        private User user;
        private Equipment equipment;
        private LocalDate startDate;
        private LocalDate endDate;
        private String projectDescription;
        private boolean isForExam = false;
        private LocalDate examDate;
        private String justification;

        public Builder user(User user) { this.user = user; return this; }
        public Builder equipment(Equipment equipment) { this.equipment = equipment; return this; }
        public Builder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public Builder projectDescription(String projectDescription) {
            this.projectDescription = projectDescription; return this;
        }
        public Builder isForExam(Boolean isForExam) {
            this.isForExam = Boolean.TRUE.equals(isForExam); return this;
        }
        public Builder examDate(LocalDate examDate) { this.examDate = examDate; return this; }
        public Builder justification(String justification) { this.justification = justification; return this; }

        public RentalRequest build() {
            if (user == null || equipment == null || startDate == null || endDate == null) {
                throw new IllegalStateException("user, equipment, startDate, and endDate are required");
            }
            if (isForExam) {
                return new AcademicRentalRequest(user, equipment, startDate, endDate,
                        projectDescription, examDate, justification);
            }
            return new StandardRentalRequest(user, equipment, startDate, endDate, projectDescription);
        }
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }

    public Double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }

    public ReturnAssessment getReturnAssessment() { return returnAssessment; }
    public void setReturnAssessment(ReturnAssessment returnAssessment) { this.returnAssessment = returnAssessment; }
}
