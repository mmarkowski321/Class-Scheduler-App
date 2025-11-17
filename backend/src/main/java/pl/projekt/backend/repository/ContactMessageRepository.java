package pl.projekt.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.projekt.backend.model.ContactMessage;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
    
    List<ContactMessage> findByRepliedFalseOrderByCreatedAtDesc();
    
    List<ContactMessage> findByRepliedTrueOrderByCreatedAtDesc();
    
}

