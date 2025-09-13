package pain_helper_back.service;

import pain_helper_back.dto.PersonDto;
import pain_helper_back.dto.PersonRegisterRequestDTO;

import java.util.List;

public interface AdminService {
    PersonDto createPerson(PersonRegisterRequestDTO dto);
    List<PersonDto> getAllPersons();
    PersonDto getPersonById(Long id);
    PersonDto updatePerson(Long id, PersonRegisterRequestDTO dto);
    void deletePerson(Long id);
}

