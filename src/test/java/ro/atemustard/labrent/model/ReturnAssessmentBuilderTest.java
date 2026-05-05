package ro.atemustard.labrent.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReturnAssessmentBuilderTest {

    private RentalRequest sampleRequest() {
        return RentalRequest.builder()
                .user(new User("u", "u@x.com", "p", Role.USER, UserType.STUDENT))
                .equipment(new Equipment("Scope", "d", "cat", 1))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();
    }

    @Test
    void build_copiesReputationImpactFromRating() {
        User op = new User("op", "op@x.com", "p", Role.ADMIN, UserType.NON_STUDENT);
        ReturnAssessment a = ReturnAssessment.builder()
                .rentalRequest(sampleRequest())
                .operator(op)
                .conditionRating(ConditionRating.EXCELLENT)
                .notes("perfect")
                .build();

        assertThat(a.getReputationImpact()).isEqualTo(ConditionRating.EXCELLENT.getReputationImpact());
        assertThat(a.getOperator()).isSameAs(op);
        assertThat(a.getNotes()).isEqualTo("perfect");
    }

    @Test
    void build_missingRating_throws() {
        User op = new User("op", "op@x.com", "p", Role.ADMIN, UserType.NON_STUDENT);
        assertThatThrownBy(() -> ReturnAssessment.builder()
                .rentalRequest(sampleRequest())
                .operator(op)
                .build()
        ).isInstanceOf(IllegalStateException.class);
    }
}
