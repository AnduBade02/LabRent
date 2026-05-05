package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.dto.EquipmentCreateDTO;
import ro.atemustard.labrent.dto.EquipmentDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;
import ro.atemustard.labrent.model.state.EquipmentStateFactory;
import ro.atemustard.labrent.repository.EquipmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public EquipmentDTO createEquipment(EquipmentCreateDTO dto) {
        Equipment equipment = new Equipment(
                dto.getName(),
                dto.getDescription(),
                dto.getCategory(),
                dto.getTotalQuantity()
        );
        Equipment saved = equipmentRepository.save(equipment);
        return EquipmentDTO.fromEntity(saved);
    }

    public EquipmentDTO getEquipmentById(Long id) {
        Equipment equipment = findEntityById(id);
        return EquipmentDTO.fromEntity(equipment);
    }

    public List<EquipmentDTO> getAllEquipment() {
        return equipmentRepository.findAll().stream()
                .map(EquipmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EquipmentDTO> getAvailableEquipment() {
        return equipmentRepository.findByStatus(EquipmentStatus.AVAILABLE).stream()
                .map(EquipmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EquipmentDTO> getByCategory(String category) {
        return equipmentRepository.findByCategory(category).stream()
                .map(EquipmentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EquipmentDTO updateEquipment(Long id, EquipmentCreateDTO dto) {
        Equipment equipment = findEntityById(id);
        equipment.setName(dto.getName());
        equipment.setDescription(dto.getDescription());
        equipment.setCategory(dto.getCategory());

        int oldTotal = equipment.getTotalQuantity();
        int newTotal = dto.getTotalQuantity();
        int diff = newTotal - oldTotal;
        equipment.setTotalQuantity(newTotal);
        equipment.setAvailableQuantity(Math.max(0, equipment.getAvailableQuantity() + diff));

        Equipment saved = equipmentRepository.save(equipment);
        return EquipmentDTO.fromEntity(saved);
    }

    public void deleteEquipment(Long id) {
        Equipment equipment = findEntityById(id);
        equipmentRepository.delete(equipment);
    }

    public void reserveUnit(Long equipmentId) {
        Equipment equipment = findEntityById(equipmentId);
        if (equipment.getAvailableQuantity() <= 0) {
            throw new InvalidOperationException("No available units for equipment: " + equipment.getName());
        }
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() - 1);
        // State pattern: when the last unit leaves the pool, transition the
        // equipment's global status through the State machine so the
        // transition rules live in one place.
        if (equipment.getAvailableQuantity() == 0
                && equipment.getStatus() == EquipmentStatus.AVAILABLE) {
            EquipmentStateFactory.fromStatus(equipment.getStatus()).reserve(equipment);
        }
        equipmentRepository.save(equipment);
    }

    public void releaseUnit(Long equipmentId) {
        Equipment equipment = findEntityById(equipmentId);
        int before = equipment.getAvailableQuantity();
        equipment.setAvailableQuantity(
                Math.min(equipment.getTotalQuantity(), before + 1));
        // State pattern: first unit returning to the pool flips the global
        // status back to AVAILABLE via the State machine.
        if (before == 0 && equipment.getAvailableQuantity() > 0
                && equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            EquipmentStateFactory.fromStatus(equipment.getStatus()).makeAvailable(equipment);
        }
        equipmentRepository.save(equipment);
    }

    public Equipment findEntityById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment", "id", id));
    }
}
