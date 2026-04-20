package ro.atemustard.labrent.service;

import org.springframework.stereotype.Service;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;
import ro.atemustard.labrent.service.observer.RentalEventListener;

import java.util.List;

/**
 * Subiectul din Observer pattern — deține lista de listeneri și îi notifică.
 *
 * Spring injectează automat toți bean-urile care implementează RentalEventListener
 * (în cazul nostru, EmailNotificationListener). Dacă adăugăm un SlackNotificationListener
 * în viitor, va fi automat inclus fără a modifica acest service.
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
