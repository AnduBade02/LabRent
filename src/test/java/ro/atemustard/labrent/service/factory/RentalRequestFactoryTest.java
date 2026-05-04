package ro.atemustard.labrent.service.factory;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.AcademicRentalRequest;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.StandardRentalRequest;
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
    void standardFactory_producesStandardSubclassWithoutAcademicFields() {
        RentalRequestCreateDTO dto = baseDto();
        RentalRequest r = new StandardRentalRequestFactory().createRequest(user, equipment, dto);

        assertThat(r).isInstanceOf(StandardRentalRequest.class);
        assertThat(r).isNotInstanceOf(AcademicRentalRequest.class);
        assertThat(r.getProjectDescription()).isEqualTo("desc");
    }

    @Test
    void academicFactory_producesAcademicSubclassWithExamFields() {
        RentalRequestCreateDTO dto = baseDto();
        dto.setIsForExam(true);
        dto.setExamDate(LocalDate.now().plusDays(10));
        dto.setJustification("Final exam in electronics");

        RentalRequest r = new AcademicRentalRequestFactory().createRequest(user, equipment, dto);

        assertThat(r).isInstanceOf(AcademicRentalRequest.class);
        AcademicRentalRequest academic = (AcademicRentalRequest) r;
        assertThat(academic.getExamDate()).isEqualTo(dto.getExamDate());
        assertThat(academic.getJustification()).isEqualTo("Final exam in electronics");
    }
}
