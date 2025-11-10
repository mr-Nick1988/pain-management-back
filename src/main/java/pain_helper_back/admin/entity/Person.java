package pain_helper_back.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.enums.Roles;

@Entity
@Data
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Технический ID для базы данных
    
    @Column(nullable = false, unique = true)
    private String personId; // ID документа человека
    
    private String firstName;
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String login;

    private String password;
    private Roles role;
    private boolean temporaryCredentials = true;
}
