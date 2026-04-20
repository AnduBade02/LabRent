package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

import java.time.ZoneOffset;

/**
 * Strategie FIFO (First In, First Out) — pentru comparație cu strategia ponderată.
 *
 * Scorul e bazat exclusiv pe momentul creării cererii:
 * cererile mai vechi primesc scor mai mare (sunt procesate primele).
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
