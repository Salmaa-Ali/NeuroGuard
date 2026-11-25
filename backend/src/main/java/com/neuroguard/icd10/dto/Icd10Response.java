package com.neuroguard.icd10.dto;

import com.neuroguard.icd10.entity.Icd10Code;

public record Icd10Response(String code, String description, String category) {

    public static Icd10Response from(Icd10Code c) {
        return new Icd10Response(c.getCode(), c.getDescription(), c.getCategory());
    }
}
