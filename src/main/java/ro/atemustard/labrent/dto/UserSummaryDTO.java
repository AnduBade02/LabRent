package ro.atemustard.labrent.dto;

import ro.atemustard.labrent.model.User;

public class UserSummaryDTO {

    private Long id;
    private String username;
    private String userType;
    private Double reputationScore;

    public UserSummaryDTO() {
    }

    public static UserSummaryDTO fromEntity(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setUserType(user.getUserType().name());
        dto.setReputationScore(user.getReputationScore());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }
}
