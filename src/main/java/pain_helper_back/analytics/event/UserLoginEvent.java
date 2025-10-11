package pain_helper_back.analytics.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/*
 * Событие: Пользователь вошел в систему
 * Публикуется в PersonService.login()
 */
@Getter
public class UserLoginEvent extends ApplicationEvent {
    private final String personId;
    private final String role;
    private final LocalDateTime loginAt;
    private final Boolean success; // true, false
    private final String ipAddress; // IP адрес (optional)

    public UserLoginEvent(Object source, String personId, String role,
                          LocalDateTime loginAt, Boolean success, String ipAddress) {
        super(source);
        this.personId = personId;
        this.role = role;
        this.loginAt = loginAt;
        this.success = success;
        this.ipAddress = ipAddress;
    }
}
