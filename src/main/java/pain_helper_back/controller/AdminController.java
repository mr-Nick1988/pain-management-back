package pain_helper_back.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.dto.PersonDto;
import pain_helper_back.dto.PersonRegisterRequestDTO;
import pain_helper_back.service.AdminServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/admin/persons")
@RequiredArgsConstructor
public class AdminController {
    private final AdminServiceImpl adminService;

    @PostMapping
    public PersonDto createPerson(@RequestBody @Valid PersonRegisterRequestDTO personRegisterRequestDTO) {
        return adminService.createPerson(personRegisterRequestDTO);
    }

    @GetMapping
    public List<PersonDto> getAllPersons() {
        return adminService.getAllPersons();
    }

    @GetMapping("/{id}")
    public PersonDto getPersonById(@PathVariable Long id) {
        return adminService.getPersonById(id);
    }

    @PatchMapping("/{id}")
    public PersonDto updatePerson(@PathVariable Long id, @RequestBody @Valid PersonRegisterRequestDTO personRegisterRequestDTO) {
        return adminService.updatePerson(id, personRegisterRequestDTO);
    }

    @DeleteMapping("/{id}")
    public void deletePerson(@PathVariable Long id) {
        adminService.deletePerson(id);
    }
}