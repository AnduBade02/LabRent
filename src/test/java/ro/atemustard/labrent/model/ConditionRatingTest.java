package ro.atemustard.labrent.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionRatingTest {

    @Test
    void reputationImpactsAreOrderedFromBestToWorst() {
        assertThat(ConditionRating.EXCELLENT.getReputationImpact()).isEqualTo(5.0);
        assertThat(ConditionRating.GOOD.getReputationImpact()).isEqualTo(2.0);
        assertThat(ConditionRating.FAIR.getReputationImpact()).isEqualTo(0.0);
        assertThat(ConditionRating.POOR.getReputationImpact()).isEqualTo(-5.0);
        assertThat(ConditionRating.DAMAGED.getReputationImpact()).isEqualTo(-15.0);

        assertThat(ConditionRating.EXCELLENT.getReputationImpact())
                .isGreaterThan(ConditionRating.GOOD.getReputationImpact());
        assertThat(ConditionRating.POOR.getReputationImpact())
                .isGreaterThan(ConditionRating.DAMAGED.getReputationImpact());
    }
}
