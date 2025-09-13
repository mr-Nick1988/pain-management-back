package pain_helper_back.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity

@Data
public class Person {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    private String role;
    private boolean temporaryCredentials = true;
}
