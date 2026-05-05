package ro.atemustard.labrent.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * AcademicRentalRequest — concrete request with academic urgency
 * (a student preparing for an exam).
 *
 * Generalization (UML): extends RentalRequest. Adds the academic-only
 * fields {@code examDate} and {@code justification}; the prioritization
 * algorithm rewards a near exam date with extra points.
 */
@Entity
@DiscriminatorValue("ACADEMIC")
public class AcademicRentalRequest extends RentalRequest {

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(columnDefinition = "TEXT")
    private String justification;

    public AcademicRentalRequest() {
        super();
    }

    public AcademicRentalRequest(User user, Equipment equipment,
                                 LocalDate startDate, LocalDate endDate,
                                 String projectDescription,
                                 LocalDate examDate, String justification) {
        super(user, equipment, startDate, endDate, projectDescription);
        this.examDate = examDate;
        this.justification = justification;
    }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
