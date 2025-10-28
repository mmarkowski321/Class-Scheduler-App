package pl.projekt.backend.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private String baseUrl;
    
    public EmailService() {
        try {
            // Load .env file
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            baseUrl = dotenv.get("APP_BASE_URL", "http://localhost:5173");
        } catch (Exception e) {
            // Fallback if .env file can't be read
            baseUrl = "http://localhost:5173";
            System.out.println("Using default base URL: " + baseUrl);
        }
    }
    
    /**
     * Send verification email - Simple file-based approach
     */
    public void sendVerificationEmail(String email, String verificationToken) {
        String verificationLink = baseUrl + "/verify?token=" + verificationToken;
        
        // Simple console output with clickable link
        System.out.println("\n========================================");
        System.out.println("📧 EMAIL VERIFICATION");
        System.out.println("========================================");
        System.out.println("To: " + email);
        System.out.println("Subject: Verify your EduScheduler account");
        System.out.println("Verification Link: " + verificationLink);
        System.out.println("========================================\n");
    }
    
    /**
     * Send welcome email after successful verification
     */
    public void sendWelcomeEmail(String email, String firstName) {
        System.out.println("\n========================================");
        System.out.println("🎉 WELCOME EMAIL");
        System.out.println("========================================");
        System.out.println("To: " + email);
        System.out.println("Subject: Welcome to EduScheduler!");
        System.out.println("Hi " + firstName + ", your email has been verified!");
        System.out.println("========================================\n");
    }
    
}

