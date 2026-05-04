package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private LocalDate returnedAt;
    private Boolean overdue;
    private Integer daysOverdue;
    private Integer daysRemaining;

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
        if (request instanceof ro.atemustard.labrent.model.AcademicRentalRequest academic) {
            dto.setIsForExam(true);
            dto.setExamDate(academic.getExamDate());
            dto.setJustification(academic.getJustification());
        } else {
            dto.setIsForExam(false);
        }
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReturnedAt(request.getReturnedAt());

        LocalDate today = LocalDate.now();
        boolean isRented = request.getStatus() == RequestStatus.RENTED;
        boolean isOverdue = isRented && request.getEndDate() != null && request.getEndDate().isBefore(today);
        dto.setOverdue(isOverdue);
        if (isOverdue) {
            dto.setDaysOverdue((int) ChronoUnit.DAYS.between(request.getEndDate(), today));
        } else {
            dto.setDaysOverdue(0);
        }
        if (isRented && request.getEndDate() != null) {
            dto.setDaysRemaining((int) ChronoUnit.DAYS.between(today, request.getEndDate()));
        } else {
            dto.setDaysRemaining(null);
        }
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

    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }

    public Boolean getOverdue() { return overdue; }
    public void setOverdue(Boolean overdue) { this.overdue = overdue; }

    public Integer getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }

    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
}
