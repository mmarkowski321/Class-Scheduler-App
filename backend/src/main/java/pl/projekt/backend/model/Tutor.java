package pl.projekt.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@DiscriminatorValue("TUTOR")
@Data
@lombok.EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Tutor extends User {
    
    // Basic profile fields
    @Column(name = "tutor_education")
    private String education;
    
    @Column(name = "tutor_experience_years")
    private Integer experienceYears;
    
    @Column(name = "tutor_photo_url")
    private String photoUrl;
    
    // Offer fields
    @Column(name = "tutor_subjects")
    private String subjects;
    
    @Column(name = "tutor_exam_results")
    private String examResults;
    
    @Column(name = "tutor_hourly_rate", columnDefinition = "NUMERIC(12,2)")
    private Double hourlyRate;
    
    @Column(name = "tutor_lesson_duration")
    private Integer lessonDuration;
    
    @Column(name = "tutor_teaching_languages")
    private String teachingLanguages;
    
    @Column(name = "tutor_lesson_modes")
    private String lessonModes; // online, onsite, hybrid
    
    @Column(name = "tutor_city")
    private String city;
    
    @Column(name = "tutor_travel_radius")
    private Integer travelRadius;
    
    // Methods & bio
    @Column(name = "tutor_teaching_methods")
    private String teachingMethods;
    
    @Column(name = "tutor_bio")
    private String bio;
    
    @Column(name = "tutor_certificates")
    private String certificates;
    
    // Links & availability
    @Column(name = "tutor_website")
    private String website;
    
    @Column(name = "tutor_linkedin")
    private String linkedIn;
    
    @Column(name = "tutor_max_lessons_per_day")
    private Integer maxLessonsPerDay;
    
    @Column(name = "tutor_buffer_time")
    private Integer bufferTime;
    
    @Column(name = "tutor_preferred_days")
    private String preferredDays;

    // Auto-accept booking when slot is free
    @Column(name = "auto_accept_bookings")
    private Boolean autoAcceptBookings = false;
}

