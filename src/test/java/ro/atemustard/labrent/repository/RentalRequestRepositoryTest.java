package ro.atemustard.labrent.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ro.atemustard.labrent.model.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RentalRequestRepositoryTest {

    @Autowired private RentalRequestRepository rentalRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EquipmentRepository equipmentRepository;

    private User user;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT));
        equipment = equipmentRepository.save(new Equipment("Scope", "d", "cat", 5));
    }

    private RentalRequest persist(double priority, RequestStatus status) {
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(equipment)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();
        r.setStatus(status);
        r.setPriorityScore(priority);
        return rentalRequestRepository.save(r);
    }

    @Test
    void orderByPriorityScoreDesc_returnsHighestFirst() {
        RentalRequest low = persist(50.0, RequestStatus.PENDING);
        RentalRequest high = persist(120.0, RequestStatus.PENDING);
        RentalRequest mid = persist(80.0, RequestStatus.PENDING);

        List<RentalRequest> ordered = rentalRequestRepository
                .findByEquipmentIdAndStatusOrderByPriorityScoreDescCreatedAtAsc(
                        equipment.getId(), RequestStatus.PENDING);

        assertThat(ordered).extracting(RentalRequest::getId)
                .containsExactly(high.getId(), mid.getId(), low.getId());
    }

    @Test
    void orderByCreatedAtAsc_returnsOldestFirst() {
        RentalRequest first = persist(50.0, RequestStatus.PENDING);
        RentalRequest second = persist(120.0, RequestStatus.PENDING);

        List<RentalRequest> ordered = rentalRequestRepository
                .findByEquipmentIdAndStatusOrderByCreatedAtAsc(
                        equipment.getId(), RequestStatus.PENDING);

        assertThat(ordered).extracting(RentalRequest::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void countByUserIdAndStatusIn_countsOnlyMatchingStatuses() {
        persist(0.0, RequestStatus.PENDING);
        persist(0.0, RequestStatus.APPROVED);
        persist(0.0, RequestStatus.REJECTED);
        persist(0.0, RequestStatus.COMPLETED);

        long active = rentalRequestRepository.countByUserIdAndStatusIn(
                user.getId(),
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.RENTED));

        assertThat(active).isEqualTo(2L);
    }

    @Test
    void findByEquipmentIdAndStatus_filtersBoth() {
        persist(0.0, RequestStatus.PENDING);
        persist(0.0, RequestStatus.APPROVED);

        List<RentalRequest> pending = rentalRequestRepository
                .findByEquipmentIdAndStatus(equipment.getId(), RequestStatus.PENDING);
        assertThat(pending).hasSize(1);
    }
}
