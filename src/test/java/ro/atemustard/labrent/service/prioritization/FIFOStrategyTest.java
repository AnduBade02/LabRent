package ro.atemustard.labrent.service.prioritization;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;
import ro.atemustard.labrent.model.UserType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FIFOStrategyTest {

    @Test
    void delegatesScoreToWeightedStrategy() {
        WeightedScoringStrategy weighted = new WeightedScoringStrategy();
        FIFOStrategy fifo = new FIFOStrategy(weighted);

        User user = new User("u", "u@x.com", "p", null, UserType.NON_STUDENT);
        Equipment eq = new Equipment("Scope", "d", "cat", 1);
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(eq)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .build();
        PrioritizationContext ctx = new PrioritizationContext(1, 2, 100.0, false, null);

        assertThat(fifo.calculatePriority(r, ctx))
                .isEqualTo(weighted.calculatePriority(r, ctx));
    }
}
