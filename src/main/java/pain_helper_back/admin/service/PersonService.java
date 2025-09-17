package pain_helper_back.admin.service;

import pain_helper_back.admin.dto.ChangeCredentialsDTO;
import pain_helper_back.admin.dto.PersonLoginRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pain_helper_back.admin.dto.PersonLoginResponseDTO;
import org.modelmapper.ModelMapper;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.admin.entity.Person;


@Service
@RequiredArgsConstructor
public class PersonService {
    private final ModelMapper modelMapper;
    private final PersonRepository personRepository;

    public PersonLoginResponseDTO login(PersonLoginRequestDTO loginRequest) {
        Person person = personRepository.findByLogin(loginRequest.getLogin())
                .orElseThrow(() -> new RuntimeException("Invalid login or password"));

        if (!person.getPassword().equals(loginRequest.getPassword())) {
            throw new RuntimeException("Invalid login or password");
        }
        String token = "fake-token" + System.currentTimeMillis();
        PersonLoginResponseDTO response = new PersonLoginResponseDTO();
        response.setToken(token);
        response.setRole(person.getRole());
        response.setTemporaryCredentials(person.isTemporaryCredentials());

        return response;

    }

    public void changeCredentials(ChangeCredentialsDTO request) {
        Person person = personRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // В дальнейшем здесь должна быть проверка пароля с использованием хеширования
        if (!person.getPassword().equals(request.getOldPassword())) {
            throw new RuntimeException("Invalid old password");
        }
        person.setPassword(request.getNewPassword());
        person.setTemporaryCredentials(false);
        personRepository.save(person);
    }
}
