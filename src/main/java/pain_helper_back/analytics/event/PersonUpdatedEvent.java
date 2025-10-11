package pain_helper_back.analytics.event;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/*
 * Событие: Обновлены данные сотрудника
 * Публикуется в AdminServiceImpl.updatePerson() или PersonService.changeCredentials()
 */
@Getter
public class PersonUpdatedEvent extends ApplicationEvent {
    private final String personId;
    private final String updatedBy; // adminId or person
    private final LocalDateTime updatedAt;
    private final Map<String, String> changedFields; // what fields are changed

    public PersonUpdatedEvent(Object source, String personId, String updatedBy,
                              LocalDateTime updatedAt, Map<String, String> changedFields) {
        super(source);
        this.personId = personId;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.changedFields = changedFields;
    }
}
