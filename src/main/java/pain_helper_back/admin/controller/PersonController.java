package pain_helper_back.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.admin.dto.ChangeCredentialsDTO;
import pain_helper_back.admin.dto.PersonLoginRequestDTO;
import pain_helper_back.admin.dto.PersonLoginResponseDTO;
import pain_helper_back.admin.service.PersonService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
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
