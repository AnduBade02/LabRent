package ro.atemustard.labrent.model;

/**
 * Condition rating of the equipment at return time.
 * Filled in by the operator in the post-return verification form.
 *
 * Each rating directly impacts the user's reputation score:
 *   EXCELLENT = +5.0   (equipment in perfect condition)
 *   GOOD      = +2.0   (good condition, minimal wear)
 *   FAIR      =  0.0   (OK condition, normal wear)
 *   POOR      = -5.0   (minor issues, needs attention)
 *   DAMAGED   = -15.0  (damaged, needs repairs)
 */
public enum ConditionRating {

    EXCELLENT(5.0),
    GOOD(2.0),
    FAIR(0.0),
    POOR(-5.0),
    DAMAGED(-15.0);

    private final double reputationImpact;

    ConditionRating(double reputationImpact) {
        this.reputationImpact = reputationImpact;
    }

    public double getReputationImpact() {
        return reputationImpact;
    }
}
