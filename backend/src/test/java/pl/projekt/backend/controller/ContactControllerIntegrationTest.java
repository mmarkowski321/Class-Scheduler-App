package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.model.ContactMessage;
import pl.projekt.backend.repository.ContactMessageRepository;
import pl.projekt.backend.service.EmailService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @MockBean
    private EmailService emailService;

    private Authentication adminAuth;
    private Authentication nonAdminAuth;

    @BeforeEach
    void setUp() {
        contactMessageRepository.deleteAll();

        // Mock admin authentication
        adminAuth = mock(Authentication.class);
        List<GrantedAuthority> adminAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(adminAuthorities).when(adminAuth).getAuthorities();
        
        // Mock non-admin authentication
        nonAdminAuth = mock(Authentication.class);
        List<GrantedAuthority> userAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(userAuthorities).when(nonAdminAuth).getAuthorities();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        contactMessageRepository.deleteAll();
    }

    @Test
    void shouldSendContactMessage() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "John Doe");
        request.put("email", "john@example.com");
        request.put("subject", "Test Subject");
        request.put("message", "Test message content");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact message sent successfully"))
                .andExpect(jsonPath("$.id").exists());

        List<ContactMessage> messages = contactMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getName()).isEqualTo("John Doe");
        assertThat(messages.get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(messages.get(0).getSubject()).isEqualTo("Test Subject");
        assertThat(messages.get(0).getMessage()).isEqualTo("Test message content");
        assertThat(messages.get(0).getReplied()).isFalse();
    }

    @Test
    void shouldRejectContactMessageWithMissingFields() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "John Doe");
        // Missing email and message

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name, email, and message are required"));

        assertThat(contactMessageRepository.findAll()).isEmpty();
    }

    @Test
    void shouldRejectContactMessageWithBlankFields() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "   ");
        request.put("email", "");
        request.put("message", "  ");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name, email, and message are required"));
    }

    @Test
    void shouldGetAllMessagesAsAdmin() throws Exception {
        // Create test messages
        ContactMessage message1 = new ContactMessage();
        message1.setName("User 1");
        message1.setEmail("user1@example.com");
        message1.setMessage("Message 1");
        contactMessageRepository.save(message1);

        ContactMessage message2 = new ContactMessage();
        message2.setName("User 2");
        message2.setEmail("user2@example.com");
        message2.setMessage("Message 2");
        contactMessageRepository.save(message2);

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRejectGetAllMessagesAsNonAdmin() throws Exception {
        // Set non-admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(nonAdminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void shouldGetUnrepliedMessagesAsAdmin() throws Exception {
        // Create replied and unreplied messages
        ContactMessage repliedMessage = new ContactMessage();
        repliedMessage.setName("User 1");
        repliedMessage.setEmail("user1@example.com");
        repliedMessage.setMessage("Message 1");
        repliedMessage.setReplied(true);
        contactMessageRepository.save(repliedMessage);

        ContactMessage unrepliedMessage = new ContactMessage();
        unrepliedMessage.setName("User 2");
        unrepliedMessage.setEmail("user2@example.com");
        unrepliedMessage.setMessage("Message 2");
        unrepliedMessage.setReplied(false);
        contactMessageRepository.save(unrepliedMessage);

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/contact/unreplied")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].replied").value(false));
    }

    @Test
    void shouldGetMessageByIdAsAdmin() throws Exception {
        ContactMessage message = new ContactMessage();
        message.setName("User 1");
        message.setEmail("user1@example.com");
        message.setMessage("Test message");
        message = contactMessageRepository.save(message);

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/contact/{id}", message.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User 1"))
                .andExpect(jsonPath("$.email").value("user1@example.com"))
                .andExpect(jsonPath("$.message").value("Test message"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentMessage() throws Exception {
        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/contact/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReplyToMessageAsAdmin() throws Exception {
        ContactMessage message = new ContactMessage();
        message.setName("User 1");
        message.setEmail("user1@example.com");
        message.setMessage("Test message");
        message = contactMessageRepository.save(message);

        Map<String, String> replyRequest = new HashMap<>();
        replyRequest.put("reply", "Thank you for your message!");

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(post("/api/contact/{id}/reply", message.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminReply").value("Thank you for your message!"))
                .andExpect(jsonPath("$.replied").value(true))
                .andExpect(jsonPath("$.repliedAt").exists());

        verify(emailService).sendContactReply(eq("user1@example.com"), eq("User 1"), eq("Thank you for your message!"));
    }

    @Test
    void shouldRejectReplyWithoutMessage() throws Exception {
        ContactMessage message = new ContactMessage();
        message.setName("User 1");
        message.setEmail("user1@example.com");
        message.setMessage("Test message");
        message = contactMessageRepository.save(message);

        Map<String, String> replyRequest = new HashMap<>();
        replyRequest.put("reply", "   "); // Blank reply

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(post("/api/contact/{id}/reply", message.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Reply message is required"));

        verify(emailService, never()).sendContactReply(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectReplyAsNonAdmin() throws Exception {
        ContactMessage message = new ContactMessage();
        message.setName("User 1");
        message.setEmail("user1@example.com");
        message.setMessage("Test message");
        message = contactMessageRepository.save(message);

        Map<String, String> replyRequest = new HashMap<>();
        replyRequest.put("reply", "Reply message");

        // Set non-admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(nonAdminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(post("/api/contact/{id}/reply", message.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }

    @Test
    void shouldHandleEmailSendFailure() throws Exception {
        ContactMessage message = new ContactMessage();
        message.setName("User 1");
        message.setEmail("user1@example.com");
        message.setMessage("Test message");
        message = contactMessageRepository.save(message);

        Map<String, String> replyRequest = new HashMap<>();
        replyRequest.put("reply", "Reply message");

        // Mock email service to throw exception
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailService).sendContactReply(anyString(), anyString(), anyString());

        // Set admin authentication
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(adminAuth);
        SecurityContextHolder.setContext(securityContext);

        // Should still succeed (email failure is logged but doesn't fail request)
        mockMvc.perform(post("/api/contact/{id}/reply", message.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replied").value(true));

        // Verify message was saved despite email failure
        ContactMessage saved = contactMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(saved.getReplied()).isTrue();
        assertThat(saved.getAdminReply()).isEqualTo("Reply message");
    }
}

