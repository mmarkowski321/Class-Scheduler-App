package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class StatsController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LessonRepository lessonRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @GetMapping("/homepage")
    public ResponseEntity<Map<String, Object>> getHomepageStats() {
        // Count all registered tutors
        long tutorCount = userRepository.findAll().stream()
            .filter(user -> user instanceof Tutor)
            .count();
        
        // Count completed lessons from the last month
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long completedLessons = lessonRepository.countByStatusAndStartTimeAfter(
            LessonStatus.COMPLETED, 
            oneMonthAgo
        );
        
        // Calculate satisfied students percentage from platform ratings
        Double avgPlatformRating = reviewRepository.getAveragePlatformRating();
        Double satisfiedStudents = null;
        
        if (avgPlatformRating != null && reviewRepository.count() > 0) {
            // Convert average rating (1-5) to percentage (0-100)
            // 5 stars = 100%, 4 stars = 80%, etc.
            double percentage = (avgPlatformRating / 5.0) * 100.0;
            // Round to 2 decimal places
            satisfiedStudents = Math.round(percentage * 100.0) / 100.0;
        }
        // If no reviews, return null (frontend will handle it)
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("tutors", tutorCount);
        stats.put("monthlyLessons", completedLessons);
        stats.put("satisfiedStudents", satisfiedStudents);
        
        return ResponseEntity.ok(stats);
    }
}

