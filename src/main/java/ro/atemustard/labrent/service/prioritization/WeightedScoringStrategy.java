package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Weighted-scoring prioritization strategy.
 *
 * Factors (all composed into a single stable score):
 * - Base:             50 points
 * - Reputation:       0..40 points (reputationScore / 100 * 20, capped at 40)
 * - Active requests:  -5 points per active request (discourages hoarding)
 * - Student bonus:    +5 points if the user is a student
 * - Exam urgency:     0..30 points (the closer the exam, the higher the score)
 *
 * Ties are broken at query time by {@code ORDER BY ..., createdAt ASC}
 * (older request wins), keeping the score itself stable across recalculations.
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

        return score;
    }
}
