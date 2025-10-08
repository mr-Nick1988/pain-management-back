package pain_helper_back.treatment_protocol_test_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.VasRepository;
import pain_helper_back.treatment_protocol.service.TreatmentProtocolService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/treatment-protocol")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TreatmentProtocolTestController {

    private final TreatmentProtocolService treatmentProtocolService;
    private final PatientRepository patientRepository;
    private final VasRepository vasRepository;

    /**
     * Тест для одного пациента
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> testRecommendation(
            @RequestParam Long patientId,
            @RequestParam Integer painLevel) {

        log.info("Testing: patientId={}, painLevel={}", patientId, painLevel);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));

        Vas vas = new Vas();
        vas.setPatient(patient);
        vas.setPainLevel(painLevel);
        vas.setCreatedBy("test");
        vas.setCreatedAt(LocalDateTime.now());
        vasRepository.save(vas);

        Recommendation recommendation = treatmentProtocolService.generateRecommendation(vas, patient);

        Map<String, Object> response = new HashMap<>();
        response.put("patientId", patientId);
        response.put("patientInfo", buildPatientInfo(patient));
        response.put("painLevel", painLevel);
        response.put("recommendation", recommendation);

        return ResponseEntity.ok(response);
    }

    /**
     * Получить список моковых пациентов
     */
    @GetMapping("/patients/mock")
    public ResponseEntity<Map<String, Object>> getMockPatients() {
        var patients = patientRepository.findAll().stream()
                .filter(p -> p.getMrn() != null && p.getMrn().startsWith("EMR-"))
                .map(this::buildPatientInfo)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalPatients", patients.size());
        response.put("patients", patients);

        return ResponseEntity.ok(response);
    }

    /**
     * Batch тест для всех моковых пациентов
     */
    @PostMapping("/batch-test")
    public ResponseEntity<Map<String, Object>> batchTest(@RequestParam Integer painLevel) {
        var patients = patientRepository.findAll().stream()
                .filter(p -> p.getMrn() != null && p.getMrn().startsWith("EMR-"))
                .toList();

        var results = patients.stream()
                .map(patient -> {
                    Vas vas = new Vas();
                    vas.setPatient(patient);
                    vas.setPainLevel(painLevel);
                    vas.setCreatedBy("batch-test");
                    vas.setCreatedAt(LocalDateTime.now());
                    vasRepository.save(vas);

                    Recommendation recommendation = treatmentProtocolService.generateRecommendation(vas, patient);

                    Map<String, Object> result = new HashMap<>();
                    result.put("patientId", patient.getId());
                    result.put("patientInfo", buildPatientInfo(patient));
                    result.put("recommendation", recommendation);
                    return result;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("painLevel", painLevel);
        response.put("totalPatients", results.size());
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildPatientInfo(Patient patient) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", patient.getId());
        info.put("mrn", patient.getMrn());
        info.put("firstName", patient.getFirstName());
        info.put("lastName", patient.getLastName());
        info.put("age", patient.getAge());

        Double weight = null;
        if (patient.getEmr() != null && !patient.getEmr().isEmpty()) {
            weight = patient.getEmr().stream()
                    .max(Comparator.comparing(Emr::getCreatedAt))
                    .map(Emr::getWeight)
                    .orElse(null);
        }
        info.put("weight", weight);

        if (patient.getEmr() != null && !patient.getEmr().isEmpty()) {
            var emr = patient.getEmr().get(patient.getEmr().size() - 1);
            info.put("gfr", emr.getGfr());
            info.put("childPughScore", emr.getChildPughScore());
            info.put("plt", emr.getPlt());
            info.put("wbc", emr.getWbc());
            info.put("sodium", emr.getSodium());
            info.put("sat", emr.getSat());
            info.put("height", emr.getHeight());
        }

        return info;
    }
}