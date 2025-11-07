package pain_helper_back.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.admin.dto.PersonDTO;
import pain_helper_back.admin.dto.PersonRegisterRequestDTO;
import pain_helper_back.admin.service.AdminServiceImpl;
import pain_helper_back.common.patients.dto.PatientDTO;
import pain_helper_back.security.JwtAuthenticationFilter;
import pain_helper_back.security.RequireRole;
import pain_helper_back.validation.ValidationGroups;

import java.util.List;

@RestController
@RequestMapping("api/admin/persons")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final AdminServiceImpl adminService;

    @PostMapping
    @RequireRole("ADMIN")
    public PersonDTO createPerson(
            @RequestBody @Validated(ValidationGroups.Create.class) PersonRegisterRequestDTO personRegisterRequestDTO,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/admin/persons - createdBy={}", userDetails.getPersonId());
        return adminService.createPerson(personRegisterRequestDTO);
    }

    @GetMapping
    @RequireRole("ADMIN")
    public List<PersonDTO> getAllPersons(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/admin/persons - requestedBy={}", userDetails.getPersonId());
        return adminService.getAllPersons();
    }

    @GetMapping("/patients")
    @RequireRole("ADMIN")
    public List<PatientDTO> getAllPatients(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/admin/persons/patients - requestedBy={}", userDetails.getPersonId());
        return adminService.getAllPatients();
    }

    @GetMapping("/{id}")
    @RequireRole("ADMIN")
    public PersonDTO getPersonById(
            @PathVariable Long id,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/admin/persons/{} - requestedBy={}", id, userDetails.getPersonId());
        return adminService.getPersonById(id);
    }

    @PatchMapping("/{personId}")
    @RequireRole("ADMIN")
    public PersonDTO updatePerson(
            @PathVariable String personId,
            @RequestBody @Validated(ValidationGroups.Update.class) PersonRegisterRequestDTO personRegisterRequestDTO,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/admin/persons/{} - updatedBy={}", personId, userDetails.getPersonId());
        return adminService.updatePerson(personId, personRegisterRequestDTO);
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public void deletePerson(
            @PathVariable String id,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("DELETE /api/admin/persons/{} - deletedBy={}", id, userDetails.getPersonId());
        adminService.deletePerson(id);
    }
}