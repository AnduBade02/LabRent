package ro.atemustard.labrent.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.dto.ReturnAssessmentCreateDTO;
import ro.atemustard.labrent.dto.ReturnAssessmentDTO;
import ro.atemustard.labrent.service.ReturnAssessmentService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/return-assessments")
public class ReturnAssessmentController {

    private final ReturnAssessmentService returnAssessmentService;

    public ReturnAssessmentController(ReturnAssessmentService returnAssessmentService) {
        this.returnAssessmentService = returnAssessmentService;
    }

    @PostMapping
    public ResponseEntity<ReturnAssessmentDTO> submitAssessment(
            @Valid @RequestBody ReturnAssessmentCreateDTO dto, Principal principal) {
        return ResponseEntity.ok(returnAssessmentService.submitAssessment(dto, principal.getName()));
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<ReturnAssessmentDTO> getByRequestId(@PathVariable Long requestId) {
        return ResponseEntity.ok(returnAssessmentService.getAssessmentByRequestId(requestId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReturnAssessmentDTO>> getUserHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(returnAssessmentService.getUserAssessmentHistory(userId));
    }
}
