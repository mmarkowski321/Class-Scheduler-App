package pl.projekt.backend.dto;

import lombok.Data;

@Data
public class TutorLessonProposalRequest {
    private String start;
    private Integer durationMinutes;
    private String note;
}




