package pain_helper_back.common.persons.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.persons.dto.ChangeCredentialsDTO;
import pain_helper_back.common.persons.dto.PersonLoginRequestDTO;
import pain_helper_back.common.persons.dto.PersonLoginResponseDTO;
import pain_helper_back.common.persons.service.PersonService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j  // Это как черный ящик в самолёте — записывает шаги системы.
// Если пользователь пишет "я не мог залогиниться" → смотришь лог: был ли запрос, с каким логином, была ли ошибка.
public class PersonController {

    private final PersonService personService;

    @PostMapping("/person/login")
    public PersonLoginResponseDTO login(@RequestBody @Valid PersonLoginRequestDTO loginRequest) {
        return personService.login(loginRequest);
    }

    @PostMapping("/person/change-credentials")
    public void changeCredentials(@RequestBody @Valid ChangeCredentialsDTO request) {
        personService.changeCredentials(request);
    }

}
