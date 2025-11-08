package pl.projekt.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    
    List<Calendar> findByUserIdAndActiveTrue(Long userId);
    
    List<Calendar> findByUserId(Long userId);
    
    Optional<Calendar> findByIdAndUserId(Long id, Long userId);
    
    void deleteByUserIdAndId(Long userId, Long id);
}


