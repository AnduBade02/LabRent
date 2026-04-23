package ro.atemustard.labrent.dto;

import java.time.LocalDateTime;

public class ActivityEventDTO {

    private String type;
    private String message;
    private String actorUsername;
    private LocalDateTime timestamp;
    private Long referenceId;

    public ActivityEventDTO() {
    }

    public ActivityEventDTO(String type, String message, String actorUsername,
                            LocalDateTime timestamp, Long referenceId) {
        this.type = type;
        this.message = message;
        this.actorUsername = actorUsername;
        this.timestamp = timestamp;
        this.referenceId = referenceId;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
}
