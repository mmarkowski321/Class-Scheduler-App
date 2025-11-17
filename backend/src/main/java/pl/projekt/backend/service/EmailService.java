package pl.projekt.backend.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    private String getFromAddress() {
        String explicitFrom = environment.getProperty("MAIL_FROM");
        if (explicitFrom != null && !explicitFrom.isBlank()) {
            return explicitFrom.trim();
        }
        String from = environment.getProperty("spring.mail.username");
        if (from != null && !from.isBlank() && from.contains("@")) {
            return from.trim();
        }
        return "onboarding@resend.dev";
    }
    
    private void sendEmail(String to, String subject, String text) {
        if (isSmtpConfigured()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(getFromAddress());
            mailSender.send(message);
        } else {
            System.out.println("\n========================================");
            System.out.println("📧 EMAIL (DEV MODE)");
            System.out.println("========================================");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("Text: " + text);
            System.out.println("Set spring.mail.* to send real emails.");
            System.out.println("========================================\n");
        }
    }

    public void sendVerificationEmail(@NonNull String email, @NonNull String verificationToken) {
        sendVerificationEmail(email, verificationToken, "pl");
    }
    
    public void sendVerificationEmail(@NonNull String email, @NonNull String verificationToken, @NonNull String lang) {
        String verificationLink = baseUrl + "/verify?token=" + verificationToken;
        
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Verify your EduScheduler account";
            text = "Click the link to verify your account: " + verificationLink;
        } else {
            subject = "Zweryfikuj swoje konto EduScheduler";
            text = "Kliknij w link, aby zweryfikować konto: " + verificationLink;
        }
        
        sendEmail(email, subject, text);
    }

    public void sendWelcomeEmail(@NonNull String email, @NonNull String firstName) {
        sendWelcomeEmail(email, firstName, "pl");
    }
    
    public void sendWelcomeEmail(@NonNull String email, @NonNull String firstName, @NonNull String lang) {
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Welcome to EduScheduler!";
            text = "Hi " + firstName + ", your email has been verified! 🎉";
        } else {
            subject = "Witaj w EduScheduler!";
            text = "Cześć " + firstName + ", Twój email został zweryfikowany! 🎉";
        }
        
        sendEmail(email, subject, text);
    }

    public void sendPasswordResetEmail(@NonNull String email, @NonNull String resetToken) {
        sendPasswordResetEmail(email, resetToken, "pl");
    }
    
    public void sendPasswordResetEmail(@NonNull String email, @NonNull String resetToken, @NonNull String lang) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Reset your EduScheduler password";
            text = "Click the link to reset your password: " + resetLink + "\n\nThis link will expire in 1 hour.";
        } else {
            subject = "Resetowanie hasła EduScheduler";
            text = "Kliknij w link, aby zresetować hasło: " + resetLink + "\n\nTen link wygaśnie za 1 godzinę.";
        }
        
        sendEmail(email, subject, text);
    }

    public void sendPasswordChangedEmail(@NonNull String email, @NonNull String firstName) {
        sendPasswordChangedEmail(email, firstName, "pl");
    }
    
    public void sendPasswordChangedEmail(@NonNull String email, @NonNull String firstName, @NonNull String lang) {
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Password changed successfully";
            text = "Hi " + firstName + ", your password has been successfully changed. If you didn't make this change, please contact support immediately.";
        } else {
            subject = "Hasło zostało zmienione";
            text = "Cześć " + firstName + ", Twoje hasło zostało pomyślnie zmienione. Jeśli nie dokonałeś tej zmiany, skontaktuj się natychmiast z pomocą techniczną.";
        }
        
        sendEmail(email, subject, text);
    }
    
    public void sendEmailChangedEmail(@NonNull String email, @NonNull String firstName, @NonNull String newEmail, @NonNull String lang) {
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Email address changed";
            text = "Hi " + firstName + ", your email address has been changed to: " + newEmail + "\n\nPlease verify your new email address by clicking the verification link that was sent to your new email.\n\nIf you didn't make this change, please contact support immediately.";
        } else {
            subject = "Adres email został zmieniony";
            text = "Cześć " + firstName + ", Twój adres email został zmieniony na: " + newEmail + "\n\nProszę zweryfikować nowy adres email, klikając w link weryfikacyjny, który został wysłany na nowy adres email.\n\nJeśli nie dokonałeś tej zmiany, skontaktuj się natychmiast z pomocą techniczną.";
        }
        
        sendEmail(email, subject, text);
    }

    public void sendContactReply(@NonNull String email, @NonNull String name, @NonNull String replyMessage) {
        sendContactReply(email, name, replyMessage, "pl");
    }
    
    public void sendContactReply(@NonNull String email, @NonNull String name, @NonNull String replyMessage, @NonNull String lang) {
        String subject, text;
        if ("en".equals(lang)) {
            subject = "Response from EduScheduler";
            text = "Hello " + name + ",\n\nThank you for contacting us. Here is our reply:\n\n" + replyMessage + "\n\nBest regards,\nEduScheduler Team";
        } else {
            subject = "Odpowiedź z EduScheduler";
            text = "Witaj " + name + ",\n\nDziękujemy za kontakt z nami. Oto nasza odpowiedź:\n\n" + replyMessage + "\n\nZ poważaniem,\nZespół EduScheduler";
        }
        
        sendEmail(email, subject, text);
    }

    public void sendTutorBookingRequestEmail(@NonNull Tutor tutor,
                                             @NonNull Student student,
                                             @NonNull Lesson lesson) {
        String lang = tutor.getEmailLanguage() != null ? tutor.getEmailLanguage() : "pl";
        Locale locale = "en".equals(lang) ? Locale.ENGLISH : new Locale("pl", "PL");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm", locale);

        String formattedStart = lesson.getStartTime()
                .atZone(ZoneId.systemDefault())
                .format(formatter);
        long durationMinutes = Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();

        String lessonsUrl = baseUrl + "/app/tutor/lessons";

        String subject;
        StringBuilder text = new StringBuilder();

        LessonDeliveryMode mode = lesson.getDeliveryMode();
        boolean isOnline = mode == LessonDeliveryMode.ONLINE;
        String modeLabelEn = isOnline ? "Online" : "Onsite";
        String modeLabelPl = isOnline ? "Zdalnie" : "Stacjonarnie";
        String streetLine = Stream.of(lesson.getOnsiteStreet(), lesson.getOnsiteBuilding(), lesson.getOnsiteApartment())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        String cityLine = Stream.of(lesson.getOnsitePostalCode(), lesson.getOnsiteCity())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));

        if ("en".equals(lang)) {
            subject = "New lesson request from " + student.getFirstName();
            text.append("Hi ").append(tutor.getFirstName()).append(",\n\n")
                    .append("A new lesson request has arrived:\n")
                    .append("Student: ").append(student.getFirstName()).append(" ").append(student.getLastName()).append(" (")
                    .append(student.getEmail()).append(")\n")
                    .append("Date: ").append(formattedStart).append("\n")
                    .append("Duration: ").append(durationMinutes).append(" min\n")
                    .append("Mode: ").append(modeLabelEn).append("\n");

            if (!isOnline && (!streetLine.isBlank() || !cityLine.isBlank())) {
                text.append("Address: ");
                if (!streetLine.isBlank()) {
                    text.append(streetLine);
                }
                if (!cityLine.isBlank()) {
                    if (!streetLine.isBlank()) {
                        text.append(", ");
                    }
                    text.append(cityLine);
                }
                text.append("\n");
            }

            if (lesson.getNotes() != null && !lesson.getNotes().isBlank()) {
                text.append("Student note: ").append(lesson.getNotes()).append("\n");
            }

            text.append("\nReview the request in your dashboard: ").append(lessonsUrl).append("\n\n")
                    .append("Best regards,\nEduScheduler");
        } else {
            subject = "Nowa prośba o lekcję od " + student.getFirstName();
            text.append("Cześć ").append(tutor.getFirstName()).append(",\n\n")
                    .append("Otrzymałeś nową prośbę o lekcję:\n")
                    .append("Uczeń: ").append(student.getFirstName()).append(" ").append(student.getLastName()).append(" (")
                    .append(student.getEmail()).append(")\n")
                    .append("Termin: ").append(formattedStart).append("\n")
                    .append("Czas trwania: ").append(durationMinutes).append(" min\n")
                    .append("Tryb zajęć: ").append(modeLabelPl).append("\n");

            if (!isOnline && (!streetLine.isBlank() || !cityLine.isBlank())) {
                text.append("Adres: ");
                if (!streetLine.isBlank()) {
                    text.append(streetLine);
                }
                if (!cityLine.isBlank()) {
                    if (!streetLine.isBlank()) {
                        text.append(", ");
                    }
                    text.append(cityLine);
                }
                text.append("\n");
            }

            if (lesson.getNotes() != null && !lesson.getNotes().isBlank()) {
                text.append("Wiadomość od ucznia: ").append(lesson.getNotes()).append("\n");
            }

            text.append("\nPotwierdź lub odrzuć w panelu korepetytora: ").append(lessonsUrl).append("\n\n")
                    .append("Pozdrowienia,\nZespół EduScheduler");
        }

        sendEmail(tutor.getEmail(), subject, text.toString());
    }
}

