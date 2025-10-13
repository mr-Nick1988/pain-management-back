package pain_helper_back.admin.service;

import pain_helper_back.admin.dto.PersonDTO;
import pain_helper_back.admin.dto.PersonRegisterRequestDTO;
import pain_helper_back.common.patients.dto.PatientDTO;

import java.util.List;

public interface AdminService {
    PersonDTO createPerson(PersonRegisterRequestDTO dto);
    List<PersonDTO> getAllPersons();
    List<PatientDTO> getAllPatients();
    PersonDTO getPersonById(Long id);
    PersonDTO getPersonByPersonId(String personId);
    PersonDTO updatePerson(String personId, PersonRegisterRequestDTO dto);
    void deletePerson(String personId);
}

