package com.neuroguard.icd10.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One-off loader for the full ICD-10-CM list. Only active under the "icd10-import" profile, so a normal
 * start never imports anything. See docs/clinical-billing.md for the command.
 */
@Component
@Profile("icd10-import")
public class Icd10ImportRunner implements CommandLineRunner {

    private final Icd10ImportService importService;
    private final String file;

    public Icd10ImportRunner(Icd10ImportService importService,
                             @Value("${icd10.import.file:}") String file) {
        this.importService = importService;
        this.file = file;
    }

    /** Fails startup loudly on a bad path: a silently skipped import would look like a successful one. */
    @Override
    public void run(String... args) {
        if (file == null || file.isBlank()) {
            throw new IllegalStateException("Profile icd10-import needs the property icd10.import.file "
                    + "(path to the CMS icd10cm_order_YYYY.txt file)");
        }
        Path path = Path.of(file);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("ICD-10 order file not found or not readable: " + path.toAbsolutePath());
        }
        importService.importFile(path);
    }
}
