package ro.atemustard.labrent.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EquipmentRepositoryTest {

    @Autowired private EquipmentRepository equipmentRepository;

    @Test
    void findByCategoryAndStatus_filtersBoth() {
        Equipment a = new Equipment("Scope", "d", "instruments", 2);
        Equipment b = new Equipment("Multimeter", "d", "instruments", 1);
        b.setStatus(EquipmentStatus.RESERVED);
        Equipment c = new Equipment("Beaker", "d", "glassware", 5);
        equipmentRepository.save(a);
        equipmentRepository.save(b);
        equipmentRepository.save(c);

        assertThat(equipmentRepository.findByCategory("instruments")).hasSize(2);
        assertThat(equipmentRepository.findByCategoryAndStatus("instruments", EquipmentStatus.AVAILABLE))
                .hasSize(1);
    }

    @Test
    void findByNameContainingIgnoreCase_returnsFuzzyMatches() {
        equipmentRepository.save(new Equipment("Rigol Oscilloscope", "d", "cat", 1));
        equipmentRepository.save(new Equipment("Fluke Multimeter", "d", "cat", 1));

        assertThat(equipmentRepository.findByNameContainingIgnoreCase("oscillo")).hasSize(1);
        assertThat(equipmentRepository.findByNameContainingIgnoreCase("METER")).hasSize(1);
    }
}
