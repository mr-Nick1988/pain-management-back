package pain_helper_back.treatment_protocol.icd_diagnosis.conroller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pain_helper_back.treatment_protocol.icd_diagnosis.entity.IcdDictionary;
import pain_helper_back.treatment_protocol.icd_diagnosis.repository.IcdDictionaryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/icd")
@RequiredArgsConstructor
public class IcdDictionaryController {
    private final IcdDictionaryRepository repo;

    @GetMapping("/search")      // Автодополнение по описанию
    public List<IcdDictionary> search(@RequestParam String query) {
        // Находим первые 20 диагнозов, где описание содержит фразу (без учёта регистра)
        return repo.findTop20ByDescriptionContainingIgnoreCase(query);
    }
}
