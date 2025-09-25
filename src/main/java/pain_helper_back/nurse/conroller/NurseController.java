package pain_helper_back.nurse.conroller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.nurse.dto.*;
import pain_helper_back.nurse.service.NurseService;

import java.util.List;

@RestController
@RequestMapping("/api/nurse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class NurseController {
    private final NurseService nurseService;

    @PostMapping("/patients")
    public PatientDTO createPatient(@Valid @RequestBody PatientDTO patientDto) {
        return nurseService.createPatient(patientDto);
    }

    @GetMapping("/patients/{personId}")
    public PatientDTO getPatientById(@PathVariable String personId) {
        return nurseService.getPatientById(personId);
    }

    @GetMapping("/patients")
    public List<PatientDTO> getAllPatients() {
        return nurseService.getAllPatients();
    }

    @DeleteMapping("/patients/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable String personId) {
        nurseService.deletePatient(personId);
    }

    @PatchMapping("/patients/{personId}")
    public PatientDTO updatePatient(@PathVariable String personId, @Valid @RequestBody PatientUpdateDTO patientUpdateDto) {
        return nurseService.updatePatient(personId, patientUpdateDto);
    }

    @PostMapping("/patients/{personId}/emr")
    public EmrDTO createEmr(@PathVariable String personId, @Valid @RequestBody EmrDTO emrDTO) {
        return nurseService.createEmr(personId, emrDTO);
    }

    @GetMapping("/patients/{personId}/emr")
    public EmrDTO getEmrByPatientId(@PathVariable String personId) {
        return nurseService.getLastEmrByPatientId(personId);
    }

    @PatchMapping("/patients/{personId}/emr")
    public EmrDTO updateEmr(@PathVariable String personId, @Valid @RequestBody EmrUpdateDTO emrUpdateDto) {
        return nurseService.updateEmr(personId, emrUpdateDto);
    }

    @PostMapping("/patients/{personId}/vas")
    public VasDTO createVAS(@PathVariable String personId, @Valid @RequestBody VasDTO vasDto) {
        return nurseService.createVAS(personId, vasDto);
    }

    @PatchMapping("/patients/{personId}/vas")
    public VasDTO updateVAS(@PathVariable String personId, @Valid @RequestBody VasDTO vasDto) {
        return nurseService.updateVAS(personId, vasDto);
    }
//
//    @DeleteMapping("/patients/{personId}/vas")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void deleteVAS(@PathVariable String personId) {
//        nurseService.deleteVAS(personId);
//    }

    @PostMapping("/patients/{personId}/recommendation")
    public RecommendationDTO createRecommendation(@PathVariable String personId, @Valid @RequestBody RecommendationDTO recommendationDTO) {
        return nurseService.createRecommendation(personId);
    }
}