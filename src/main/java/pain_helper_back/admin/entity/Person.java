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

    @Enumerated(EnumType.STRING)
    private Roles role;
    
    // NOTE: password удален - аутентификация через Authentication Service (порт 8082)
    private boolean temporaryCredentials = true;
}
