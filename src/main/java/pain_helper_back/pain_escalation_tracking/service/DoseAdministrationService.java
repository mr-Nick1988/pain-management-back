package pain_helper_back.pain_escalation_tracking.service;

import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationRequestDTO;
import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationResponseDTO;
import pain_helper_back.pain_escalation_tracking.dto.DoseHistoryDTO;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;

import java.time.LocalDateTime;
import java.util.List;

public interface DoseAdministrationService {
    DoseAdministration registerDoseAdministration(DoseAdministration doseAdministration);

    DoseAdministrationResponseDTO registerDoseAdministration(String mrn, DoseAdministrationRequestDTO request);

    boolean canAdministerNextDose(String mrn);

    List<DoseHistoryDTO> getDoseHistory(String mrn);

    LocalDateTime getNextDoseTime(String mrn);
}
