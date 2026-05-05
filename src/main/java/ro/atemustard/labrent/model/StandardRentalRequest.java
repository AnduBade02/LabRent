package ro.atemustard.labrent.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * StandardRentalRequest — concrete request without academic urgency.
 *
 * Generalization (UML): extends RentalRequest. Carries no extra fields;
 * the type is significant for prioritization (no exam bonus) and for
 * the discriminator-driven persistence layout.
 */
@Entity
@DiscriminatorValue("STANDARD")
public class StandardRentalRequest extends RentalRequest {

    public StandardRentalRequest() {
        super();
    }

    public StandardRentalRequest(User user, Equipment equipment,
                                 LocalDate startDate, LocalDate endDate,
                                 String projectDescription) {
        super(user, equipment, startDate, endDate, projectDescription);
    }
}
