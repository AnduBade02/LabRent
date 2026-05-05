package ro.atemustard.labrent.service.observer;

import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;

/**
 * Design Pattern: OBSERVER
 *
 * Observer interface — listeners implement these methods to be notified about
 * rental request lifecycle events.
 *
 * NotificationService (the subject) keeps a listener list and notifies them.
 *
 * Note: Spring ships a native mechanism (@EventListener + ApplicationEventPublisher)
 * that does the same thing, but the manual variant keeps the pattern explicit.
 */
public interface RentalEventListener {

    void onRequestCreated(RentalRequest request);

    void onRequestApproved(RentalRequest request);

    void onRequestRejected(RentalRequest request);

    void onEquipmentReturned(RentalRequest request);

    void onAssessmentCompleted(ReturnAssessment assessment);
}
