package pain_helper_back.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.dto.PersonDTO;
import pain_helper_back.dto.PersonRegisterRequestDTO;
import pain_helper_back.entity.Person;
import pain_helper_back.repository.PersonRepository;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService, CommandLineRunner {
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;

    @Override
    public PersonDTO createPerson(PersonRegisterRequestDTO dto) {
        if (personRepository.existsByPersonId(dto.getPersonId())) {
            throw new RuntimeException("Person with this personId already exists");
        }
        if (personRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Person with this login already exists");
        }
        Person person = new Person();
        person.setPersonId(dto.getPersonId());
        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setLogin(dto.getLogin());
        person.setPassword(dto.getPassword());
        person.setRole(dto.getRole());
        person.setTemporaryCredentials(true);

        personRepository.save(person);
        return modelMapper.map(person, PersonDTO.class);
    }

    @Override
    public List<PersonDTO> getAllPersons() {
        return personRepository.findAll().stream()
                .map(person -> modelMapper.map(person, PersonDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public PersonDTO getPersonByPersonId(String personId) {
        Person person = personRepository.findByPersonId(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return modelMapper.map(person, PersonDTO.class);
    }


    @Override
    public PersonDTO getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return modelMapper.map(person, PersonDTO.class);
    }

    @Override
    public PersonDTO updatePerson(Long id, PersonRegisterRequestDTO dto) {
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

        personRepository.save(person);
        return modelMapper.map(person, PersonDTO.class);
    }

    @Override
    public void deletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new RuntimeException("Person not found");
        }
        personRepository.deleteById(id);
    }

    @Override
    public void run(String... args) throws Exception {
        if (!personRepository.existsByLogin("admin")) {
            Person admin = new Person();
            admin.setPersonId("admin123");
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setLogin("admin");
            admin.setPassword("admin");
            admin.setRole("ADMIN");
            admin.setTemporaryCredentials(false);
            personRepository.save(admin);
            System.out.println("Admin user created successfully");
        } else {
            System.out.println("Admin user already exists");
        }
    }
}