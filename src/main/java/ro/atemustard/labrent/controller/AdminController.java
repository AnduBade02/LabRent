package ro.atemustard.labrent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.dto.ActivityEventDTO;
import ro.atemustard.labrent.dto.AdminDashboardDTO;
import ro.atemustard.labrent.service.ActivityFeedService;
import ro.atemustard.labrent.service.AdminDashboardService;
import ro.atemustard.labrent.service.PrioritizationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PrioritizationService prioritizationService;
    private final AdminDashboardService adminDashboardService;
    private final ActivityFeedService activityFeedService;

    public AdminController(PrioritizationService prioritizationService,
                           AdminDashboardService adminDashboardService,
                           ActivityFeedService activityFeedService) {
        this.prioritizationService = prioritizationService;
        this.adminDashboardService = adminDashboardService;
        this.activityFeedService = activityFeedService;
    }

    @GetMapping("/prioritization-strategy")
    public ResponseEntity<Map<String, String>> getStrategy() {
        return ResponseEntity.ok(Map.of("strategy", prioritizationService.getActiveStrategyName()));
    }

    @PutMapping("/prioritization-strategy")
    public ResponseEntity<Map<String, String>> setStrategy(@RequestBody Map<String, String> body) {
        String strategy = body.get("strategy");
        prioritizationService.setActiveStrategyName(strategy);
        // Rescore all PENDING requests under the new strategy so the queue
        // reflects the change immediately.
        prioritizationService.recalculateAllPending();
        return ResponseEntity.ok(Map.of("strategy", prioritizationService.getActiveStrategyName()));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<AdminDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }

    @GetMapping("/activity-feed")
    public ResponseEntity<List<ActivityEventDTO>> getActivityFeed(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(activityFeedService.getRecentEvents(limit));
    }
}
