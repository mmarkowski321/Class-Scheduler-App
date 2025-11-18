package pl.projekt.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "app_users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Email verification fields
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;

    // Password reset fields
    @Column(name = "reset_password_token")
    private String resetPasswordToken;
    
    @Column(name = "reset_password_token_expiry")
    private String resetPasswordTokenExpiry; // ISO string timestamp
    
    // Ban status - banned users cannot login or register again with same email
    @Column(nullable = false)
    private Boolean banned = false;
    
    // Email language preference (pl or en)
    @Column(name = "email_language", nullable = false)
    private String emailLanguage = "pl";

    // Notification preferences
    @Column(name = "pref_email_notifications")
    private Boolean emailNotifications = true;
    @Column(name = "pref_booking_reminders")
    private Boolean bookingReminders = true;
    @Column(name = "pref_lesson_reminders")
    private Boolean lessonReminders = true;
    @Column(name = "pref_change_notifications")
    private Boolean changeNotifications = true;
    
    // Relationship with calendars (one-to-many)
    // Ignored in JSON serialization to avoid circular references and lazy loading issues
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Calendar> calendars;
    
    public void verifyEmail() {
        this.emailVerified = true;
        this.verificationToken = null;
    }

    public void clearResetToken() {
        this.resetPasswordToken = null;
        this.resetPasswordTokenExpiry = null;
    }
    
    public void ban() {
        this.banned = true;
    }
    
    public void unban() {
        this.banned = false;
    }
}

