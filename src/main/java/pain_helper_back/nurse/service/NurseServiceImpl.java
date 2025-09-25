package pain_helper_back.nurse.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.nurse.entity.Recommendation;
import pain_helper_back.nurse.dto.*;
import pain_helper_back.nurse.dto.exceptions.EntityExistsException;
import pain_helper_back.nurse.dto.exceptions.NotFoundException;
import pain_helper_back.nurse.entity.Emr;
import pain_helper_back.nurse.entity.Patient;
import pain_helper_back.nurse.entity.Vas;
import pain_helper_back.nurse.repository.EmrRepository;
import pain_helper_back.nurse.repository.PatientRepository;
import pain_helper_back.nurse.repository.RecommendationRepository;
import pain_helper_back.treatment_protocol.service.TreatmentProtocolService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NurseServiceImpl implements NurseService {
    private final PatientRepository patientRepository;
    private final TreatmentProtocolService treatmentProtocolService;
    private final EmrRepository emrRepository;
    private final RecommendationRepository recommendationRepository;
    private final ModelMapper modelMapper;


    private Patient findPatientOrThrow(String personId) {
        return patientRepository.findByPersonId(personId)
                .orElseThrow(() -> new NotFoundException("Patient with this " + personId + " not found"));
    }

    @Override
    @Transactional
    public PatientDTO createPatient(PatientDTO patientDto) {
        String personId = patientDto.getPersonId();
        if (patientRepository.existsByPersonId(personId)) {
            throw new EntityExistsException("Patient with this " + personId + " already exists");
        }
        Patient patient = modelMapper.map(patientDto, Patient.class);
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientById(String personId) {
        Patient patient = findPatientOrThrow(personId);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream()
                .map(patient -> modelMapper.map(patient, PatientDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePatient(String personId) {
        patientRepository.deleteByPersonId(personId);
    }

    @Override
    @Transactional
    public PatientDTO updatePatient(String personId, PatientUpdateDTO patientUpdateDto) {
        Patient patient = findPatientOrThrow(personId);

        if (patientUpdateDto.getFirstName() != null) patient.setFirstName(patientUpdateDto.getFirstName());
        if (patientUpdateDto.getLastName() != null) patient.setLastName(patientUpdateDto.getLastName());
        if (patientUpdateDto.getGender() != null) patient.setGender(patientUpdateDto.getGender());
        if (patientUpdateDto.getWeight() != null) patient.setWeight(patientUpdateDto.getWeight());
        if (patientUpdateDto.getHeight() != null) patient.setHeight(patientUpdateDto.getHeight());

        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional
    public EmrDTO createEmr(String personId, EmrDTO emrDto) {
        Patient patient = findPatientOrThrow(personId);
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);
        patient.getEmr().add(emr);
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EmrDTO getLastEmrByPatientId(String personId) {
        Patient patient = findPatientOrThrow(personId);
        Emr emr = patient.getEmr().getLast();
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional
    public EmrDTO updateEmr(String personId, EmrUpdateDTO emrUpdateDto) {
        Patient patient = findPatientOrThrow(personId);
        Emr emr = patient.getEmr().getLast();

        if (emrUpdateDto.getGfr() != null) emr.setGfr(emrUpdateDto.getGfr());
        if (emrUpdateDto.getSat() != null) emr.setSat(emrUpdateDto.getSat());
        if (emrUpdateDto.getPlt() != null) emr.setPlt(emrUpdateDto.getPlt());
        if (emrUpdateDto.getWbc() != null) emr.setWbc(emrUpdateDto.getWbc());
        if (emrUpdateDto.getChildPughScore() != null) emr.setChildPughScore(emrUpdateDto.getChildPughScore());
        if (emrUpdateDto.getSodium() != null) emr.setSodium(emrUpdateDto.getSodium());

        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional
    public VasDTO createVAS(String personId, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(personId);
        Vas vas = modelMapper.map(vasDto, Vas.class);
        vas.setPatient(patient);
        patient.getVas().add(vas);
        return modelMapper.map(vas, VasDTO.class);
    }

    @Override
    @Transactional
    public VasDTO updateVAS(String personId, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(personId);
        Vas vas = patient.getVas().getLast();
        vas.setPainLevel(vasDto.getPainLevel());
        return modelMapper.map(vas, VasDTO.class);
    }

//    @Override
//    @Transactional
//    public void deleteVAS(String personId) {
//        Patient patient = findPatientOrThrow(personId);
//        Vas vas = patient.getVas().removeLast();
//    }

    @Override
    @Transactional
    public RecommendationDTO createRecommendation(String personId) {
        Patient patient = findPatientOrThrow(personId);
        Emr emr = patient.getEmr().getLast();
        Vas vas = patient.getVas().getLast();

        // 🔹 Алгоритм генерации рекомендации
        Recommendation recommendation = treatmentProtocolService.generateRecommendation(emr, vas, patient);

        recommendation.setPatient(patient);
        patient.getRecommendations().add(recommendation);
        patientRepository.save(patient);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }
}