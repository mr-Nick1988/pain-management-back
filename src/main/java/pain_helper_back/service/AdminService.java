package pain_helper_back.service;

import pain_helper_back.dto.PersonDTO;
import pain_helper_back.dto.PersonRegisterRequestDTO;

import java.util.List;

public interface AdminService {
    PersonDTO createPerson(PersonRegisterRequestDTO dto);
    List<PersonDTO> getAllPersons();
    PersonDTO getPersonById(Long id);
    PersonDTO getPersonByPersonId(String personId);
    PersonDTO updatePerson(Long id, PersonRegisterRequestDTO dto);
    void deletePerson(Long id);
}

