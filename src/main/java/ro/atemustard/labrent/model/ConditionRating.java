package ro.atemustard.labrent.model;

/**
 * Rating-ul condiției echipamentului la returnare.
 * Completat de operator în formularul de verificare post-returnare.
 *
 * Fiecare rating are un impact direct asupra scorului de reputație al utilizatorului:
 *   EXCELLENT = +5.0   (echipament în stare perfectă)
 *   GOOD      = +2.0   (echipament în stare bună, uzură minimă)
 *   FAIR      =  0.0   (echipament OK, uzură normală)
 *   POOR      = -5.0   (probleme minore, necesită atenție)
 *   DAMAGED   = -15.0  (echipament deteriorat, necesită reparații)
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
