package ro.atemustard.labrent.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.atemustard.labrent.dto.EquipmentCreateDTO;
import ro.atemustard.labrent.dto.EquipmentDTO;
import ro.atemustard.labrent.service.EquipmentService;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<List<EquipmentDTO>> getAllEquipment() {
        return ResponseEntity.ok(equipmentService.getAllEquipment());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDTO> getEquipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    @GetMapping("/available")
    public ResponseEntity<List<EquipmentDTO>> getAvailableEquipment() {
        return ResponseEntity.ok(equipmentService.getAvailableEquipment());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EquipmentDTO>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(equipmentService.getByCategory(category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentDTO> createEquipment(@Valid @RequestBody EquipmentCreateDTO dto) {
        return ResponseEntity.ok(equipmentService.createEquipment(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EquipmentDTO> updateEquipment(@PathVariable Long id,
                                                         @Valid @RequestBody EquipmentCreateDTO dto) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}
