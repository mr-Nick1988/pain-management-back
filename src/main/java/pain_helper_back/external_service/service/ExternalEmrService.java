package pain_helper_back.external_service.service;

import pain_helper_back.external_service.dto.ExternalEmrDTO;
import pain_helper_back.nurse.dto.EmrDTO;

public interface ExternalEmrService {
    EmrDTO convertToInternal(ExternalEmrDTO externalEmrDTO);
}
