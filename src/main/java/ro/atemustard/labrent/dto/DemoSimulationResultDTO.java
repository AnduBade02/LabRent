package ro.atemustard.labrent.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DemoSimulationResultDTO {

    private int simulatedDays;
    private LocalDate simulationStartDate;
    private LocalDate simulationEndDate;
    private int createdRequests;
    private int approvedRequests;
    private int rejectedRequests;
    private int rentedRequests;
    private int returnedRequests;
    private int completedAssessments;
    private List<DemoSimulationEventDTO> timeline = new ArrayList<>();
    private List<DemoPrioritySnapshotDTO> prioritySnapshots = new ArrayList<>();
    private List<DemoReputationChangeDTO> reputationChanges = new ArrayList<>();

    public int getSimulatedDays() { return simulatedDays; }
    public void setSimulatedDays(int simulatedDays) { this.simulatedDays = simulatedDays; }

    public LocalDate getSimulationStartDate() { return simulationStartDate; }
    public void setSimulationStartDate(LocalDate simulationStartDate) {
        this.simulationStartDate = simulationStartDate;
    }

    public LocalDate getSimulationEndDate() { return simulationEndDate; }
    public void setSimulationEndDate(LocalDate simulationEndDate) {
        this.simulationEndDate = simulationEndDate;
    }

    public int getCreatedRequests() { return createdRequests; }
    public void setCreatedRequests(int createdRequests) { this.createdRequests = createdRequests; }

    public int getApprovedRequests() { return approvedRequests; }
    public void setApprovedRequests(int approvedRequests) { this.approvedRequests = approvedRequests; }

    public int getRejectedRequests() { return rejectedRequests; }
    public void setRejectedRequests(int rejectedRequests) { this.rejectedRequests = rejectedRequests; }

    public int getRentedRequests() { return rentedRequests; }
    public void setRentedRequests(int rentedRequests) { this.rentedRequests = rentedRequests; }

    public int getReturnedRequests() { return returnedRequests; }
    public void setReturnedRequests(int returnedRequests) { this.returnedRequests = returnedRequests; }

    public int getCompletedAssessments() { return completedAssessments; }
    public void setCompletedAssessments(int completedAssessments) {
        this.completedAssessments = completedAssessments;
    }

    public List<DemoSimulationEventDTO> getTimeline() { return timeline; }
    public void setTimeline(List<DemoSimulationEventDTO> timeline) { this.timeline = timeline; }

    public List<DemoPrioritySnapshotDTO> getPrioritySnapshots() { return prioritySnapshots; }
    public void setPrioritySnapshots(List<DemoPrioritySnapshotDTO> prioritySnapshots) {
        this.prioritySnapshots = prioritySnapshots;
    }

    public List<DemoReputationChangeDTO> getReputationChanges() { return reputationChanges; }
    public void setReputationChanges(List<DemoReputationChangeDTO> reputationChanges) {
        this.reputationChanges = reputationChanges;
    }

    public static class DemoSimulationEventDTO {
        private int day;
        private LocalDate date;
        private String type;
        private String message;

        public DemoSimulationEventDTO() {
        }

        public DemoSimulationEventDTO(int day, LocalDate date, String type, String message) {
            this.day = day;
            this.date = date;
            this.type = type;
            this.message = message;
        }

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class DemoPrioritySnapshotDTO {
        private int day;
        private LocalDate date;
        private Long equipmentId;
        private String equipmentName;
        private List<DemoQueueEntryDTO> queue = new ArrayList<>();

        public DemoPrioritySnapshotDTO() {
        }

        public DemoPrioritySnapshotDTO(int day, LocalDate date, Long equipmentId, String equipmentName,
                                       List<DemoQueueEntryDTO> queue) {
            this.day = day;
            this.date = date;
            this.equipmentId = equipmentId;
            this.equipmentName = equipmentName;
            this.queue = queue;
        }

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public Long getEquipmentId() { return equipmentId; }
        public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

        public String getEquipmentName() { return equipmentName; }
        public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

        public List<DemoQueueEntryDTO> getQueue() { return queue; }
        public void setQueue(List<DemoQueueEntryDTO> queue) { this.queue = queue; }
    }

    public static class DemoQueueEntryDTO {
        private int rank;
        private Long requestId;
        private String username;
        private Double priorityScore;
        private int waitingDays;
        private int previousRejectedSimilarCount;
        private boolean examRequest;
        private Double userReputationScore;

        public DemoQueueEntryDTO() {
        }

        public DemoQueueEntryDTO(int rank, Long requestId, String username, Double priorityScore,
                                 int waitingDays, int previousRejectedSimilarCount,
                                 boolean examRequest, Double userReputationScore) {
            this.rank = rank;
            this.requestId = requestId;
            this.username = username;
            this.priorityScore = priorityScore;
            this.waitingDays = waitingDays;
            this.previousRejectedSimilarCount = previousRejectedSimilarCount;
            this.examRequest = examRequest;
            this.userReputationScore = userReputationScore;
        }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Double getPriorityScore() { return priorityScore; }
        public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }

        public int getWaitingDays() { return waitingDays; }
        public void setWaitingDays(int waitingDays) { this.waitingDays = waitingDays; }

        public int getPreviousRejectedSimilarCount() { return previousRejectedSimilarCount; }
        public void setPreviousRejectedSimilarCount(int previousRejectedSimilarCount) {
            this.previousRejectedSimilarCount = previousRejectedSimilarCount;
        }

        public boolean isExamRequest() { return examRequest; }
        public void setExamRequest(boolean examRequest) { this.examRequest = examRequest; }

        public Double getUserReputationScore() { return userReputationScore; }
        public void setUserReputationScore(Double userReputationScore) {
            this.userReputationScore = userReputationScore;
        }
    }

    public static class DemoReputationChangeDTO {
        private Long userId;
        private String username;
        private Double before;
        private Double after;
        private Double delta;

        public DemoReputationChangeDTO() {
        }

        public DemoReputationChangeDTO(Long userId, String username, Double before, Double after) {
            this.userId = userId;
            this.username = username;
            this.before = before;
            this.after = after;
            this.delta = after - before;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Double getBefore() { return before; }
        public void setBefore(Double before) { this.before = before; }

        public Double getAfter() { return after; }
        public void setAfter(Double after) { this.after = after; }

        public Double getDelta() { return delta; }
        public void setDelta(Double delta) { this.delta = delta; }
    }
}
