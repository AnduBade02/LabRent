package ro.atemustard.labrent.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalRequestBuilderTest {

    private final User user = new User("u", "u@x.com", "p", Role.USER, UserType.STUDENT);
    private final Equipment eq = new Equipment("Scope", "d", "cat", 1);

    @Test
    void build_setsDefaultsAndStatusPending_andProducesStandardSubclass() {
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(eq)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .build();

        assertThat(r.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(r).isInstanceOf(StandardRentalRequest.class);
        assertThat(r.getUser()).isSameAs(user);
        assertThat(r.getEquipment()).isSameAs(eq);
    }

    @Test
    void build_missingUser_throws() {
        assertThatThrownBy(() -> RentalRequest.builder()
                .equipment(eq)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build()
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void build_missingDates_throws() {
        assertThatThrownBy(() -> RentalRequest.builder()
                .user(user).equipment(eq)
                .build()
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void build_examFieldsArePropagated_andProducesAcademicSubclass() {
        LocalDate exam = LocalDate.now().plusDays(7);
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(eq)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .isForExam(true)
                .examDate(exam)
                .justification("midterm")
                .build();

        assertThat(r).isInstanceOf(AcademicRentalRequest.class);
        AcademicRentalRequest academic = (AcademicRentalRequest) r;
        assertThat(academic.getExamDate()).isEqualTo(exam);
        assertThat(academic.getJustification()).isEqualTo("midterm");
    }
}
