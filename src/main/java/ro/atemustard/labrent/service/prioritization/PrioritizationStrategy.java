package ro.atemustard.labrent.service.prioritization;

import ro.atemustard.labrent.model.RentalRequest;

/**
 * Design Pattern: STRATEGY
 *
 * Interfața definește contractul pentru algoritmii de prioritizare.
 * Implementările concrete (WeightedScoringStrategy, FIFOStrategy) oferă
 * algoritmi diferiți, iar PrioritizationService poate comuta între ei la runtime.
 *
 * Echivalentul Spring al acestui pattern: diferite @Component-uri care
 * implementează aceeași interfață, injectate într-un Map<String, PrioritizationStrategy>.
 */
public interface PrioritizationStrategy {

    double calculatePriority(RentalRequest request, PrioritizationContext context);
}
