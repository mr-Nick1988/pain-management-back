package pain_helper_back.common.persons.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.analytics.event.PersonUpdatedEvent;
import pain_helper_back.analytics.event.UserLoginEvent;
import pain_helper_back.common.persons.dto.ChangeCredentialsDTO;
import pain_helper_back.common.persons.dto.PersonLoginRequestDTO;
import pain_helper_back.common.persons.dto.PersonLoginResponseDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {
    private final PersonRepository personRepository;
    private final ApplicationEventPublisher eventPublisher;


    public PersonLoginResponseDTO login(PersonLoginRequestDTO loginRequest) {
        Person person = personRepository.findByLogin(loginRequest.getLogin())
                .orElseThrow(() -> {
                    //Публикуем событие неудачного входа
                    eventPublisher.publishEvent(new UserLoginEvent(
                            this,
                            loginRequest.getLogin(),
                            "UNKNOWN",
                            LocalDateTime.now(),
                            false, // success = false
                            null // IP address (можно добавить позже)
                    ));
                    log.warn("Failed login attempt: login={}", loginRequest.getLogin());
                    return new RuntimeException("Invalid login or password");
                });

        if (!person.getPassword().equals(loginRequest.getPassword())) {
            //Публикуем событие неудачного входа (неверный пароль)
            eventPublisher.publishEvent(new UserLoginEvent(
                    this,
                    person.getPersonId(),
                    person.getRole().name(),
                    LocalDateTime.now(),
                    false, // success = false
                    null
            ));
            log.warn("Failed login attempt (wrong password): personId={}", person.getPersonId());
            throw new RuntimeException("Invalid login or password");
        }
        //Публикуем событие успешного входа
        eventPublisher.publishEvent(new UserLoginEvent(
                this,
                person.getPersonId(),
                person.getRole().name(),
                LocalDateTime.now(),
                true, // success = true
                null // IP address
        ));
        log.info("Successful login: personId={}, role={}", person.getPersonId(), person.getRole());

        PersonLoginResponseDTO response = new PersonLoginResponseDTO();
        response.setFirstName(person.getFirstName());
        response.setRole(person.getRole().name());
        response.setTemporaryCredentials(person.isTemporaryCredentials());
        return response;
    }

    public void changeCredentials(ChangeCredentialsDTO request) {
        Person person = personRepository.findByLogin(request.getCurrentLogin())
                .orElseThrow(() -> new RuntimeException("User not found"));

        //TODO В дальнейшем здесь должна быть проверка пароля с использованием хеширования
        if (!person.getPassword().equals(request.getOldPassword())) {
            throw new RuntimeException("Invalid old password");
        }
        person.setLogin(request.getNewLogin());
        person.setPassword(request.getNewPassword());
        person.setTemporaryCredentials(false);

        // Отслеживаем изменения
        Map<String, String> changedFields = new HashMap<>();
        changedFields.put("login", request.getNewLogin());
        changedFields.put("password", "***CHANGED***"); // Не логируем пароль

        personRepository.save(person);

        //событие обновления учетных данных
        eventPublisher.publishEvent(new PersonUpdatedEvent(
                this,
                person.getPersonId(),
                person.getPersonId(), // Сам пользователь обновил свои данные
                LocalDateTime.now(),
                changedFields
        ));
        log.info("Credentials changed: personId={}", person.getPersonId());
    }
}
