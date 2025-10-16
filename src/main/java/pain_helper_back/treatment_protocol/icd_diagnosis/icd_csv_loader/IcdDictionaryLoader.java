package pain_helper_back.treatment_protocol.icd_diagnosis.icd_csv_loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pain_helper_back.treatment_protocol.icd_diagnosis.entity.IcdDictionary;
import pain_helper_back.treatment_protocol.icd_diagnosis.repository.IcdDictionaryRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IcdDictionaryLoader implements CommandLineRunner {

    private final IcdDictionaryRepository repo;

    @Override
    public void run(String... args) throws Exception {

        //  если таблица уже не пуста — загрузка не выполняется
        if (repo.count() > 0) {
            log.info("ICD dictionary already loaded");
            return;
        }

        log.info("Loading ICD dictionary from CSV...");

        // путь к файлу в resources/
        String path = "icd9cm_2015_converted.csv";

        // открываем поток чтения (Spring -> ClassPathResource)
        try (InputStream is = new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            List<IcdDictionary> batch = new ArrayList<>(1000); // буфер для пакетной вставки
            String line;
            int total = 0;

            //  пропускаем первую строку (заголовок)
            reader.readLine();

            //  читаем построчно весь CSV
            while ((line = reader.readLine()) != null) {

                // делим строку на две части: код и всё остальное
                String[] parts = line.split(",", 2);
                if (parts.length < 2) continue; // пропускаем битые строки

                String code = clean(parts[0]);
                String desc = clean(parts[1]);
                if (code.isEmpty() || desc.isEmpty()) continue; // пропускаем пустые значения

                // создаём объект ICD и добавляем в батч
                batch.add(new IcdDictionary(code.toUpperCase(), desc));
                total++;

                // каждые 1000 строк сохраняем в БД и очищаем буфер
                if (batch.size() >= 1000) {
                    repo.saveAll(batch);
                    batch.clear();
                    log.info(" Saved {} records so far...", total);
                }
            }

            // сохраняем остаток, если он есть
            if (!batch.isEmpty()) repo.saveAll(batch);

            log.info(" ICD dictionary loaded successfully, total {}", total);
        }
    }

    //  Утилита очистки текста
    private String clean(String text) {
        return text == null ? "" :
                text.replaceAll("[\\u00A0\\s]+", " ") // заменяет множественные пробелы и неразрывные на один
                        .replaceAll("[–—]", "-")         // длинные тире на короткое
                        .replaceAll("\"", "")            // убираем кавычки вокруг текста
                        .trim();                         // убираем пробелы по краям
    }
}