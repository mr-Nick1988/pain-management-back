package pain_helper_back.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.dto.ChangeCredentialsDTO;
import pain_helper_back.dto.PersonLoginRequestDTO;
import pain_helper_back.dto.PersonLoginResponseDTO;
import pain_helper_back.service.PersonService;
@RestController
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping("/login")
    public PersonLoginResponseDTO login(@RequestBody @Valid PersonLoginRequestDTO loginRequest) {
        return personService.login(loginRequest);
    }

    @PostMapping("/change-credentials")
    public void changeCredentials(@RequestBody @Valid ChangeCredentialsDTO request) {
        personService.changeCredentials(request);
    }

}
