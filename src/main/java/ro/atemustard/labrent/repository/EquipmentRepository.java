package ro.atemustard.labrent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.EquipmentStatus;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByCategory(String category);

    List<Equipment> findByCategoryAndStatus(String category, EquipmentStatus status);

    List<Equipment> findByNameContainingIgnoreCase(String name);
}
