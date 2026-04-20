package ro.atemustard.labrent.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.dto.RentalRequestDTO;
import ro.atemustard.labrent.service.RentalRequestService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/rental-requests")
public class RentalRequestController {

    private final RentalRequestService rentalRequestService;

    public RentalRequestController(RentalRequestService rentalRequestService) {
        this.rentalRequestService = rentalRequestService;
    }

    @PostMapping
    public ResponseEntity<RentalRequestDTO> createRequest(
            @Valid @RequestBody RentalRequestCreateDTO dto, Principal principal) {
        return ResponseEntity.ok(rentalRequestService.createRequest(dto, principal.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<RentalRequestDTO>> getMyRequests(Principal principal) {
        return ResponseEntity.ok(rentalRequestService.getUserRequests(principal.getName()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RentalRequestDTO>> getPendingRequests() {
        return ResponseEntity.ok(rentalRequestService.getPendingRequests());
    }

    @GetMapping("/prioritized/{equipmentId}")
    public ResponseEntity<List<RentalRequestDTO>> getPrioritizedRequests(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(rentalRequestService.getPrioritizedPendingRequests(equipmentId));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<RentalRequestDTO> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(rentalRequestService.approveRequest(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<RentalRequestDTO> rejectRequest(@PathVariable Long id) {
        return ResponseEntity.ok(rentalRequestService.rejectRequest(id));
    }

    @PutMapping("/{id}/rent")
    public ResponseEntity<RentalRequestDTO> markAsRented(@PathVariable Long id) {
        return ResponseEntity.ok(rentalRequestService.markAsRented(id));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<RentalRequestDTO> markAsReturned(@PathVariable Long id) {
        return ResponseEntity.ok(rentalRequestService.markAsReturned(id));
    }
}
