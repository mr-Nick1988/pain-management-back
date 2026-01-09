package pain_helper_back.doctor.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;

import java.time.LocalDate;
import java.util.List;



    public interface DoctorService {

        // ================= PATIENTS ================= //

        /*
         * Create new patient
         * @param patientDto DTO with patient data
         * @return created patient
         * @throws EntityExistsException if email or phone already exist
         */
        PatientDTO createPatient(PatientDTO patientDto);

        /*
         * Get patient by MRN (Medical Record Number)
         * @param mrn unique medical record number
         * @return patient data
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByMrn(String mrn);

        /*
         * Get patient by email
         * @param email patient email
         * @return patient data
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByEmail(String email);

        /*
         * Get patient by phone number
         * @param phoneNumber patient phone number
         * @return patient data
         * @throws NotFoundException if patient not found
         */
        PatientDTO getPatientByPhoneNumber(String phoneNumber);

        /*
         * Search patients by various criteria
         * All parameters are optional and can be combined
         * @param firstName first name (partial match, case insensitive)
         * @param lastName last name (partial match, case insensitive)
         * @param isActive activity status (true/false)
         * @param birthDate birth date (exact match)
         * @param gender gender (MALE/FEMALE/OTHER)
         * @param insurancePolicyNumber insurance policy number (partial match)
         * @param address address (partial match, case insensitive)
         * @param phoneNumber phone number (partial match)
         * @param email email (partial match, case insensitive)
         * @return list of found patients
         */
        List<PatientDTO> searchPatients(
                String firstName,
                String lastName,
                Boolean isActive,
                LocalDate birthDate,
                String gender,
                String insurancePolicyNumber,
                String address,
                String phoneNumber,
                String email
        );

        /*
         * Delete patient by MRN
         * @param mrn unique medical record number
         */
        void deletePatient(String mrn);

        /*
         * Update patient data
         * Only provided (non-null) fields are updated
         * @param mrn unique medical record number
         * @param patientUpdateDto DTO with updated fields
         * @return updated patient data
         * @throws NotFoundException if patient not found
         */
        PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

        // ================= EMR (Electronic Medical Records) ================= //

        /*
         * Create new medical record for patient
         * @param mrn unique patient medical record number
         * @param emrDto DTO with medical data
         * @return created medical record
         * @throws NotFoundException if patient not found
         */
        EmrDTO createEmr(String mrn, EmrDTO emrDto);

        /*
         * Get last medical record of patient
         * @param mrn unique patient medical record number
         * @return last medical record
         * @throws NotFoundException if patient not found
         */
        EmrDTO getLastEmrByPatientMrn(String mrn);

        /*
         * Update last medical record of patient
         * Only provided (non-null) fields are updated
         * @param mrn unique patient medical record number
         * @param emrUpdateDto DTO with updated fields
         * @return updated medical record
         * @throws NotFoundException if patient not found
         */
        EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

        /*
         * Get all medical records of patient (history)
         * @param mrn unique patient medical record number
         * @return list of all patient medical records
         * @throws NotFoundException if patient not found
         */
        List<EmrDTO> getAllEmrByPatientMrn(String mrn);

        // ================= RECOMMENDATIONS ================= //

        /*
         * Get all recommendations with PENDING status
         * Returns recommendations together with VAS (pain level)
         * @return list of recommendations awaiting doctor approval
         */
        List<RecommendationWithVasDTO> getAllPendingRecommendations();

        /*
         * Get last recommendation for patient
         * Returns recommendation together with VAS (pain level)
         * @param mrn unique patient medical record number
         * @return last recommendation with VAS
         * @throws NotFoundException if patient not found
         */
        RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);



        RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto);


        List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn);
    }
