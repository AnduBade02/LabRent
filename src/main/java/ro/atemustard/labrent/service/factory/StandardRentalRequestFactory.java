package ro.atemustard.labrent.service.factory;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;

/**
 * Factory for standard rental requests (without academic urgency).
 */
@Component("standardFactory")
public class StandardRentalRequestFactory implements RentalRequestFactory {

    @Override
    public RentalRequest createRequest(User user, Equipment equipment, RentalRequestCreateDTO dto) {
        return RentalRequest.builder()
                .user(user)
                .equipment(equipment)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .projectDescription(dto.getProjectDescription())
                .build();
    }
}
