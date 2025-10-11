package pain_helper_back.admin.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.admin.dto.PersonDTO;
import pain_helper_back.admin.dto.PersonRegisterRequestDTO;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.analytics.event.PersonCreatedEvent;
import pain_helper_back.analytics.event.PersonDeletedEvent;
import pain_helper_back.analytics.event.PersonUpdatedEvent;
import pain_helper_back.enums.Roles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminServiceImpl implements AdminService, CommandLineRunner {
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PersonDTO createPerson(PersonRegisterRequestDTO dto) {
        if (personRepository.existsByPersonId(dto.getPersonId())) {
            throw new RuntimeException("Person with this personId already exists");
        }
        if (personRepository.existsByLogin(dto.getLogin())) {
            throw new RuntimeException("Person with this login already exists");
        }
        Person person = modelMapper.map(dto, Person.class);
        person.setTemporaryCredentials(true);
        personRepository.save(person);

        eventPublisher.publishEvent(new PersonCreatedEvent(
                this,
                person.getPersonId(),
                person.getFirstName(),
                person.getLastName(),
                person.getRole().name(),
                "admin", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now()
        ));
        log.info("Person created: personId={}, role={}", person.getPersonId(), person.getRole());
        return modelMapper.map(person, PersonDTO.class);
    }

    @Override
    public PersonDTO updatePerson(String personId, PersonRegisterRequestDTO dto) {
        Person person = personRepository.findByPersonId(personId)
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
        person.setRole(Roles.valueOf(dto.getRole()));
        //Отслеживаем изменения
        Map<String, String> changedFields = new HashMap<>();
        changedFields.put("firstName", person.getFirstName());
        changedFields.put("lastName", person.getLastName());
        changedFields.put("login", person.getLogin());
        changedFields.put("role", person.getRole().name());
        personRepository.save(person);

        eventPublisher.publishEvent(new PersonUpdatedEvent(
                this,
                person.getPersonId(),
                "admin", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                changedFields
        ));

        log.info("Person updated: personId={}, changedFields={}", person.getPersonId(), changedFields.keySet());
        return modelMapper.map(person, PersonDTO.class);
    }

    @Override
    public void deletePerson(String personId) {
        Person person = personRepository.findByPersonId(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        //Сохраняем данные перед удалением
        String firstName = person.getFirstName();
        String lastName = person.getLastName();
        String role = person.getRole().name();

        personRepository.delete(person);
        // Публикуем событие удаления сотрудника
        eventPublisher.publishEvent(new PersonDeletedEvent(
                this,
                personId,
                firstName,
                lastName,
                role,
                "admin", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                "Deleted by admin" // Причина удаления
        ));
        log.warn("Person deleted: personId={}, role={}", personId, role);
    }


    @Override
    @Transactional(readOnly = true)
    public List<PersonDTO> getAllPersons() {
        return personRepository.findAll().stream()
                .map(person -> modelMapper.map(person, PersonDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PersonDTO getPersonByPersonId(String personId) {
        Person person = personRepository.findByPersonId(personId)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return modelMapper.map(person, PersonDTO.class);
    }


    @Override
    @Transactional(readOnly = true)
    public PersonDTO getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return modelMapper.map(person, PersonDTO.class);
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
            admin.setRole(Roles.ADMIN);
            admin.setTemporaryCredentials(false);
            personRepository.save(admin);
            System.out.println("Admin user created successfully");
        } else {
            System.out.println("Admin user already exists");
        }
    }
}