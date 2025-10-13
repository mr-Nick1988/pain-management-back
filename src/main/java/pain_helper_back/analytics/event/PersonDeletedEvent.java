package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 *
 * Событие: Удален сотрудник
 * Публикуется в AdminServiceImpl.deletePerson()
 */
@Getter
public class PersonDeletedEvent extends ApplicationEvent {
    private final String personId;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final String deletedBy; // adminId
    private final LocalDateTime deletedAt;
    private final String reason; // reason of delete (optional)

    public PersonDeletedEvent(Object source, String personId, String firstName,
                              String lastName, String role, String deletedBy,
                              LocalDateTime deletedAt, String reason) {
        super(source);
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
        this.reason = reason;
    }
}
