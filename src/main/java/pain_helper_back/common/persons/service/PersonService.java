package pain_helper_back.common.persons.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;

/**
 * PersonService - сервис для работы с профилями пользователей
 * 
 * NOTE: Методы login() и changeCredentials() удалены.
 * Аутентификация и управление учетными данными теперь выполняются 
 * через Authentication Service (порт 8082).
 * 
 * Этот сервис оставлен для работы с профилями пользователей (если потребуется).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {
    private final PersonRepository personRepository;

    /**
     * Получить информацию о пользователе по personId
     */
    public Person getPersonByPersonId(String personId) {
        return personRepository.findByPersonId(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
    }

    /**
     * Получить информацию о пользователе по login
     */
    public Person getPersonByLogin(String login) {
        return personRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Person not found with login: " + login));
    }
}
