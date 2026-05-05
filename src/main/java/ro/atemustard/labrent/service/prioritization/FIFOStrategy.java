package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

/**
 * FIFO (First In, First Out) strategy — for comparison with weighted scoring.
 *
 * FIFO does not rewrite a request's intrinsic priority score — a request's
 * merit (reputation, exam urgency, etc.) stays the same regardless of how the
 * operator chooses to serve the queue. The only thing FIFO changes is the
 * order in which the queue is displayed/served:
 * {@link ro.atemustard.labrent.service.PrioritizationService#getPrioritizedQueue}
 * reads the active strategy name and sorts by {@code createdAt ASC} when FIFO
 * is active.
 *
 * So this strategy delegates scoring to the weighted strategy: switching to
 * FIFO leaves the displayed score unchanged but flips the queue order to
 * strictly chronological.
 */
@Component("fifo")
public class FIFOStrategy implements PrioritizationStrategy {

    private final WeightedScoringStrategy weighted;

    public FIFOStrategy(WeightedScoringStrategy weighted) {
        this.weighted = weighted;
    }

    @Override
    public double calculatePriority(RentalRequest request, PrioritizationContext context) {
        return weighted.calculatePriority(request, context);
    }
}
