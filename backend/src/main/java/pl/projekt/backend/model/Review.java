package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;
    
    @Column(name = "tutor_rating")
    private Integer tutorRating; // Rating for tutor (1-5)
    
    @Column(name = "platform_rating")
    private Integer platformRating; // Rating for platform (1-5)
    
    @Column(name = "comment", length = 400)
    private String comment;
    
    @Column(name = "student_review_at")
    private LocalDateTime studentReviewAt;
    
    @Column(name = "student_behavior_rating")
    private Integer studentBehaviorRating; // Rating for student behavior by tutor (1-5, nullable, visible only to tutors and admins)

    @Column(name = "tutor_platform_rating")
    private Integer tutorPlatformRating; // Tutor feedback about platform

    @Column(name = "tutor_comment", length = 400)
    private String tutorComment; // Tutor written feedback about student / platform

    @Column(name = "tutor_review_at")
    private LocalDateTime tutorReviewAt;
}

