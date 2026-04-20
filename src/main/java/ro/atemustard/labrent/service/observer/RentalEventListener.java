package ro.atemustard.labrent.service.observer;

import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;

/**
 * Design Pattern: OBSERVER
 *
 * Interfața Observer — ascultătorii implementează aceste metode pentru a fi
 * notificați despre evenimente din ciclul de viață al cererilor de închiriere.
 *
 * NotificationService (subiectul) ține o listă de listeneri și îi notifică.
 *
 * Notă: Spring oferă și un mecanism nativ (@EventListener + ApplicationEventPublisher)
 * care funcționează la fel, dar varianta manuală face pattern-ul mai vizibil.
 */
public interface RentalEventListener {

    void onRequestCreated(RentalRequest request);

    void onRequestApproved(RentalRequest request);

    void onRequestRejected(RentalRequest request);

    void onEquipmentReturned(RentalRequest request);

    void onAssessmentCompleted(ReturnAssessment assessment);
}
