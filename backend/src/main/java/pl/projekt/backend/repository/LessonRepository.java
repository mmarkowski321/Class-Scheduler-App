package pl.projekt.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    
    // Count lessons with given status after start date
    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.status = :status AND l.startTime >= :startDate")
    long countByStatusAndStartTimeAfter(LessonStatus status, LocalDateTime startDate);
    
    // Find all lessons for a specific tutor
    List<Lesson> findByTutorId(Long tutorId);
    
    // Find all lessons for a specific student
    List<Lesson> findByStudentId(Long studentId);
    
    // Find lessons by status
    List<Lesson> findByStatus(LessonStatus status);
    
    // Find lessons between dates
    @Query("SELECT l FROM Lesson l WHERE l.startTime >= :start AND l.startTime <= :end")
    List<Lesson> findBetweenDates(LocalDateTime start, LocalDateTime end);
}

