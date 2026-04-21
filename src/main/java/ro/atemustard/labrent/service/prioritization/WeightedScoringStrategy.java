package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Strategie de prioritizare cu scor ponderat.
 *
 * Factori:
 * - Reputație:        0–40 puncte (reputationScore / 100 * 20, max 40)
 * - Cereri active:    -5 puncte per cerere activă (descurajează acapararea)
 * - Bonus student:    +5 puncte dacă e student
 * - Urgență examen:   0–30 puncte (cu cât examenul e mai aproape, cu atât scor mai mare)
 * - Cerere timpurie:  tiebreaker mic bazat pe timestamp (prima cerere câștigă la egalitate)
 *
 * Scor final = 50 (bază) + reputație + active penalty + student bonus + exam urgency
 */
@Component("weightedScoring")
public class WeightedScoringStrategy implements PrioritizationStrategy {

    @Override
    public double calculatePriority(RentalRequest request, PrioritizationContext context) {
        double score = 50.0;

        // Reputation factor: max 40 points
        score += Math.min(context.getReputationScore() / 100.0 * 20.0, 40.0);

        // Active requests penalty
        score -= 5.0 * context.getActiveRequestCount();

        // Student bonus
        if (context.isStudent()) {
            score += 5.0;
        }

        // Exam urgency: up to 30 points as exam date approaches
        if (context.getExamDate() != null) {
            long daysUntilExam = ChronoUnit.DAYS.between(LocalDate.now(), context.getExamDate());
            if (daysUntilExam >= 0 && daysUntilExam <= 30) {
                score += (30.0 - daysUntilExam);
            }
        }

        // Tiebreaker: earlier requests get a tiny bonus (older = higher)
        if (request.getCreatedAt() != null) {
            long ageSeconds = ChronoUnit.SECONDS.between(
                    request.getCreatedAt(), java.time.LocalDateTime.now());
            score += 0.0001 * Math.max(0, ageSeconds);
        }

        return score;
    }
}
