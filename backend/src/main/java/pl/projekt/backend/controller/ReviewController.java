package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.config.JwtPrincipal;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private LessonRepository lessonRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> request) {
        // Check authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Authentication required"));
        }
        
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        Long authenticatedUserId = principal.userId;
        try {
            Long lessonId = Long.valueOf(request.get("lessonId").toString());
            Long studentId = Long.valueOf(request.get("studentId").toString());
            Integer tutorRating = Integer.valueOf(request.get("tutorRating").toString());
            Integer platformRating = Integer.valueOf(request.get("platformRating").toString());
            String comment = (String) request.get("comment");
            
            // Validate ratings
            if (tutorRating < 1 || tutorRating > 5 || platformRating < 1 || platformRating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }
            
            // Check if lesson exists
            Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
            if (lessonOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Lesson not found"));
            }
            
            Lesson lesson = lessonOpt.get();
            
            // Get student and tutor
            var studentOpt = userRepository.findById(studentId);
            var tutorOpt = userRepository.findById(lesson.getTutor().getId());
            
            if (studentOpt.isEmpty() || !(studentOpt.get() instanceof Student)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid student"));
            }
            
            if (tutorOpt.isEmpty() || !(tutorOpt.get() instanceof Tutor)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid tutor"));
            }
            
            Student student = (Student) studentOpt.get();
            Tutor tutor = (Tutor) tutorOpt.get();
            
            // Verify that the authenticated user is the student
            if (!student.getId().equals(authenticatedUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only submit reviews for your own lessons"));
            }
            
            // Verify that the lesson belongs to this student
            if (!lesson.getStudent().getId().equals(studentId)) {
                return ResponseEntity.status(403).body(Map.of("error", "This lesson does not belong to you"));
            }
            
            // Verify that the lesson is completed
            if (lesson.getStatus() != pl.projekt.backend.model.LessonStatus.COMPLETED) {
                return ResponseEntity.badRequest().body(Map.of("error", "You can only review completed lessons"));
            }
            
            // Create or update review
            Review review = reviewRepository.findByLessonId(lessonId)
                    .orElseGet(() -> {
                        Review r = new Review();
                        r.setLesson(lesson);
                        r.setStudent(student);
                        r.setTutor(tutor);
                        return r;
                    });
            review.setLesson(lesson);
            review.setStudent(student);
            review.setTutor(tutor);
            review.setTutorRating(tutorRating);
            review.setPlatformRating(platformRating);
            review.setComment(comment);
            review.setStudentReviewAt(LocalDateTime.now());
            
            Review saved = reviewRepository.save(review);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getReviewByLesson(@PathVariable Long lessonId) {
        Optional<Review> review = reviewRepository.findByLessonId(lessonId);
        if (review.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(review.get());
    }
    
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<?> getReviewsByTutor(@PathVariable Long tutorId) {
        return ResponseEntity.ok(reviewRepository.findByTutorId(tutorId));
    }
    
    @PostMapping("/lesson/{lessonId}/tutor")
    public ResponseEntity<?> addTutorFeedback(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> request) {
        try {
            Long tutorId = Long.valueOf(request.get("tutorId").toString());
            Integer behaviorRating = null;
            if (request.get("studentRating") != null) {
                behaviorRating = Integer.valueOf(request.get("studentRating").toString());
                if (behaviorRating < 1 || behaviorRating > 5) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Student rating must be between 1 and 5"));
                }
            }
            Integer platformRating = null;
            if (request.get("platformRating") != null) {
                platformRating = Integer.valueOf(request.get("platformRating").toString());
                if (platformRating < 1 || platformRating > 5) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Platform rating must be between 1 and 5"));
                }
            }
            String tutorComment = request.get("comment") != null ? request.get("comment").toString() : null;
            
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Lesson not found"));
            
            if (!lesson.getTutor().getId().equals(tutorId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Review not allowed for this tutor"));
            }
            
            Review review = reviewRepository.findByLessonId(lessonId)
                    .orElseGet(() -> {
                        Review r = new Review();
                        r.setLesson(lesson);
                        r.setStudent(lesson.getStudent());
                        r.setTutor(lesson.getTutor());
                        return r;
                    });
            review.setLesson(lesson);
            review.setStudent(lesson.getStudent());
            review.setTutor(lesson.getTutor());
            
            review.setStudentBehaviorRating(behaviorRating);
            review.setTutorPlatformRating(platformRating);
            review.setTutorComment(tutorComment);
            review.setTutorReviewAt(LocalDateTime.now());
            
            Review saved = reviewRepository.save(review);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

