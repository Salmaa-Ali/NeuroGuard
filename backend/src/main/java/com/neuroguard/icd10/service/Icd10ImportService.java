package com.neuroguard.icd10.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Loads the CMS ICD-10-CM "order file" (icd10cm_order_YYYY.txt, public domain) into icd10_code.
 *
 * <p>Fixed-width layout (1-based columns): 1-5 order number, 7-13 code (no dot, space padded),
 * 15 header flag (0 = category/header, 1 = billable), 17-76 short description, 78-end long description.
 * Headers are imported too, so 3-character categories such as G40 exist for grouping.
 */
@Service
public class Icd10ImportService {

    private static final Logger log = LoggerFactory.getLogger(Icd10ImportService.class);

    private static final int CODE_START = 6;
    private static final int CODE_END = 13;
    private static final int LONG_DESCRIPTION_START = 77;
    private static final Pattern VALID_CODE = Pattern.compile("[A-Z][0-9][0-9A-Z]{1,5}");
    private static final int BATCH_SIZE = 1000;

    private static final String UPSERT = """
            INSERT INTO icd10_code (code, description, category) VALUES (?, ?, ?)
            ON CONFLICT (code) DO UPDATE
               SET description = EXCLUDED.description, category = EXCLUDED.category, updated_at = now()
            """;

    private final JdbcTemplate jdbc;

    public Icd10ImportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Counts reported after an import. {@code skipped} = malformed lines and repeated codes in the file. */
    public record ImportResult(int linesRead, int inserted, int updated, int unchanged, int skipped) {
    }

    @Transactional
    public ImportResult importFile(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return importStream(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read ICD-10 order file " + file, e);
        }
    }

    /**
     * Idempotent by design: it compares against what is already stored and writes only new or changed
     * codes, so re-running it (or running a newer yearly file) is safe and its counts show what changed.
     * Codes missing from the file are never deleted, because diagnoses and invoices may still reference them.
     * Runs in one transaction so a failure halfway leaves the table as it was.
     */
    @Transactional
    public ImportResult importStream(InputStream in) {
        Map<String, String> fromFile = new LinkedHashMap<>();
        int linesRead = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                linesRead++;
                String[] parsed = parse(line);
                if (parsed == null) {
                    skipped++;
                    log.warn("Skipping malformed ICD-10 line {}: '{}'", linesRead, line);
                } else if (fromFile.putIfAbsent(parsed[0], parsed[1]) != null) {
                    skipped++;
                    log.warn("Skipping repeated ICD-10 code {} on line {}", parsed[0], linesRead);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read ICD-10 order file", e);
        }

        Map<String, String> existing = new HashMap<>();
        jdbc.query("SELECT code, description FROM icd10_code",
                rs -> { existing.put(rs.getString(1), rs.getString(2)); });

        List<Object[]> writes = new ArrayList<>();
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        for (Map.Entry<String, String> e : fromFile.entrySet()) {
            String current = existing.get(e.getKey());
            if (current == null) {
                inserted++;
            } else if (!current.equals(e.getValue())) {
                updated++;
            } else {
                unchanged++;
                continue;
            }
            writes.add(new Object[]{e.getKey(), e.getValue(), e.getKey().substring(0, 3)});
        }
        for (int i = 0; i < writes.size(); i += BATCH_SIZE) {
            jdbc.batchUpdate(UPSERT, writes.subList(i, Math.min(i + BATCH_SIZE, writes.size())));
        }

        ImportResult result = new ImportResult(linesRead, inserted, updated, unchanged, skipped);
        log.info("ICD-10 import finished: {}", result);
        return result;
    }

    /** Returns {code, longDescription}, or null if the line does not follow the order-file layout. */
    static String[] parse(String line) {
        if (line.length() <= LONG_DESCRIPTION_START) {
            return null;
        }
        String code = line.substring(CODE_START, CODE_END).trim();
        String description = line.substring(LONG_DESCRIPTION_START).trim();
        if (!VALID_CODE.matcher(code).matches() || description.isEmpty()) {
            return null;
        }
        return new String[]{code, description};
    }
}
