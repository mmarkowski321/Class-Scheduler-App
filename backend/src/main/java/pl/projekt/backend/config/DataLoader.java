package pl.projekt.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pl.projekt.backend.model.Admin;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;

import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        // Create default admin if it doesn't exist
        if (!userRepository.existsByEmail("admin@eduscheduler.com")) {
            Admin admin = new Admin();
            admin.setEmail("admin@eduscheduler.com");
            // Strong default password - change this in production!
            admin.setPassword(passwordEncoder.encode("EduScheduler2024!Admin"));
            admin.setFirstName("Admin");
            admin.setLastName("EduScheduler");
            admin.setBirthDate(LocalDate.of(1990, 1, 1));
            admin.setEmailVerified(true); // Admin doesn't need email verification
            
            userRepository.save(admin);
            System.out.println("Default admin created");
        }
    }
}

