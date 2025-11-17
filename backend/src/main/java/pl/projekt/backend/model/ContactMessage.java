package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Data
public class ContactMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String email;
    
    private String subject;
    
    @Column(nullable = false)
    private String message;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Fields used in code but not in DB schema
    @jakarta.persistence.Transient
    private Boolean replied = false;
    
    @jakarta.persistence.Transient
    private String adminReply;
    
    @jakarta.persistence.Transient
    private LocalDateTime repliedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
}

