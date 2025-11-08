package pl.projekt.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "app_users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Data
public abstract class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private LocalDate birthDate;
    
    private String createdAt;
    
    // Email verification fields
    @Column(nullable = false)
    private Boolean emailVerified = false;
    
    private String verificationToken;

    // Password reset fields
    private String resetPasswordToken;
    private String resetPasswordTokenExpiry; // ISO string timestamp
    
    // Ban status - banned users cannot login or register again with same email
    @Column(nullable = false)
    private Boolean banned = false;
    
    // Email language preference (pl or en)
    @Column(nullable = false)
    private String emailLanguage = "pl";
    
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

