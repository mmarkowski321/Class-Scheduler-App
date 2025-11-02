package pl.projekt.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.projekt.backend.model.Review;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Find review by lesson ID
    Optional<Review> findByLessonId(Long lessonId);
    
    // Find all reviews for a tutor
    List<Review> findByTutorId(Long tutorId);
    
    // Find all reviews for a student
    List<Review> findByStudentId(Long studentId);
    
    // Calculate average platform rating
    @Query("SELECT AVG(r.platformRating) FROM Review r")
    Double getAveragePlatformRating();
    
    // Count total reviews
    long count();
    
    // Check if lesson already has a review
    boolean existsByLessonId(Long lessonId);
}

