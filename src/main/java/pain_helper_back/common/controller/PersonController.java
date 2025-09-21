package pain_helper_back.common.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.dto.ChangeCredentialsDTO;
import pain_helper_back.common.dto.PersonLoginRequestDTO;
import pain_helper_back.common.dto.PersonLoginResponseDTO;
import pain_helper_back.common.service.PersonService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class PersonController {

    private final PersonService personService;

    @PostMapping("/person/login")
    public PersonLoginResponseDTO login(@RequestBody @Valid PersonLoginRequestDTO loginRequest) {
        PersonLoginResponseDTO response = personService.login(loginRequest);
        return response;
    }

    @PostMapping("/person/change-credentials")
    public void changeCredentials(@RequestBody @Valid ChangeCredentialsDTO request) {
        personService.changeCredentials(request);
    }

}
