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

    @GetMapping("/search")
    public List<IcdDictionary> search(@RequestParam String query) {
        String q = query.trim();

        // если похоже на код (буква + цифры, может быть точка)
        boolean looksLikeCode = q.matches("^[A-Za-z]?[0-9]{2,3}(\\.[0-9A-Za-z]{0,4})?$");

        if (looksLikeCode) {
            return repo.findTop20ByCodeStartingWithIgnoreCase(q);
        } else {
            return repo.findTop20ByDescriptionContainingIgnoreCase(q);
        }
    }
}
