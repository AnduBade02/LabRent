package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;
import ro.atemustard.labrent.service.observer.RentalEventListener;

import java.util.List;

/**
 * Subject of the Observer pattern — holds the listener list and notifies them.
 *
 * Spring auto-injects every bean that implements RentalEventListener
 * (currently EmailNotificationListener). Adding a SlackNotificationListener
 * later requires no changes to this service.
 */
@Service
public class NotificationService {

    private final List<RentalEventListener> listeners;

    public NotificationService(List<RentalEventListener> listeners) {
        this.listeners = listeners;
    }

    public void notifyRequestCreated(RentalRequest request) {
        listeners.forEach(l -> l.onRequestCreated(request));
    }

    public void notifyRequestApproved(RentalRequest request) {
        listeners.forEach(l -> l.onRequestApproved(request));
    }

    public void notifyRequestRejected(RentalRequest request) {
        listeners.forEach(l -> l.onRequestRejected(request));
    }

    public void notifyEquipmentReturned(RentalRequest request) {
        listeners.forEach(l -> l.onEquipmentReturned(request));
    }

    public void notifyAssessmentCompleted(ReturnAssessment assessment) {
        listeners.forEach(l -> l.onAssessmentCompleted(assessment));
    }
}
