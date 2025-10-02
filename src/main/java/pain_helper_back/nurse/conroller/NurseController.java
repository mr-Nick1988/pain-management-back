package pain_helper_back.nurse.conroller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.nurse.service.NurseService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/nurse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class NurseController {
    private final NurseService nurseService;

    @PostMapping("/patients")
    public PatientDTO createPatient(@Valid @RequestBody PatientDTO patientDto) {
        return nurseService.createPatient(patientDto);
    }

    @GetMapping("/patients/mrn/{mrn}")
    public PatientDTO getPatientByMrn(@PathVariable String mrn) {
        return nurseService.getPatientByMrn(mrn);
    }

    @GetMapping("/patients/email/{email}")
    public PatientDTO getPatientByEmail(@PathVariable String email) {
        return nurseService.getPatientByEmail(email);
    }

    @GetMapping("/patients/phoneNumber/{phoneNumber}")
    public PatientDTO getPatientByPhoneNumber(@PathVariable String phoneNumber) {
        return nurseService.getPatientByPhoneNumber(phoneNumber);
    }


    @GetMapping("/patients")
    public List<PatientDTO> getPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false)@DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate birthDate
    ) {
        return nurseService.searchPatients(firstName, lastName, isActive, birthDate);
    }


    @DeleteMapping("/patients/{mrn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable String mrn) {
        nurseService.deletePatient(mrn);
    }

    @PatchMapping("/patients/{mrn}")
    public PatientDTO updatePatient(@PathVariable String mrn, @Valid @RequestBody PatientUpdateDTO patientUpdateDto) {
        return nurseService.updatePatient(mrn, patientUpdateDto);
    }

    @PostMapping("/patients/{mrn}/emr")
    public EmrDTO createEmr(@PathVariable String mrn, @Valid @RequestBody EmrDTO emrDTO) {
        return nurseService.createEmr(mrn, emrDTO);
    }

    @GetMapping("/patients/{mrn}/emr")
    public EmrDTO getEmrByPatintMrn(@PathVariable String mrn) {
        return nurseService.getLastEmrByPatientMrn(mrn);
    }

    @PatchMapping("/patients/{mrn}/emr")
    public EmrDTO updateEmr(@PathVariable String mrn, @Valid @RequestBody EmrUpdateDTO emrUpdateDto) {
        return nurseService.updateEmr(mrn, emrUpdateDto);
    }

    @PostMapping("/patients/{mrn}/vas")
    public VasDTO createVAS(@PathVariable String mrn, @Valid @RequestBody VasDTO vasDto) {
        return nurseService.createVAS(mrn, vasDto);
    }

    @PatchMapping("/patients/{mrn}/vas")
    public VasDTO updateVAS(@PathVariable String mrn, @Valid @RequestBody VasDTO vasDto) {
        return nurseService.updateVAS(mrn, vasDto);
    }

    @DeleteMapping("/patients/{mrn}/vas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVAS(@PathVariable String mrn) {
        nurseService.deleteVAS(mrn);
    }

    @PostMapping("/patients/{mrn}/recommendation")
    public RecommendationDTO createRecommendation(@PathVariable String mrn, @Valid @RequestBody RecommendationDTO recommendationDTO) {
        return nurseService.createRecommendation(mrn);
    }
}