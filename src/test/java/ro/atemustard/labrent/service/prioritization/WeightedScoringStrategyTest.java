package ro.atemustard.labrent.service.prioritization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedScoringStrategyTest {

    private WeightedScoringStrategy strategy;
    private RentalRequest request;

    @BeforeEach
    void setUp() {
        strategy = new WeightedScoringStrategy();
        User user = new User("u", "u@x.com", "p", null, UserType.STUDENT);
        Equipment eq = new Equipment("Scope", "d", "cat", 1);
        request = RentalRequest.builder()
                .user(user)
                .equipment(eq)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();
    }

    @Test
    void baselineScore_nonStudent_neutralReputation() {
        // base 50 + reputation (100/100 * 20 = 20)
        PrioritizationContext ctx = new PrioritizationContext(0, 0, 100.0, false, null);
        assertThat(strategy.calculatePriority(request, ctx)).isEqualTo(70.0);
    }

    @Test
    void studentBonusAddsFivePoints() {
        PrioritizationContext nonStudent = new PrioritizationContext(0, 0, 100.0, false, null);
        PrioritizationContext student = new PrioritizationContext(0, 0, 100.0, true, null);

        double diff = strategy.calculatePriority(request, student)
                - strategy.calculatePriority(request, nonStudent);
        assertThat(diff).isEqualTo(5.0);
    }

    @Test
    void activeRequestsPenalty_fivePerActive() {
        PrioritizationContext ctx0 = new PrioritizationContext(0, 0, 100.0, false, null);
        PrioritizationContext ctx3 = new PrioritizationContext(3, 0, 100.0, false, null);
        assertThat(strategy.calculatePriority(request, ctx0)
                - strategy.calculatePriority(request, ctx3)).isEqualTo(15.0);
    }

    @Test
    void reputationFactorIsCappedAtForty() {
        // 200 reputation would map to 40 (capped); 1000 reputation also caps at 40
        PrioritizationContext capped = new PrioritizationContext(0, 0, 1000.0, false, null);
        PrioritizationContext at200 = new PrioritizationContext(0, 0, 200.0, false, null);
        assertThat(strategy.calculatePriority(request, capped))
                .isEqualTo(strategy.calculatePriority(request, at200));
    }

    @Test
    void examUrgency_closerExamScoresHigher() {
        LocalDate inOneDay = LocalDate.now().plusDays(1);
        LocalDate inTwentyDays = LocalDate.now().plusDays(20);

        PrioritizationContext close = new PrioritizationContext(0, 0, 100.0, true, inOneDay);
        PrioritizationContext far = new PrioritizationContext(0, 0, 100.0, true, inTwentyDays);

        assertThat(strategy.calculatePriority(request, close))
                .isGreaterThan(strategy.calculatePriority(request, far));
    }

    @Test
    void examUrgency_zeroBeyondThirtyDays() {
        LocalDate withinWindow = LocalDate.now().plusDays(30);
        LocalDate outsideWindow = LocalDate.now().plusDays(60);

        PrioritizationContext within = new PrioritizationContext(0, 0, 100.0, false, withinWindow);
        PrioritizationContext outside = new PrioritizationContext(0, 0, 100.0, false, outsideWindow);

        // within window adds (30 - 30) = 0; outside adds 0; expect equal
        assertThat(strategy.calculatePriority(request, within))
                .isEqualTo(strategy.calculatePriority(request, outside));
    }

    @Test
    void examInPast_noBonus() {
        LocalDate past = LocalDate.now().minusDays(1);
        PrioritizationContext ctx = new PrioritizationContext(0, 0, 100.0, false, past);
        // baseline = 70.0, exam in past contributes 0
        assertThat(strategy.calculatePriority(request, ctx)).isEqualTo(70.0);
    }

    @Test
    void zeroReputation_addsNoReputationPoints() {
        PrioritizationContext ctx = new PrioritizationContext(0, 0, 0.0, false, null);
        assertThat(strategy.calculatePriority(request, ctx)).isEqualTo(50.0);
    }
}
