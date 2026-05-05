package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.ReturnAssessment;

import java.time.LocalDateTime;

public class ReturnAssessmentDTO {

    private Long id;
    private Long rentalRequestId;
    private Long operatorId;
    private String operatorUsername;
    private String conditionRating;
    private String notes;
    private Double reputationImpact;
    private LocalDateTime assessedAt;

    public ReturnAssessmentDTO() {
    }

    public static ReturnAssessmentDTO fromEntity(ReturnAssessment assessment) {
        ReturnAssessmentDTO dto = new ReturnAssessmentDTO();
        dto.setId(assessment.getId());
        dto.setRentalRequestId(assessment.getRentalRequest().getId());
        dto.setOperatorId(assessment.getOperator().getId());
        dto.setOperatorUsername(assessment.getOperator().getUsername());
        dto.setConditionRating(assessment.getConditionRating().name());
        dto.setNotes(assessment.getNotes());
        dto.setReputationImpact(assessment.getReputationImpact());
        dto.setAssessedAt(assessment.getAssessedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRentalRequestId() { return rentalRequestId; }
    public void setRentalRequestId(Long rentalRequestId) { this.rentalRequestId = rentalRequestId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public String getConditionRating() { return conditionRating; }
    public void setConditionRating(String conditionRating) { this.conditionRating = conditionRating; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getReputationImpact() { return reputationImpact; }
    public void setReputationImpact(Double reputationImpact) { this.reputationImpact = reputationImpact; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
}
