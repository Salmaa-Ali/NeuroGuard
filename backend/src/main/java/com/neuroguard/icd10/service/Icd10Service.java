package com.neuroguard.icd10.service;

import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.icd10.dto.Icd10Response;
import com.neuroguard.icd10.entity.Icd10Code;
import com.neuroguard.icd10.repository.Icd10CodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class Icd10Service {

    private final Icd10CodeRepository repository;

    public Icd10Service(Icd10CodeRepository repository) {
        this.repository = repository;
    }

    /**
     * Users type codes the way they appear on paper ("g40.9"), but the table stores the CMS form
     * ("G409"), so every lookup goes through this one normalisation.
     */
    public static String normalize(String code) {
        return code == null ? null : code.trim().replace(".", "").toUpperCase(Locale.ROOT);
    }

    /**
     * One search box serves both clinicians who know the code and those who only know the condition,
     * so {@code q} is matched as a code prefix OR as a description substring. Blank {@code q} lists all.
     */
    @Transactional(readOnly = true)
    public PageResponse<Icd10Response> search(String q, int page, int size) {
        PageRequest pageable = Pages.of(page, size, Sort.by("code"));
        Page<Icd10Code> result;
        if (q == null || q.isBlank()) {
            result = repository.findAll(pageable);
        } else {
            String codePrefix = escapeLike(normalize(q)) + "%";
            String descriptionPattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
            result = repository.search(codePrefix, descriptionPattern, pageable);
        }
        return PageResponse.of(result, Icd10Response::from);
    }

    @Transactional(readOnly = true)
    public Icd10Response get(String code) {
        return Icd10Response.from(find(normalize(code))
                .orElseThrow(() -> new ResourceNotFoundException("ICD-10 code", code)));
    }

    /**
     * Lookup for other modules (diagnoses, invoices) that must reject unknown codes with 400 rather
     * than let the foreign key turn them into a generic 409.
     */
    @Transactional(readOnly = true)
    public Optional<Icd10Code> find(String code) {
        String normalized = normalize(code);
        return normalized == null || normalized.isEmpty() ? Optional.empty() : repository.findById(normalized);
    }

    /** User input must not act as a wildcard ("%" would otherwise match every code). */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
