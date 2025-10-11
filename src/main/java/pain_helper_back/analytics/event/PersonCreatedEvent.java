package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Создан новый сотрудник (врач, медсестра, анестезиолог)
 * Публикуется в AdminServiceImpl.createPerson()
 */
@Getter
public class PersonCreatedEvent extends ApplicationEvent {
    private final String personId;
    private final String firstName;
    private final String lastName;
    private final String role; // DOCTOR, NURSE, ANESTHESIOLOGIST, ADMIN
    private final String createdBy; // adminId
    private final LocalDateTime createdAt;

    public PersonCreatedEvent(Object source, String personId, String firstName,
                              String lastName, String role, String createdBy,
                              LocalDateTime createdAt) {
        super(source);
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
