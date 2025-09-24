package pain_helper_back.external_service.service;

import pain_helper_back.external_service.dto.ExternalEmrDTO;
import pain_helper_back.nurse.dto.EmrDTO;

import java.time.LocalDate;

public class ExternalServiceImpl implements ExternalEmrService {

    @Override
    public EmrDTO convertToInternal(ExternalEmrDTO externalEmrDTO) {
        EmrDTO emrDTO = new EmrDTO();
        // Здесь "маппинг" из внешней структуры в твою внутреннюю
        emrDTO.setGfr(externalEmrDTO.getCreatinine() != null ? "OK" : "Unknown");
        emrDTO.setChildPughScore("N/A"); // пока заглушка, в будущем можно конвертить
        emrDTO.setPlt(externalEmrDTO.getHemoglobin()); // временно подставим
        emrDTO.setWbc(5.0); // нет данных во внешней системе → дефолт
        emrDTO.setSat(98.0); // тоже дефолт
        emrDTO.setSodium(140.0); // дефолт
        emrDTO.setCreatedAt(externalEmrDTO.getRecordDate() != null ? externalEmrDTO.getRecordDate() : LocalDate.now());
        return emrDTO;
    }
}
