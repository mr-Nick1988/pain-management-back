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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NurseServiceImpl implements NurseService {
    private final PatientRepository patientRepository;
    private final TreatmentProtocolService treatmentProtocolService;
    private final EmrRepository emrRepository;
    private final RecommendationRepository recommendationRepository;
    private final ModelMapper modelMapper;


    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with this " + mrn + " not found"));
    }

    @Override
    @Transactional
    public PatientDTO createPatient(PatientDTO patientDto) {
        if (patientDto.getEmail() != null && patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EntityExistsException("Patient with this email already exists");
        }
        if (patientRepository.existsByPhoneNumber(patientDto.getPhoneNumber())) {
            throw new EntityExistsException("Patient with this phone number already exists");
        }
        Patient patient = modelMapper.map(patientDto, Patient.class);
        patientRepository.save(patient);
        String mrn = String.format("%06d", patient.getId());
        patient.setMrn(mrn);
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
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
    @Transactional(readOnly = true)
    public PatientDTO getPatientByEmail(String email) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Patient with this email not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByPhoneNumber(String phoneNumber) {
        Patient patient = patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Patient with this phone number not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDTO> getPatientsByFullName(String firstName, String lastName) {
        List<Patient> patients = patientRepository.getPatientsByFirstNameAndLastName(firstName, lastName);
        return patients.stream().map(patient -> modelMapper.map(patient, PatientDTO.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePatient(String mrn) {
        patientRepository.deleteByMrn(mrn);
    }

    @Override
    @Transactional
    public PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto) {
        Patient patient = findPatientOrThrow(mrn);

        if (patientUpdateDto.getFirstName() != null) patient.setFirstName(patientUpdateDto.getFirstName());
        if (patientUpdateDto.getLastName() != null) patient.setLastName(patientUpdateDto.getLastName());
        if (patientUpdateDto.getGender() != null) patient.setGender(patientUpdateDto.getGender());
        if (patientUpdateDto.getInsurancePolicyNumber() != null)
            patient.setInsurancePolicyNumber(patientUpdateDto.getInsurancePolicyNumber());
        if (patientUpdateDto.getPhoneNumber() != null) patient.setPhoneNumber(patientUpdateDto.getPhoneNumber());
        if (patientUpdateDto.getEmail() != null) patient.setEmail(patientUpdateDto.getEmail());
        if (patientUpdateDto.getAddress() != null) patient.setAddress(patientUpdateDto.getAddress());
        if (patientUpdateDto.getAdditionalInfo() != null)
            patient.setAdditionalInfo(patientUpdateDto.getAdditionalInfo());

        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional
    public EmrDTO createEmr(String mrn, EmrDTO emrDto) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);
        patient.getEmr().add(emr);
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EmrDTO getLastEmrByPatientMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional
    public EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        if (emrUpdateDto.getHeight() != null) emr.setHeight(emrUpdateDto.getHeight());
        if (emrUpdateDto.getWeight() != null) emr.setWeight(emrUpdateDto.getWeight());
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
    public VasDTO createVAS(String mrn, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(mrn);
        Vas vas = modelMapper.map(vasDto, Vas.class);
        vas.setPatient(patient);
        patient.getVas().add(vas);
        return modelMapper.map(vas, VasDTO.class);
    }

    @Override
    @Transactional
    public VasDTO updateVAS(String mrn, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(mrn);
        Vas vas = patient.getVas().getLast();
        vas.setPainLevel(vasDto.getPainLevel());
        return modelMapper.map(vas, VasDTO.class);
    }

    @Override
    @Transactional
    public void deleteVAS(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        patient.getVas().removeLast();
    }

    @Override
    @Transactional
    public RecommendationDTO createRecommendation(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
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