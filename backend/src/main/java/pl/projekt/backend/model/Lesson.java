package pl.projekt.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Data
public class Lesson {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status = LessonStatus.REQUESTED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false)
    private LessonDeliveryMode deliveryMode;

    @Column(name = "meeting_link")
    private String meetingLink; // Meeting link (Google Meet, Zoom, Teams, etc.)
    
    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "notes")
    private String notes; // Additional notes
    
    // Onsite location fields (for ONSITE delivery mode)
    @Column(name = "onsite_city")
    private String onsiteCity;
    
    @Column(name = "onsite_postal_code")
    private String onsitePostalCode;
    
    @Column(name = "onsite_street")
    private String onsiteStreet;
    
    @Column(name = "onsite_building")
    private String onsiteBuilding;
    
    @Column(name = "onsite_apartment")
    private String onsiteApartment;
    
    // Proposed reschedule (awaiting confirmation by the other party)
    @Column(name = "proposed_start_time")
    private LocalDateTime proposedStartTime;
    
    @Column(name = "proposed_end_time")
    private LocalDateTime proposedEndTime;
    
    @Column(name = "proposed_by") // "STUDENT" or "TUTOR"
    private String proposedBy;
    
    @Column(name = "proposal_notes")
    private String proposalNotes;
    
    @Column(name = "proposal_created_at")
    private LocalDateTime proposalCreatedAt;
    
    @Column(name = "proposal_accepted_at")
    private LocalDateTime proposalAcceptedAt;
    
    @Column(name = "proposal_rejected_at")
    private LocalDateTime proposalRejectedAt;

    // Reminder tracking
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

