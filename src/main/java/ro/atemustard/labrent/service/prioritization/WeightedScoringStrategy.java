package ro.atemustard.labrent.service.prioritization;

import org.springframework.stereotype.Component;
import ro.atemustard.labrent.model.RentalRequest;

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
 * - Waiting age:      +0.5 points per waiting day, capped at 15
 * - Retry boost:      +3 points per similar rejected request, capped at 9
 *
 * Ties are broken at query time by {@code ORDER BY ..., createdAt ASC}
 * (older request wins), keeping the score itself stable across recalculations.
 */
@Component("weightedScoring")
public class WeightedScoringStrategy implements PrioritizationStrategy {

    private static final double WAITING_DAY_WEIGHT = 0.5;
    private static final double MAX_WAITING_AGE_BONUS = 15.0;
    private static final double REJECTED_RETRY_WEIGHT = 3.0;
    private static final double MAX_REJECTED_RETRY_BONUS = 9.0;

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

        score += Math.min(context.getWaitingDays() * WAITING_DAY_WEIGHT, MAX_WAITING_AGE_BONUS);

        score += Math.min(context.getPreviousRejectedSimilarCount() * REJECTED_RETRY_WEIGHT,
                MAX_REJECTED_RETRY_BONUS);

        // Exam urgency: up to 30 points as exam date approaches
        if (context.getExamDate() != null) {
            long daysUntilExam = ChronoUnit.DAYS.between(context.getCurrentDate(), context.getExamDate());
            if (daysUntilExam >= 0 && daysUntilExam <= 30) {
                score += (30.0 - daysUntilExam);
            }
        }

        return score;
    }
}
