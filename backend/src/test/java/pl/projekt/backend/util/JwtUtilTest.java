package pl.projekt.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    // Verifies that generating a token produces a non-empty, well-formed JWT string
    @Test
    void shouldGenerateValidToken() {
        Long userId = 123L;
        String role = "STUDENT";
        String email = "student@example.com";

        String token = jwtUtil.generateToken(userId, role, email);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // Ensures we can extract user id from a valid token
    @Test
    void shouldParseTokenAndExtractUserId() {
        Long userId = 456L;
        String role = "TUTOR";
        String email = "tutor@example.com";

        String token = jwtUtil.generateToken(userId, role, email);
        Long extractedUserId = jwtUtil.getUserIdFromToken(token);

        assertThat(extractedUserId).isEqualTo(userId);
    }

    // Ensures we can extract role claim from a valid token
    @Test
    void shouldParseTokenAndExtractRole() {
        Long userId = 789L;
        String role = "ADMIN";
        String email = "admin@example.com";

        String token = jwtUtil.generateToken(userId, role, email);
        String extractedRole = jwtUtil.getRoleFromToken(token);

        assertThat(extractedRole).isEqualTo(role);
    }

    // Confirms email claim is stored and readable from the token
    @Test
    void shouldParseTokenAndExtractEmail() {
        Long userId = 101L;
        String role = "STUDENT";
        String email = "test@example.com";

        String token = jwtUtil.generateToken(userId, role, email);
        Claims claims = jwtUtil.parseToken(token);
        String extractedEmail = claims.get("email", String.class);

        assertThat(extractedEmail).isEqualTo(email);
    }

    // Valid tokens should pass validation
    @Test
    void shouldValidateValidToken() {
        String token = jwtUtil.generateToken(123L, "STUDENT", "student@example.com");

        boolean isValid = jwtUtil.validateToken(token);

        assertThat(isValid).isTrue();
    }

    // Malformed tokens should fail validation
    @Test
    void shouldInvalidateMalformedToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    // Empty tokens should fail validation
    @Test
    void shouldInvalidateEmptyToken() {
        boolean isValid = jwtUtil.validateToken("");

        assertThat(isValid).isFalse();
    }

    // Null tokens should fail validation
    @Test
    void shouldInvalidateNullToken() {
        boolean isValid = jwtUtil.validateToken(null);

        assertThat(isValid).isFalse();
    }

    // Tokens for different users should yield different user ids
    @Test
    void shouldHandleDifferentUserIds() {
        String token1 = jwtUtil.generateToken(1L, "STUDENT", "user1@example.com");
        String token2 = jwtUtil.generateToken(999L, "TUTOR", "user2@example.com");

        Long userId1 = jwtUtil.getUserIdFromToken(token1);
        Long userId2 = jwtUtil.getUserIdFromToken(token2);

        assertThat(userId1).isEqualTo(1L);
        assertThat(userId2).isEqualTo(999L);
        assertThat(userId1).isNotEqualTo(userId2);
    }

    // Role extraction should be consistent for various roles
    @Test
    void shouldHandleDifferentRoles() {
        String studentToken = jwtUtil.generateToken(1L, "STUDENT", "student@example.com");
        String tutorToken = jwtUtil.generateToken(2L, "TUTOR", "tutor@example.com");
        String adminToken = jwtUtil.generateToken(3L, "ADMIN", "admin@example.com");

        assertThat(jwtUtil.getRoleFromToken(studentToken)).isEqualTo("STUDENT");
        assertThat(jwtUtil.getRoleFromToken(tutorToken)).isEqualTo("TUTOR");
        assertThat(jwtUtil.getRoleFromToken(adminToken)).isEqualTo("ADMIN");
    }

    // Two tokens generated with same input must have identical claims
    @Test
    void shouldGenerateTokensWithSameClaimsForSameInput() {
        String token1 = jwtUtil.generateToken(123L, "STUDENT", "student@example.com");
        String token2 = jwtUtil.generateToken(123L, "STUDENT", "student@example.com");

        assertThat(jwtUtil.getUserIdFromToken(token1)).isEqualTo(jwtUtil.getUserIdFromToken(token2));
        assertThat(jwtUtil.getRoleFromToken(token1)).isEqualTo(jwtUtil.getRoleFromToken(token2));
        
        Claims claims1 = jwtUtil.parseToken(token1);
        Claims claims2 = jwtUtil.parseToken(token2);
        assertThat(claims1.get("email", String.class)).isEqualTo(claims2.get("email", String.class));
    }

    // Getting user id from an invalid token should throw
    @Test
    void shouldThrowExceptionForInvalidTokenWhenGettingUserId() {
        String invalidToken = "invalid.token";

        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    // Getting role from an invalid token should throw
    @Test
    void shouldThrowExceptionForInvalidTokenWhenGettingRole() {
        String invalidToken = "invalid.token";

        assertThatThrownBy(() -> jwtUtil.getRoleFromToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    // Getting user id from a null token should throw
    @Test
    void shouldThrowExceptionForNullTokenWhenGettingUserId() {
        assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(null))
                .isInstanceOf(Exception.class);
    }

    // Emails with special characters should be preserved in claims
    @Test
    void shouldParseTokenWithSpecialCharactersInEmail() {
        String email = "test+user@example.com";
        String token = jwtUtil.generateToken(123L, "STUDENT", email);

        Claims claims = jwtUtil.parseToken(token);
        String extractedEmail = claims.get("email", String.class);

        assertThat(extractedEmail).isEqualTo(email);
    }
}

