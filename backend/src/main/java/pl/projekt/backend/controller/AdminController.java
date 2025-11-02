package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.*;
import pl.projekt.backend.repository.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private LessonRepository lessonRepository;
    
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        
        // Check role from authorities (set by JwtAuthenticationFilter)
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    
    private ResponseEntity<?> checkAdminAccess() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return null;
    }
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;
        
        List<User> allUsers = userRepository.findAll();
        
        List<User> tutors = allUsers.stream()
            .filter(user -> user instanceof Tutor)
            .collect(Collectors.toList());
        
        List<User> students = allUsers.stream()
            .filter(user -> user instanceof Student)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("tutors", tutors);
        response.put("students", students);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/tutors")
    public ResponseEntity<List<User>> getAllTutors() {
        List<User> tutors = userRepository.findAll().stream()
            .filter(user -> user instanceof Tutor)
            .collect(Collectors.toList());
        return ResponseEntity.ok(tutors);
    }
    
    @GetMapping("/users/students")
    public ResponseEntity<List<User>> getAllStudents() {
        List<User> students = userRepository.findAll().stream()
            .filter(user -> user instanceof Student)
            .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;
        
        return userRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/users/{id}/lessons")
    public ResponseEntity<?> getUserLessons(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Lesson> lessons;
        if (user instanceof Tutor) {
            lessons = lessonRepository.findByTutorId(id);
        } else if (user instanceof Student) {
            lessons = lessonRepository.findByStudentId(id);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user type"));
        }
        
        return ResponseEntity.ok(lessons);
    }
    
    @GetMapping("/users/{id}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        long lessonsCount;
        long reviewsCount;
        
        if (user instanceof Tutor) {
            lessonsCount = lessonRepository.findByTutorId(id).size();
            reviewsCount = reviewRepository.findByTutorId(id).size();
        } else if (user instanceof Student) {
            lessonsCount = lessonRepository.findByStudentId(id).size();
            reviewsCount = reviewRepository.findByStudentId(id).size();
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user type"));
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("lessonsCount", lessonsCount);
        stats.put("reviewsCount", reviewsCount);
        
        return ResponseEntity.ok(stats);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> banUser(@PathVariable Long id) {
        ResponseEntity<?> adminCheck = checkAdminAccess();
        if (adminCheck != null) return adminCheck;
        
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Don't allow banning admins
            if (user instanceof Admin) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot ban admin users"));
            }
            
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("message", "User banned successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewRepository.findAll());
    }
    
    @GetMapping("/reviews/user/{userId}")
    public ResponseEntity<?> getReviewsByUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user instanceof Tutor) {
            return ResponseEntity.ok(reviewRepository.findByTutorId(userId));
        } else if (user instanceof Student) {
            return ResponseEntity.ok(reviewRepository.findByStudentId(userId));
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid user type"));
    }
    
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<?> getReview(@PathVariable Long reviewId) {
        return reviewRepository.findById(reviewId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/lessons")
    public ResponseEntity<List<Lesson>> getAllLessons() {
        return ResponseEntity.ok(lessonRepository.findAll());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        long totalUsers = userRepository.count();
        long tutorsCount = userRepository.findAll().stream()
            .filter(user -> user instanceof Tutor)
            .count();
        long studentsCount = userRepository.findAll().stream()
            .filter(user -> user instanceof Student)
            .count();
        long totalLessons = lessonRepository.count();
        long totalReviews = reviewRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("tutorsCount", tutorsCount);
        stats.put("studentsCount", studentsCount);
        stats.put("totalLessons", totalLessons);
        stats.put("totalReviews", totalReviews);
        
        return ResponseEntity.ok(stats);
    }
}

