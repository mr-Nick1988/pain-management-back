package pain_helper_back.common.persons.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.common.persons.service.PersonService;

/**
 * PersonController - Controller for работы с профилями пользователей
 * 
 * NOTE: Endpoints /login и /change-credentials удалены.
 * for аутентификации используйте Authentication Service:
 * - POST http://localhost:8082/api/auth/login
 * - POST http://localhost:8082/api/auth/change-password
 * - GET http://localhost:8082/api/auth/me
 * 
 * Этот Controller оставлен for дополнительных операций с профилем (if потребуется).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/person")
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class PersonController {

    private final PersonService personService;

    /**
     * Получить информацию о текущем пользователе via JWT
     */
    @GetMapping("/me")
    public Person getCurrentPerson(@AuthenticationPrincipal String personId) {
        log.info("Getting current person info for personId: {}", personId);
        return personService.getPersonByPersonId(personId);
    }
}
