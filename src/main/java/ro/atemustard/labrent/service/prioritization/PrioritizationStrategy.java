package ro.atemustard.labrent.service.prioritization;

import ro.atemustard.labrent.model.RentalRequest;

/**
 * Design Pattern: STRATEGY
 *
 * Contract for prioritization algorithms. Concrete implementations
 * (WeightedScoringStrategy, FIFOStrategy) provide different algorithms, and
 * PrioritizationService can switch between them at runtime.
 *
 * Spring equivalent of this pattern: multiple @Components implementing the same
 * interface, injected into a Map<String, PrioritizationStrategy>.
 */
public interface PrioritizationStrategy {

    double calculatePriority(RentalRequest request, PrioritizationContext context);
}
