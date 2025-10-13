package pain_helper_back.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.admin.dto.PersonDTO;
import pain_helper_back.admin.dto.PersonRegisterRequestDTO;
import pain_helper_back.admin.service.AdminServiceImpl;
import pain_helper_back.common.patients.dto.PatientDTO;
import pain_helper_back.validation.ValidationGroups;

import java.util.List;

@RestController
@RequestMapping("api/admin/persons")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminController {
    private final AdminServiceImpl adminService;

    @PostMapping
    public PersonDTO createPerson(@RequestBody @Validated(ValidationGroups.Create.class) PersonRegisterRequestDTO personRegisterRequestDTO) {
        return adminService.createPerson(personRegisterRequestDTO);
    }

    @GetMapping
    public List<PersonDTO> getAllPersons() {
        return adminService.getAllPersons();
    }

    @GetMapping("/patients")
    public List<PatientDTO> getAllPatients() {
        return adminService.getAllPatients();
    }

    @GetMapping("/{id}")
    public PersonDTO getPersonById(@PathVariable Long id) {
        return adminService.getPersonById(id);
    }

    @PatchMapping("/{personId}")
    public PersonDTO updatePerson(@PathVariable String personId, @RequestBody @Validated(ValidationGroups.Update.class) PersonRegisterRequestDTO personRegisterRequestDTO) {
        return adminService.updatePerson(personId, personRegisterRequestDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePerson(@PathVariable String id) {
        adminService.deletePerson(id);
    }
}