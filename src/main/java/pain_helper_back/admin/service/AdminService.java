package pain_helper_back.admin.service;

import pain_helper_back.admin.dto.PersonDTO;
import pain_helper_back.admin.dto.PersonRegisterRequestDTO;

import java.util.List;

public interface AdminService {
    PersonDTO createPerson(PersonRegisterRequestDTO dto);
    List<PersonDTO> getAllPersons();
    PersonDTO getPersonById(Long id);
    PersonDTO getPersonByPersonId(String personId);
    PersonDTO updatePerson(Long id, PersonRegisterRequestDTO dto);
    void deletePerson(Long id);
}

