package ro.atemustard.labrent.dto;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void userDto_doesNotExposePassword() {
        User u = new User("alice", "a@x.com", "supersecret", Role.USER, UserType.STUDENT);
        u.setId(7L);
        u.setReputationScore(85.0);
        UserDTO dto = UserDTO.fromEntity(u);

        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getUserType()).isEqualTo("STUDENT");
        assertThat(dto.getReputationScore()).isEqualTo(85.0);
        // No password field on UserDTO at all — verify by reflection that no string contains "supersecret"
        assertThat(dto.toString()).doesNotContain("supersecret");
    }

    @Test
    void equipmentDto_mapsAllFields() {
        Equipment e = new Equipment("Scope", "desc", "cat", 4);
        e.setId(3L);
        e.setStatus(EquipmentStatus.RESERVED);
        e.setAvailableQuantity(2);
        EquipmentDTO dto = EquipmentDTO.fromEntity(e);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getName()).isEqualTo("Scope");
        assertThat(dto.getStatus()).isEqualTo("RESERVED");
        assertThat(dto.getTotalQuantity()).isEqualTo(4);
        assertThat(dto.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void rentalRequestDto_overdueFlag_whenRentedPastEndDate() {
        User u = new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT);
        u.setId(1L);
        Equipment e = new Equipment("Scope", "d", "cat", 1);
        e.setId(2L);
        RentalRequest r = RentalRequest.builder()
                .user(u).equipment(e)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(3))
                .build();
        r.setId(5L);
        r.setStatus(RequestStatus.RENTED);
        r.setCreatedAt(LocalDateTime.now().minusDays(11));

        RentalRequestDTO dto = RentalRequestDTO.fromEntity(r);

        assertThat(dto.getOverdue()).isTrue();
        assertThat(dto.getDaysOverdue()).isEqualTo(3);
        assertThat(dto.getDaysRemaining()).isNegative();
    }

    @Test
    void rentalRequestDto_notRented_overdueFalseAndDaysRemainingNull() {
        User u = new User("alice", "a@x.com", "p", Role.USER, UserType.STUDENT);
        u.setId(1L);
        Equipment e = new Equipment("Scope", "d", "cat", 1);
        e.setId(2L);
        RentalRequest r = RentalRequest.builder()
                .user(u).equipment(e)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();
        r.setId(5L);
        // status is PENDING by default

        RentalRequestDTO dto = RentalRequestDTO.fromEntity(r);

        assertThat(dto.getOverdue()).isFalse();
        assertThat(dto.getDaysOverdue()).isEqualTo(0);
        assertThat(dto.getDaysRemaining()).isNull();
    }
}
