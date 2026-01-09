package pain_helper_back.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.enums.Roles;

@Entity
@Data
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Technical database ID
    
    @Column(nullable = false, unique = true)
    private String personId; // Person's document ID
    
    private String firstName;
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String login;

    @Enumerated(EnumType.STRING)
    private Roles role;
    
    // NOTE: password removed - authentication handled by Authentication Service (port 8082)
    private boolean temporaryCredentials = true;
}
