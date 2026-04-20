package ro.atemustard.labrent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.service.PrioritizationService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PrioritizationService prioritizationService;

    public AdminController(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;
    }

    @GetMapping("/prioritization-strategy")
    public ResponseEntity<Map<String, String>> getStrategy() {
        return ResponseEntity.ok(Map.of("strategy", prioritizationService.getActiveStrategyName()));
    }

    @PutMapping("/prioritization-strategy")
    public ResponseEntity<Map<String, String>> setStrategy(@RequestBody Map<String, String> body) {
        String strategy = body.get("strategy");
        prioritizationService.setActiveStrategyName(strategy);
        return ResponseEntity.ok(Map.of("strategy", prioritizationService.getActiveStrategyName()));
    }
}
