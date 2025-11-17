package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.ContactMessage;
import pl.projekt.backend.repository.ContactMessageRepository;
import pl.projekt.backend.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "http://localhost:5173")
public class ContactController {
    
    @Autowired
    private ContactMessageRepository contactMessageRepository;
    
    @Autowired
    private EmailService emailService;
    
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    
    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String email = request.get("email");
            String subject = request.get("subject");
            String message = request.get("message");
            
            if (name == null || name.isBlank() || email == null || email.isBlank() || 
                message == null || message.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name, email, and message are required"));
            }
            
            ContactMessage contactMessage = new ContactMessage();
            contactMessage.setName(name);
            contactMessage.setEmail(email);
            contactMessage.setSubject(subject);
            contactMessage.setMessage(message);
            contactMessage.setReplied(false);
            
            ContactMessage saved = contactMessageRepository.save(contactMessage);
            return ResponseEntity.ok(Map.of("message", "Contact message sent successfully", "id", saved.getId()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getAllMessages() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(contactMessageRepository.findAllByOrderByCreatedAtDesc());
    }
    
    @GetMapping("/unreplied")
    public ResponseEntity<?> getUnrepliedMessages() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(contactMessageRepository.findByRepliedFalseOrderByCreatedAtDesc());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getMessage(@PathVariable Long id) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return contactMessageRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyToMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        
        try {
            String adminReply = request.get("reply");
            if (adminReply == null || adminReply.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Reply message is required"));
            }
            
            ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
            
            message.setAdminReply(adminReply);
            message.setReplied(true);
            message.setRepliedAt(LocalDateTime.now());
            
            ContactMessage saved = contactMessageRepository.save(message);
            
            // Send email reply to the user
            try {
                emailService.sendContactReply(message.getEmail(), message.getName(), adminReply);
            } catch (Exception e) {
                // Log error but don't fail the request - reply is already saved
                System.err.println("Failed to send contact reply email: " + e.getMessage());
            }
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

