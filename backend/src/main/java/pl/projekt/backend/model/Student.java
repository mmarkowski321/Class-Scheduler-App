package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("STUDENT")
@Data
public class Student extends User {
    
    // Profile fields from registration form
    private String school;
    private String grade;
    private String track;
    private String phone;
    private String languages;
    private String timezone;
    
    // Additional profile fields (for profile completion later)
    private String aboutMe;
    private String goals;
    private String strengths;
    private String difficulties;
    private String preferredSubjects;
    private String avoidSubjects;
    private String learningStyle;
    private String city;
    private String meetingMode;
    private String preferredTools;
    private String otherTool;
    private String preferredDays;
    private String availabilityNote;
    private String guardianName;
    private String guardianEmail;
    private Boolean shareProfile;
}

