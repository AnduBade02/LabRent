package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.RentalRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalRequestDTO {

    private Long id;
    private Long userId;
    private String username;
    private Long equipmentId;
    private String equipmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String projectDescription;
    private Double priorityScore;
    private Boolean isForExam;
    private LocalDate examDate;
    private String justification;
    private LocalDateTime createdAt;

    public RentalRequestDTO() {
    }

    public static RentalRequestDTO fromEntity(RentalRequest request) {
        RentalRequestDTO dto = new RentalRequestDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUser().getId());
        dto.setUsername(request.getUser().getUsername());
        dto.setEquipmentId(request.getEquipment().getId());
        dto.setEquipmentName(request.getEquipment().getName());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setStatus(request.getStatus().name());
        dto.setProjectDescription(request.getProjectDescription());
        dto.setPriorityScore(request.getPriorityScore());
        dto.setIsForExam(request.getIsForExam());
        dto.setExamDate(request.getExamDate());
        dto.setJustification(request.getJustification());
        dto.setCreatedAt(request.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }

    public Double getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }

    public Boolean getIsForExam() { return isForExam; }
    public void setIsForExam(Boolean isForExam) { this.isForExam = isForExam; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
