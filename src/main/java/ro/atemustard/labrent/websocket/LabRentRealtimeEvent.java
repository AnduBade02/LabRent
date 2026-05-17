package ro.atemustard.labrent.websocket;

import ro.atemustard.labrent.model.RentalRequest;

import java.time.Instant;

public record LabRentRealtimeEvent(
        String type,
        String action,
        Long requestId,
        Long userId,
        String username,
        Long equipmentId,
        String equipmentName,
        Instant emittedAt
) {

    public static LabRentRealtimeEvent rentalRequest(String action, RentalRequest request) {
        return new LabRentRealtimeEvent(
                "RENTAL_REQUEST",
                action,
                request.getId(),
                request.getUser().getId(),
                request.getUser().getUsername(),
                request.getEquipment().getId(),
                request.getEquipment().getName(),
                Instant.now());
    }
}
