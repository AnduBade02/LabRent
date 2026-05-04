package ro.atemustard.labrent.service.factory;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RentalRequestFactoryTest {

    private final User user = new User("u", "u@x.com", "p", null, UserType.STUDENT);
    private final Equipment equipment = new Equipment("Scope", "d", "cat", 1);

    private RentalRequestCreateDTO baseDto() {
        RentalRequestCreateDTO dto = new RentalRequestCreateDTO();
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setProjectDescription("desc");
        return dto;
    }

    @Test
    void standardFactory_doesNotSetExamFields() {
        RentalRequestCreateDTO dto = baseDto();
        RentalRequest r = new StandardRentalRequestFactory().createRequest(user, equipment, dto);

        assertThat(r.getIsForExam()).isFalse();
        assertThat(r.getExamDate()).isNull();
        assertThat(r.getJustification()).isNull();
        assertThat(r.getProjectDescription()).isEqualTo("desc");
    }

    @Test
    void academicFactory_setsAllExamFields() {
        RentalRequestCreateDTO dto = baseDto();
        dto.setIsForExam(true);
        dto.setExamDate(LocalDate.now().plusDays(10));
        dto.setJustification("Final exam in electronics");

        RentalRequest r = new AcademicRentalRequestFactory().createRequest(user, equipment, dto);

        assertThat(r.getIsForExam()).isTrue();
        assertThat(r.getExamDate()).isEqualTo(dto.getExamDate());
        assertThat(r.getJustification()).isEqualTo("Final exam in electronics");
    }
}
