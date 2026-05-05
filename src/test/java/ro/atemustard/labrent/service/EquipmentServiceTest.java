package ro.atemustard.labrent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.dto.EquipmentCreateDTO;
import ro.atemustard.labrent.dto.EquipmentDTO;
import ro.atemustard.labrent.exception.InvalidOperationException;
import ro.atemustard.labrent.exception.ResourceNotFoundException;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;
import ro.atemustard.labrent.repository.EquipmentRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock private EquipmentRepository equipmentRepository;
    @InjectMocks private EquipmentService equipmentService;

    private Equipment equipment(int total, int available, EquipmentStatus status) {
        Equipment e = new Equipment("Scope", "d", "cat", total);
        e.setId(1L);
        e.setAvailableQuantity(available);
        e.setStatus(status);
        return e;
    }

    @Test
    void createEquipment_initialAvailableEqualsTotal() {
        EquipmentCreateDTO dto = new EquipmentCreateDTO();
        dto.setName("Multimeter");
        dto.setCategory("instruments");
        dto.setTotalQuantity(5);
        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipmentDTO out = equipmentService.createEquipment(dto);

        assertThat(out.getTotalQuantity()).isEqualTo(5);
        assertThat(out.getAvailableQuantity()).isEqualTo(5);
        assertThat(out.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void reserveUnit_decrementsAndFlipsToReservedAtZero() {
        Equipment e = equipment(2, 1, EquipmentStatus.AVAILABLE);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));

        equipmentService.reserveUnit(1L);

        assertThat(e.getAvailableQuantity()).isEqualTo(0);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.RESERVED);
    }

    @Test
    void reserveUnit_keepsAvailableStatusWhenStockRemains() {
        Equipment e = equipment(5, 3, EquipmentStatus.AVAILABLE);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));

        equipmentService.reserveUnit(1L);

        assertThat(e.getAvailableQuantity()).isEqualTo(2);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void reserveUnit_zeroAvailable_throws() {
        Equipment e = equipment(2, 0, EquipmentStatus.RESERVED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> equipmentService.reserveUnit(1L))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void releaseUnit_increasesAvailableAndFlipsBackToAvailable() {
        Equipment e = equipment(2, 0, EquipmentStatus.RESERVED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));

        equipmentService.releaseUnit(1L);

        assertThat(e.getAvailableQuantity()).isEqualTo(1);
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void releaseUnit_doesNotExceedTotalQuantity() {
        Equipment e = equipment(3, 3, EquipmentStatus.AVAILABLE);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));

        equipmentService.releaseUnit(1L);

        assertThat(e.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void updateEquipment_adjustsAvailableByDelta() {
        Equipment e = equipment(5, 2, EquipmentStatus.AVAILABLE);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EquipmentCreateDTO dto = new EquipmentCreateDTO();
        dto.setName("Scope+");
        dto.setCategory("cat");
        dto.setTotalQuantity(8);
        equipmentService.updateEquipment(1L, dto);

        // delta = +3, available 2 + 3 = 5
        assertThat(e.getTotalQuantity()).isEqualTo(8);
        assertThat(e.getAvailableQuantity()).isEqualTo(5);
        assertThat(e.getName()).isEqualTo("Scope+");
    }

    @Test
    void updateEquipment_negativeAvailableIsClampedToZero() {
        Equipment e = equipment(5, 1, EquipmentStatus.AVAILABLE);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EquipmentCreateDTO dto = new EquipmentCreateDTO();
        dto.setName("Scope");
        dto.setCategory("cat");
        dto.setTotalQuantity(1); // delta -4, would become -3

        equipmentService.updateEquipment(1L, dto);
        assertThat(e.getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    void findById_missing_throws() {
        when(equipmentRepository.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> equipmentService.findEntityById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
