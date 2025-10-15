package pain_helper_back.treatment_protocol.icd_diagnosis.icd_excel_loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pain_helper_back.treatment_protocol.icd_diagnosis.entity.IcdDictionary;
import pain_helper_back.treatment_protocol.icd_diagnosis.repository.IcdDictionaryRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IcdDictionaryLoader implements CommandLineRunner {
    private final IcdDictionaryRepository repo;

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() > 0) {
            log.info(" ICD dictionary already loaded");
            return;
        }
        log.info(" Loading ICD dictionary...");

        String path = "icd_dictionary.xlsx";
        try (InputStream is = new ClassPathResource(path).getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<IcdDictionary> batch = new ArrayList<>(1000);
            int total = 0;

            for (Row row : sheet) {
                if (row == null || row.getRowNum() == 0) continue;

                String code = clean(formatter.formatCellValue(row.getCell(0)));
                String desc = clean(formatter.formatCellValue(row.getCell(1)));

                if (code.isEmpty() || desc.isEmpty()) continue;

                batch.add(new IcdDictionary(code.toUpperCase(), desc));
                total++;

                // Каждые 1000 строк — сохраняем в базу
                if (batch.size() >= 1000) {
                    repo.saveAll(batch);
                    batch.clear();
                    log.info("💾 Saved {} records so far...", total);
                }
            }

            // Сохраняем остаток
            if (!batch.isEmpty()) repo.saveAll(batch);

            log.info("✅ ICD dictionary loaded successfully, total {} records", total);
        }
    }

    // 🔧 Очистка строки от лишних символов
    private String clean(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u00A0\\s]+", " ") // заменяет неразрывные пробелы и множественные пробелы на один
                .replaceAll("[–—]", "-")          // заменяет длинные тире на обычное
                .trim();
    }
}