package ro.atemustard.labrent.service.factory;

import ro.atemustard.labrent.dto.RentalRequestCreateDTO;
import ro.atemustard.labrent.model.Equipment;
import ro.atemustard.labrent.model.RentalRequest;
import ro.atemustard.labrent.model.User;

/**
 * Design Pattern: FACTORY
 *
 * Interfața Factory — definește contractul pentru crearea obiectelor RentalRequest.
 * Implementările concrete (Standard, Academic) încapsulează logica de creare
 * pentru diferite tipuri de cereri.
 */
public interface RentalRequestFactory {

    RentalRequest createRequest(User user, Equipment equipment, RentalRequestCreateDTO dto);
}
