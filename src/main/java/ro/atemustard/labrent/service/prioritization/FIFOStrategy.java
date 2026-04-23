package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

import java.time.ZoneOffset;

/**
 * FIFO (First In, First Out) strategy — for comparison with the weighted one.
 *
 * The score is based purely on the request creation timestamp:
 * older requests get a higher score (processed first).
 */
@Component("fifo")
public class FIFOStrategy implements PrioritizationStrategy {

    @Override
    public double calculatePriority(RentalRequest request, PrioritizationContext context) {
        if (request.getCreatedAt() == null) {
            return 0.0;
        }
        // Earlier requests get higher scores
        long epochSecond = request.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
        return Double.MAX_VALUE / 2.0 - epochSecond;
    }
}
