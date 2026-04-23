package ro.atemustard.labrent.service.factory;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;

/**
 * Factory for requests with academic urgency (students with an exam).
 * Sets the isForExam, examDate and justification fields.
 */
@Component("academicFactory")
public class AcademicRentalRequestFactory implements RentalRequestFactory {

    @Override
    public RentalRequest createRequest(User user, Equipment equipment, RentalRequestCreateDTO dto) {
        return RentalRequest.builder()
                .user(user)
                .equipment(equipment)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .projectDescription(dto.getProjectDescription())
                .isForExam(true)
                .examDate(dto.getExamDate())
                .justification(dto.getJustification())
                .build();
    }
}
