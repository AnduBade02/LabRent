package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.Equipment;

import java.time.LocalDateTime;

public class EquipmentDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String status;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private LocalDateTime createdAt;

    public EquipmentDTO() {
    }

    public static EquipmentDTO fromEntity(Equipment equipment) {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setId(equipment.getId());
        dto.setName(equipment.getName());
        dto.setDescription(equipment.getDescription());
        dto.setCategory(equipment.getCategory());
        dto.setStatus(equipment.getStatus().name());
        dto.setTotalQuantity(equipment.getTotalQuantity());
        dto.setAvailableQuantity(equipment.getAvailableQuantity());
        dto.setCreatedAt(equipment.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
