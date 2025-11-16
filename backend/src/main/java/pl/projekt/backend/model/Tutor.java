package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@DiscriminatorValue("TUTOR")
@Data
public class Tutor extends User {
    
    // Basic profile fields
    private String education;
    private Integer experienceYears;
    private String photoUrl;
    
    // Offer fields
    private String subjects;
    private String examResults;
    private Double hourlyRate;
    private Integer lessonDuration;
    private String teachingLanguages;
    private String lessonModes; // online, onsite, hybrid
    private String city;
    private Integer travelRadius;
    
    // Methods & bio
    private String teachingMethods;
    private String bio;
    private String certificates;
    
    // Links & availability
    private String website;
    private String linkedIn;
    private Integer maxLessonsPerDay;
    private Integer bufferTime;
    private String preferredDays;

    // Auto-accept booking when slot is free
    @Column(name = "auto_accept_bookings")
    private Boolean autoAcceptBookings = false;
}

