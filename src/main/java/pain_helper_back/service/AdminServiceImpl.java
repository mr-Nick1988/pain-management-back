package pain_helper_back.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.dto.PersonDto;
import pain_helper_back.dto.PersonRegisterRequestDTO;
import pain_helper_back.entity.Person;
import pain_helper_back.repository.PersonRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;

    @Override
    public PersonDto createPerson(PersonRegisterRequestDTO dto) {
        if (personRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Person with this login already exists");
        }

        Person person = new Person();
        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setLogin(dto.getLogin());
        person.setPassword(dto.getPassword());
        person.setRole(dto.getRole());
        person.setTemporaryCredentials(true);

        Person savedPerson = personRepository.save(person);
        return modelMapper.map(savedPerson, PersonDto.class);
    }

    @Override
    public List<PersonDto> getAllPersons() {
        return personRepository.findAll().stream()
                .map(person -> modelMapper.map(person, PersonDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public PersonDto getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return modelMapper.map(person, PersonDto.class);
    }

    @Override
    public PersonDto updatePerson(Long id, PersonRegisterRequestDTO dto) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        // Проверяем, не занят ли новый логин
        if (!person.getLogin().equals(dto.getLogin()) &&
                personRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Person with this login already exists");
        }

        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setLogin(dto.getLogin());

        // Обновляем пароль только если он предоставлен
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            person.setPassword(dto.getPassword());
            person.setTemporaryCredentials(true);
        }

        person.setRole(dto.getRole());

        Person updatedPerson = personRepository.save(person);
        return modelMapper.map(updatedPerson, PersonDto.class);
    }

    @Override
    public void deletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new RuntimeException("Person not found");
        }
        personRepository.deleteById(id);
    }
}