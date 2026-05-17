package ro.atemustard.labrent.service.observer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.ReturnAssessment;
import ro.atemustard.labrent.websocket.LabRentRealtimeEvent;
import ro.atemustard.labrent.websocket.LabRentWebSocketHandler;

@Component
public class RealtimeRentalEventListener implements RentalEventListener {

    private final LabRentWebSocketHandler webSocketHandler;

    public RealtimeRentalEventListener(LabRentWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void onRequestCreated(RentalRequest request) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest("created", request));
    }

    @Override
    public void onRequestApproved(RentalRequest request) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest("approved", request));
    }

    @Override
    public void onRequestRejected(RentalRequest request) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest("rejected", request));
    }

    @Override
    public void onRequestRented(RentalRequest request) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest("rented", request));
    }

    @Override
    public void onEquipmentReturned(RentalRequest request) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest("returned", request));
    }

    @Override
    public void onAssessmentCompleted(ReturnAssessment assessment) {
        publishAfterCommit(LabRentRealtimeEvent.rentalRequest(
                "assessment_completed",
                assessment.getRentalRequest()));
    }

    private void publishAfterCommit(LabRentRealtimeEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            webSocketHandler.broadcast(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                webSocketHandler.broadcast(event);
            }
        });
    }
}
