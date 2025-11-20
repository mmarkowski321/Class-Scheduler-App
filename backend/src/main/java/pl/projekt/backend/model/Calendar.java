package pl.projekt.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "calendars")
@Data
public class Calendar {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @Column(name = "calendar_url", nullable = false)
    private String calendarUrl;
    
    @Column(nullable = false)
    private Boolean active = true; // Can be disabled without deleting
    
    // Optional name for the calendar
    @Column(name = "name", length = 255)
    private String name;
}

