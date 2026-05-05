package ro.atemustard.labrent.service.prioritization;

import java.time.LocalDate;

/**
 * Pre-computed data required by prioritization strategies.
 * Avoids each strategy issuing its own DB queries.
 */
public class PrioritizationContext {

    private final int activeRequestCount;
    private final int competingRequestCount;
    private final double reputationScore;
    private final boolean isStudent;
    private final LocalDate examDate;

    public PrioritizationContext(int activeRequestCount, int competingRequestCount,
                                 double reputationScore, boolean isStudent, LocalDate examDate) {
        this.activeRequestCount = activeRequestCount;
        this.competingRequestCount = competingRequestCount;
        this.reputationScore = reputationScore;
        this.isStudent = isStudent;
        this.examDate = examDate;
    }

    public int getActiveRequestCount() { return activeRequestCount; }
    public int getCompetingRequestCount() { return competingRequestCount; }
    public double getReputationScore() { return reputationScore; }
    public boolean isStudent() { return isStudent; }
    public LocalDate getExamDate() { return examDate; }
}
