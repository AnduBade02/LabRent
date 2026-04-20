package ro.atemustard.labrent.dto;

import jakarta.validation.constraints.NotNull;

public class ReturnAssessmentCreateDTO {

    @NotNull(message = "Rental request ID is required")
    private Long rentalRequestId;

    @NotNull(message = "Condition rating is required (EXCELLENT, GOOD, FAIR, POOR, DAMAGED)")
    private String conditionRating;

    private String notes;

    public ReturnAssessmentCreateDTO() {
    }

    public Long getRentalRequestId() { return rentalRequestId; }
    public void setRentalRequestId(Long rentalRequestId) { this.rentalRequestId = rentalRequestId; }

    public String getConditionRating() { return conditionRating; }
    public void setConditionRating(String conditionRating) { this.conditionRating = conditionRating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
