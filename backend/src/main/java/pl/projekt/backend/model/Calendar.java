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
    
    @Column(nullable = false, length = 1000)
    private String calendarUrl;
    
    @Column(length = 255)
    private String name; // Optional name for the calendar (e.g., "Work Calendar", "Personal")
    
    @Column(nullable = false)
    private Boolean active = true; // Can be disabled without deleting
}

