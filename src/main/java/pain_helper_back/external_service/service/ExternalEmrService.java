package pain_helper_back.external_service.service;

import pain_helper_back.external_service.dto.ExternalEmrDTO;
import pain_helper_back.nurse.dto.EmrDto;

public interface ExternalEmrService {
    EmrDto convertToInternal(ExternalEmrDTO externalEmrDTO);
}
