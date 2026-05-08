package ro.atemustard.labrent.service.prioritization;

import java.time.LocalDate;

/**
 * Pre-computed data required by prioritization strategies.
 * Avoids each strategy issuing its own DB queries.
 */
public class PrioritizationContext {

    private final int activeRequestCount;
    private final int competingRequestCount;
    private final int waitingDays;
    private final int previousRejectedSimilarCount;
    private final double reputationScore;
    private final boolean isStudent;
    private final LocalDate examDate;
    private final LocalDate currentDate;

    public PrioritizationContext(int activeRequestCount, int competingRequestCount,
                                 double reputationScore, boolean isStudent, LocalDate examDate) {
        this(activeRequestCount, competingRequestCount, 0, 0,
                reputationScore, isStudent, examDate, LocalDate.now());
    }

    public PrioritizationContext(int activeRequestCount, int competingRequestCount,
                                 int waitingDays, int previousRejectedSimilarCount,
                                 double reputationScore, boolean isStudent, LocalDate examDate) {
        this(activeRequestCount, competingRequestCount, waitingDays, previousRejectedSimilarCount,
                reputationScore, isStudent, examDate, LocalDate.now());
    }

    public PrioritizationContext(int activeRequestCount, int competingRequestCount,
                                 int waitingDays, int previousRejectedSimilarCount,
                                 double reputationScore, boolean isStudent, LocalDate examDate,
                                 LocalDate currentDate) {
        this.activeRequestCount = activeRequestCount;
        this.competingRequestCount = competingRequestCount;
        this.waitingDays = waitingDays;
        this.previousRejectedSimilarCount = previousRejectedSimilarCount;
        this.reputationScore = reputationScore;
        this.isStudent = isStudent;
        this.examDate = examDate;
        this.currentDate = currentDate;
    }

    public int getActiveRequestCount() { return activeRequestCount; }
    public int getCompetingRequestCount() { return competingRequestCount; }
    public int getWaitingDays() { return waitingDays; }
    public int getPreviousRejectedSimilarCount() { return previousRejectedSimilarCount; }
    public double getReputationScore() { return reputationScore; }
    public boolean isStudent() { return isStudent; }
    public LocalDate getExamDate() { return examDate; }
    public LocalDate getCurrentDate() { return currentDate; }
}
