package com.neuroguard.icd10.controller;

import com.neuroguard.common.PageResponse;
import com.neuroguard.icd10.dto.Icd10Response;
import com.neuroguard.icd10.service.Icd10Service;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/icd10")
@PreAuthorize(Authz.ANY_STAFF)
@Tag(name = "ICD-10")
public class Icd10Controller {

    private final Icd10Service icd10Service;

    public Icd10Controller(Icd10Service icd10Service) {
        this.icd10Service = icd10Service;
    }

    /** {@code q} matches a code prefix (dot optional, e.g. "G40.9") or part of the description. */
    @GetMapping
    public PageResponse<Icd10Response> search(
            @RequestParam(required = false) @Size(max = 100, message = "must be at most 100 characters") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return icd10Service.search(q, page, size);
    }

    @GetMapping("/{code}")
    public Icd10Response get(@PathVariable String code) {
        return icd10Service.get(code);
    }
}
