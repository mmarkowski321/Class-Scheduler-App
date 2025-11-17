package pl.projekt.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@DiscriminatorValue("ADMIN")
@Data
@lombok.EqualsAndHashCode(callSuper = false)
public class Admin extends User {
    
}

