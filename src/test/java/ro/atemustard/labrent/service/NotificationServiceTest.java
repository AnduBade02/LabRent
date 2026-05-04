package ro.atemustard.labrent.service;

import org.junit.jupiter.api.Test;
import ro.atemustard.labrent.model.*;
import ro.atemustard.labrent.service.observer.RentalEventListener;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void notifiesAllRegisteredListeners() {
        RentalEventListener a = mock(RentalEventListener.class);
        RentalEventListener b = mock(RentalEventListener.class);
        NotificationService service = new NotificationService(List.of(a, b));

        RentalRequest r = RentalRequest.builder()
                .user(new User("u", "u@x.com", "p", Role.USER, UserType.STUDENT))
                .equipment(new Equipment("Scope", "d", "cat", 1))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .build();

        service.notifyRequestCreated(r);
        service.notifyRequestApproved(r);
        service.notifyRequestRejected(r);
        service.notifyEquipmentReturned(r);

        verify(a).onRequestCreated(r);
        verify(a).onRequestApproved(r);
        verify(a).onRequestRejected(r);
        verify(a).onEquipmentReturned(r);
        verify(b).onRequestCreated(r);
        verify(b).onRequestApproved(r);
        verify(b).onRequestRejected(r);
        verify(b).onEquipmentReturned(r);
    }
}
