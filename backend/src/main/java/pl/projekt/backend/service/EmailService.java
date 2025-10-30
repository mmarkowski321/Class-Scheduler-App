package pl.projekt.backend.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;
    private String baseUrl;

    @Autowired
    public EmailService(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;

        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            baseUrl = dotenv.get("APP_BASE_URL", "http://localhost:5173");
        } catch (Exception e) {
            baseUrl = "http://localhost:5173";
            System.out.println("Using default base URL: " + baseUrl);
        }
    }

    private boolean isSmtpConfigured() {
        String host = environment.getProperty("spring.mail.host");
        return host != null && !host.isBlank();
    }

    public void sendVerificationEmail(@NonNull String email, @NonNull String verificationToken) {
        String verificationLink = baseUrl + "/verify?token=" + verificationToken;

        if (isSmtpConfigured()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Verify your EduScheduler account");
            message.setText("Click the link to verify your account: " + verificationLink);

            String explicitFrom = environment.getProperty("MAIL_FROM");
            if (explicitFrom != null && !explicitFrom.isBlank()) {
                message.setFrom(explicitFrom.trim());
            } else {
                String from = environment.getProperty("spring.mail.username");
                if (from != null && !from.isBlank() && from.contains("@")) {
                    message.setFrom(from.trim());
                } else {
                    // Safe default for Resend when MAIL_FROM is not provided
                    message.setFrom("onboarding@resend.dev");
                }
            }
            mailSender.send(message);
        } else {
            System.out.println("\n========================================");
            System.out.println("📧 EMAIL VERIFICATION (DEV MODE)");
            System.out.println("========================================");
            System.out.println("To: " + email);
            System.out.println("Subject: Verify your EduScheduler account");
            System.out.println("Verification Link: " + verificationLink);
            System.out.println("Set spring.mail.* to send real emails.");
            System.out.println("========================================\n");
        }
    }

    public void sendWelcomeEmail(@NonNull String email, @NonNull String firstName) {
        if (isSmtpConfigured()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Welcome to EduScheduler!");
            message.setText("Hi " + firstName + ", your email has been verified! 🎉");
            String explicitFrom = environment.getProperty("MAIL_FROM");
            if (explicitFrom != null && !explicitFrom.isBlank()) {
                message.setFrom(explicitFrom.trim());
            } else {
                String from = environment.getProperty("spring.mail.username");
                if (from != null && !from.isBlank() && from.contains("@")) {
                    message.setFrom(from.trim());
                } else {
                    message.setFrom("onboarding@resend.dev");
                }
            }
            mailSender.send(message);
        } else {
            System.out.println("\n========================================");
            System.out.println("🎉 WELCOME EMAIL (DEV MODE)");
            System.out.println("========================================");
            System.out.println("To: " + email);
            System.out.println("Subject: Welcome to EduScheduler!");
            System.out.println("Hi " + firstName + ", your email has been verified!");
            System.out.println("Set spring.mail.* to send real emails.");
            System.out.println("========================================\n");
        }
    }
}

