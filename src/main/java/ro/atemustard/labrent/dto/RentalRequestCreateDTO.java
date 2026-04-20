package ro.atemustard.labrent.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class RentalRequestCreateDTO {

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    private String projectDescription;

    private Boolean isForExam = false;

    private LocalDate examDate;

    private String justification;

    public RentalRequestCreateDTO() {
    }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }

    public Boolean getIsForExam() { return isForExam; }
    public void setIsForExam(Boolean isForExam) { this.isForExam = isForExam; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
}
