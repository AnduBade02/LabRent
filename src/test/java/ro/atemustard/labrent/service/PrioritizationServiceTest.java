package ro.atemustard.labrent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.repository.RentalRequestRepository;
import ro.atemustard.labrent.service.prioritization.FIFOStrategy;
import ro.atemustard.labrent.service.prioritization.PrioritizationStrategy;
import ro.atemustard.labrent.service.prioritization.WeightedScoringStrategy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrioritizationServiceTest {

    @Mock
    private RentalRequestRepository rentalRequestRepository;

    private PrioritizationService service;
    private WeightedScoringStrategy weighted;
    private FIFOStrategy fifo;

    @BeforeEach
    void setUp() {
        weighted = new WeightedScoringStrategy();
        fifo = new FIFOStrategy(weighted);
        Map<String, PrioritizationStrategy> strategies = Map.of(
                "weightedScoring", weighted,
                "fifo", fifo
        );
        service = new PrioritizationService(strategies, rentalRequestRepository, "weightedScoring");
    }

    private RentalRequest sampleRequest() {
        User user = new User("u", "u@x.com", "p", Role.USER, UserType.STUDENT);
        user.setId(7L);
        Equipment eq = new Equipment("Scope", "d", "cat", 1);
        eq.setId(11L);
        RentalRequest r = RentalRequest.builder()
                .user(user).equipment(eq)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .build();
        r.setId(1L);
        return r;
    }

    @Test
    void calculatePriority_buildsContextFromRepoData() {
        RentalRequest r = sampleRequest();
        when(rentalRequestRepository.countByUserIdAndStatusIn(eq(7L), anyList())).thenReturn(2L);
        when(rentalRequestRepository.findByEquipmentIdAndStatus(11L, RequestStatus.PENDING))
                .thenReturn(List.of(r, r));

        double score = service.calculatePriority(r);
        // 50 base + 20 reputation + 5 student - 10 (2 active) = 65
        assertThat(score).isEqualTo(65.0);
    }

    @Test
    void setActiveStrategy_unknownThrows() {
        assertThatThrownBy(() -> service.setActiveStrategyName("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setActiveStrategy_acceptsKnownName() {
        service.setActiveStrategyName("fifo");
        assertThat(service.getActiveStrategyName()).isEqualTo("fifo");
    }

    @Test
    void getPrioritizedQueue_fifoUsesCreatedAtOrder() {
        service.setActiveStrategyName("fifo");
        when(rentalRequestRepository.findByEquipmentIdAndStatusOrderByCreatedAtAsc(99L, RequestStatus.PENDING))
                .thenReturn(List.of());

        service.getPrioritizedQueue(99L);

        verify(rentalRequestRepository).findByEquipmentIdAndStatusOrderByCreatedAtAsc(99L, RequestStatus.PENDING);
        verify(rentalRequestRepository, never())
                .findByEquipmentIdAndStatusOrderByPriorityScoreDescCreatedAtAsc(any(), any());
    }

    @Test
    void getPrioritizedQueue_weightedUsesPriorityScoreOrder() {
        when(rentalRequestRepository.findByEquipmentIdAndStatusOrderByPriorityScoreDescCreatedAtAsc(99L, RequestStatus.PENDING))
                .thenReturn(List.of());

        service.getPrioritizedQueue(99L);

        verify(rentalRequestRepository)
                .findByEquipmentIdAndStatusOrderByPriorityScoreDescCreatedAtAsc(99L, RequestStatus.PENDING);
    }

    @Test
    void recalculateForEquipment_setsScoresAndSaves() {
        RentalRequest r = sampleRequest();
        when(rentalRequestRepository.findByEquipmentIdAndStatus(11L, RequestStatus.PENDING))
                .thenReturn(List.of(r));
        when(rentalRequestRepository.countByUserIdAndStatusIn(eq(7L), anyList())).thenReturn(0L);

        service.recalculateForEquipment(11L);

        ArgumentCaptor<List<RentalRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(rentalRequestRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getPriorityScore()).isNotNull();
    }

    @Test
    void recalculateAllPending_processesAllPending() {
        RentalRequest r = sampleRequest();
        when(rentalRequestRepository.findByStatus(RequestStatus.PENDING)).thenReturn(List.of(r));
        when(rentalRequestRepository.countByUserIdAndStatusIn(eq(7L), anyList())).thenReturn(0L);
        when(rentalRequestRepository.findByEquipmentIdAndStatus(11L, RequestStatus.PENDING)).thenReturn(List.of(r));

        service.recalculateAllPending();

        verify(rentalRequestRepository).saveAll(anyList());
        assertThat(r.getPriorityScore()).isNotNull();
    }
}
