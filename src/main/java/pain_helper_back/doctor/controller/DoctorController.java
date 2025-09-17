package pain_helper_back.doctor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.doctor.service.DoctorService;

import java.util.List;


@RestController
@RequestMapping("api/doctor")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    // === RECOMMENDATIONS ENDPOINTS ===

    @GetMapping("/recommendations")
    public List<RecommendationDTO> getAllRecommendations() {
        return doctorService.getAllRecommendations();
    }

    @GetMapping("/recommendations/{id}")
    public RecommendationDTO getRecommendationById(@PathVariable Long id) {
        return doctorService.getRecommendationById(id);
    }

    @PostMapping("/recommendations")
    public RecommendationDTO createRecommendation(
            @RequestBody @Valid RecommendationRequestDTO dto,
            @RequestParam(defaultValue = "system") String createdBy) {
        return doctorService.createRecommendation(dto, createdBy);
    }

    @PostMapping("/recommendations/{id}/approve")
    public RecommendationDTO approveRecommendation(
            @PathVariable Long id,
            @RequestBody RecommendationApprovalDTO dto,
            @RequestParam(defaultValue = "system") String approvedBy) {
        return doctorService.approveRecommendation(id, dto, approvedBy);
    }

    @PostMapping("/recommendations/{id}/reject")
    public RecommendationDTO rejectRecommendation(
            @PathVariable Long id,
            @RequestBody RecommendationApprovalDTO dto,
            @RequestParam(defaultValue = "system") String rejectedBy) {
        return doctorService.rejectRecommendation(id, dto, rejectedBy);
    }

    @PatchMapping("/recommendations/{id}")
    public RecommendationDTO updateRecommendation(
            @PathVariable Long id,
            @RequestBody @Valid RecommendationRequestDTO dto,
            @RequestParam(defaultValue = "system") String updatedBy) {
        return doctorService.updateRecommendation(id, dto, updatedBy);
    }

    @DeleteMapping("/recommendations/{id}")
    public RecommendationDTO deleteRecommendation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String deletedBy) {
        return doctorService.deleteRecommendation(id, deletedBy);
    }

    // === PATIENTS ENDPOINTS ===

    @GetMapping("/patients")
    public List<PatientResponseDTO> getAllPatients() {
        return doctorService.getAllPatients();
    }

    @GetMapping("/patients/{id}")
    public PatientResponseDTO getPatientById(@PathVariable Long id) {
        return doctorService.getPatientById(id);
    }

    @PostMapping("/patients")
    public PatientResponseDTO createPatient(@RequestBody @Valid PatientCreationDTO dto, @RequestParam(defaultValue = "system") String createdBy) {
        return doctorService.createPatient(dto, createdBy);
    }

    @PatchMapping("/patients/{id}")
    public PatientResponseDTO updatePatient(@PathVariable Long id, @RequestBody PatientResponseDTO dto, @RequestParam(defaultValue = "system") String updatedBy) {
        return doctorService.updatePatient(id, dto, updatedBy);
    }

    @DeleteMapping("/patients/{id}")
    public PatientResponseDTO deletePatient(@PathVariable Long id, @RequestParam(defaultValue = "system") String deletedBy) {
        return doctorService.deletePatient(id, deletedBy);
    }
}
