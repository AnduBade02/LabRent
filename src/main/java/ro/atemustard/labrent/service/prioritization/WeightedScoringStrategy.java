package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Weighted-scoring prioritization strategy.
 *
 * Factors:
 * - Reputation:       0–40 points (reputationScore / 100 * 20, capped at 40)
 * - Active requests:  -5 points per active request (discourages hoarding)
 * - Student bonus:    +5 points if the user is a student
 * - Exam urgency:     0–30 points (the closer the exam, the higher the score)
 * - Earlier request:  small timestamp-based tiebreaker (older wins on ties)
 *
 * Final score = 50 (base) + reputation + active penalty + student bonus + exam urgency
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
