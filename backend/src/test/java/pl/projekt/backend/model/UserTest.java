package pl.projekt.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    private Student student;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setEmail("student@example.com");
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(false);
        student.setBanned(false);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor.setBanned(false);
    }

    // Marks email as verified and clears verification token
    @Test
    void shouldVerifyEmail() {
        student.setVerificationToken("token-123");
        assertThat(student.getEmailVerified()).isFalse();
        assertThat(student.getVerificationToken()).isNotNull();

        student.verifyEmail();

        assertThat(student.getEmailVerified()).isTrue();
        assertThat(student.getVerificationToken()).isNull();
    }

    // Sets banned flag to true
    @Test
    void shouldBanUser() {
        assertThat(student.getBanned()).isFalse();

        student.ban();

        assertThat(student.getBanned()).isTrue();
    }

    // Sets banned flag to false
    @Test
    void shouldUnbanUser() {
        student.setBanned(true);
        assertThat(student.getBanned()).isTrue();

        student.unban();

        assertThat(student.getBanned()).isFalse();
    }

    // Clears reset password token and expiry
    @Test
    void shouldClearResetToken() {
        student.setResetPasswordToken("reset-token-123");
        student.setResetPasswordTokenExpiry("2024-12-31T23:59:59");

        assertThat(student.getResetPasswordToken()).isNotNull();
        assertThat(student.getResetPasswordTokenExpiry()).isNotNull();

        student.clearResetToken();

        assertThat(student.getResetPasswordToken()).isNull();
        assertThat(student.getResetPasswordTokenExpiry()).isNull();
    }

    // Handles multiple ban/unban transitions
    @Test
    void shouldHandleBanAndUnbanMultipleTimes() {
        student.ban();
        assertThat(student.getBanned()).isTrue();

        student.unban();
        assertThat(student.getBanned()).isFalse();

        student.ban();
        assertThat(student.getBanned()).isTrue();

        student.unban();
        assertThat(student.getBanned()).isFalse();
    }

    // Applies verify/ban/unban logic also to tutors
    @Test
    void shouldWorkWithTutor() {
        tutor.ban();
        assertThat(tutor.getBanned()).isTrue();

        tutor.unban();
        assertThat(tutor.getBanned()).isFalse();

        tutor.setVerificationToken("token");
        tutor.verifyEmail();
        assertThat(tutor.getEmailVerified()).isTrue();
        assertThat(tutor.getVerificationToken()).isNull();
    }

    // Does nothing when tokens are already null
    @Test
    void shouldClearResetTokenWhenTokenIsNull() {
        student.setResetPasswordToken(null);
        student.setResetPasswordTokenExpiry(null);

        student.clearResetToken();

        assertThat(student.getResetPasswordToken()).isNull();
        assertThat(student.getResetPasswordTokenExpiry()).isNull();
    }

    // Verifying multiple times stays verified and token remains cleared
    @Test
    void shouldVerifyEmailMultipleTimes() {
        student.setVerificationToken("token-1");
        student.verifyEmail();
        assertThat(student.getEmailVerified()).isTrue();

        student.verifyEmail();
        assertThat(student.getEmailVerified()).isTrue();
        assertThat(student.getVerificationToken()).isNull();
    }
}

