package pain_helper_back.common.persons.service;

import pain_helper_back.common.persons.dto.ChangeCredentialsDTO;
import pain_helper_back.common.persons.dto.PersonLoginRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pain_helper_back.common.persons.dto.PersonLoginResponseDTO;
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
        personRepository.save(person);
    }
}
