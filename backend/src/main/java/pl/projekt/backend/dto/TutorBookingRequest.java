package pl.projekt.backend.dto;

import lombok.Data;

@Data
public class TutorBookingRequest {
    private String start;
    private String end;
    private Integer durationMinutes;
    private String notes;
    private String deliveryMode;
    private String onsiteCity;
    private String onsitePostalCode;
    private String onsiteStreet;
    private String onsiteBuilding;
    private String onsiteApartment;
}


