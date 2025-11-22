package pl.projekt.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment environment;

    @InjectMocks
    private EmailService emailService;

    private Tutor tutor;
    private Student student;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock environment
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.example.com");

        // Create tutor
        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));

        // Create student
        student = new Student();
        student.setEmail("student@example.com");
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));

        // Create lesson
        lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
    }

    @Test
    void shouldSendVerificationEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@example.com", "token123");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendVerificationEmailWithLanguage() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("test@example.com", "token123", "en");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendPasswordResetEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendPasswordResetEmail("test@example.com", "token123");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendWelcomeEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendWelcomeEmail("test@example.com", "Test User");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendTutorBookingRequestEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendTutorBookingRequestEmail(tutor, student, lesson);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendContactReply() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendContactReply("test@example.com", "Test User", "Reply message");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotSendEmailWhenSmtpNotConfigured() {
        when(environment.getProperty("spring.mail.host")).thenReturn(null);

        emailService.sendVerificationEmail("test@example.com", "token123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldIncludeLessonDetailsInBookingRequest() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson.setMeetingLink("https://meet.example.com/123");

        emailService.sendTutorBookingRequestEmail(tutor, student, lesson);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendPasswordChangedEmail() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendPasswordChangedEmail("test@example.com", "Test User");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

