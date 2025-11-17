package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@DiscriminatorValue("ADMIN")
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class Admin extends User {
    
    // Admin-specific fields can be added here in the future
    // For now, Admin is just a special type of User that can't register
    
}

