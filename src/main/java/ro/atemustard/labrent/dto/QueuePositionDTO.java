package ro.atemustard.labrent.dto;

public class QueuePositionDTO {

    private Long requestId;
    private Long equipmentId;
    private Integer position;
    private Integer total;

    public QueuePositionDTO() {
    }

    public QueuePositionDTO(Long requestId, Long equipmentId, Integer position, Integer total) {
        this.requestId = requestId;
        this.equipmentId = equipmentId;
        this.position = position;
        this.total = total;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
