package ro.atemustard.labrent.dto;

import java.util.List;
import java.util.Map;

public class AdminDashboardDTO {

    private Long pendingCount;
    private Long activeRentalsCount;
    private Long overdueCount;
    private Long returnedCount;
    private Long totalEquipment;
    private Double utilizationPct;
    private Map<String, Long> statusDistribution;
    private List<UserSummaryDTO> topUsers;
    private Map<String, Double> perEquipmentUtilization;

    public AdminDashboardDTO() {
    }

    public Long getPendingCount() { return pendingCount; }
    public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }

    public Long getActiveRentalsCount() { return activeRentalsCount; }
    public void setActiveRentalsCount(Long activeRentalsCount) { this.activeRentalsCount = activeRentalsCount; }

    public Long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Long overdueCount) { this.overdueCount = overdueCount; }

    public Long getReturnedCount() { return returnedCount; }
    public void setReturnedCount(Long returnedCount) { this.returnedCount = returnedCount; }

    public Long getTotalEquipment() { return totalEquipment; }
    public void setTotalEquipment(Long totalEquipment) { this.totalEquipment = totalEquipment; }

    public Double getUtilizationPct() { return utilizationPct; }
    public void setUtilizationPct(Double utilizationPct) { this.utilizationPct = utilizationPct; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public List<UserSummaryDTO> getTopUsers() { return topUsers; }
    public void setTopUsers(List<UserSummaryDTO> topUsers) { this.topUsers = topUsers; }

    public Map<String, Double> getPerEquipmentUtilization() { return perEquipmentUtilization; }
    public void setPerEquipmentUtilization(Map<String, Double> perEquipmentUtilization) {
        this.perEquipmentUtilization = perEquipmentUtilization;
    }
}
