package pl.projekt.backend.dto;

import lombok.Data;

@Data
public class TutorBookingRequest {
    private String start;
    private String end;
    private Integer durationMinutes;
    private String notes;
}


