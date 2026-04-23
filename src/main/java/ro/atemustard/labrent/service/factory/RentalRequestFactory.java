package ro.atemustard.labrent.service.factory;

import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;

/**
 * Design Pattern: FACTORY
 *
 * Factory interface — contract for building RentalRequest objects.
 * Concrete implementations (Standard, Academic) encapsulate the creation logic
 * for different request types.
 */
public interface RentalRequestFactory {

    RentalRequest createRequest(User user, Equipment equipment, RentalRequestCreateDTO dto);
}
