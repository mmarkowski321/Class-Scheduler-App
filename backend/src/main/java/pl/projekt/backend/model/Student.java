package pl.projekt.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("STUDENT")
@Data
@lombok.EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student extends User {
    
    // Profile fields from registration form
    @Column(name = "student_school")
    private String school;
    
    @Column(name = "student_grade")
    private String grade;
    
    @Column(name = "student_track")
    private String track;
    
    @Column(name = "student_phone")
    private String phone;
    
    @Column(name = "student_languages")
    private String languages;
    
    @Column(name = "student_timezone")
    private String timezone;
    
    // Additional profile fields (for profile completion later)
    @Column(name = "student_about_me")
    private String aboutMe;
    
    @Column(name = "student_goals")
    private String goals;
    
    @Column(name = "student_strengths")
    private String strengths;
    
    @Column(name = "student_difficulties")
    private String difficulties;
    
    @Column(name = "student_preferred_subjects")
    private String preferredSubjects;
    
    @Column(name = "student_avoid_subjects")
    private String avoidSubjects;
    
    @Column(name = "student_learning_style")
    private String learningStyle;
    
    @Column(name = "student_city")
    private String city;
    
    @Column(name = "student_meeting_mode")
    private String meetingMode;
    
    @Column(name = "student_preferred_tools")
    private String preferredTools;
    
    @Column(name = "student_other_tool")
    private String otherTool;
    
    @Column(name = "student_preferred_days")
    private String preferredDays;
    
    @Column(name = "student_availability_note")
    private String availabilityNote;
    
    @Column(name = "student_guardian_name")
    private String guardianName;
    
    @Column(name = "student_guardian_email")
    private String guardianEmail;
    
    @Column(name = "student_share_profile")
    private Boolean shareProfile;

    @Column(name = "student_photo_url")
    private String photoUrl;
}

