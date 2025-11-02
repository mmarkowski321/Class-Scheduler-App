package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

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
            
            // Check if review already exists for this lesson
            if (reviewRepository.existsByLessonId(lessonId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Review already exists for this lesson"));
            }
            
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
            
            // Create review
            Review review = new Review();
            review.setLesson(lesson);
            review.setStudent(student);
            review.setTutor(tutor);
            review.setTutorRating(tutorRating);
            review.setPlatformRating(platformRating);
            review.setComment(comment);
            
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
    
    @PutMapping("/{reviewId}/student-behavior")
    public ResponseEntity<?> addStudentBehaviorRating(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer behaviorRating = null;
            if (request.get("behaviorRating") != null) {
                behaviorRating = Integer.valueOf(request.get("behaviorRating").toString());
                if (behaviorRating < 1 || behaviorRating > 5) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Behavior rating must be between 1 and 5"));
                }
            }
            
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
            
            review.setStudentBehaviorRating(behaviorRating);
            
            Review saved = reviewRepository.save(review);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

