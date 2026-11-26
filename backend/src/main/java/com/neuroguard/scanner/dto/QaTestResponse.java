package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.QaTest;

import java.math.BigDecimal;
import java.util.UUID;

public record QaTestResponse(
        UUID id,
        String code,
        String name,
        String unit,
        BigDecimal lowerLimit,
        BigDecimal upperLimit,
        String reference,
        boolean placeholder,
        boolean active) {

    public static QaTestResponse from(QaTest t) {
        return new QaTestResponse(t.getId(), t.getCode(), t.getName(), t.getUnit(), t.getLowerLimit(),
                t.getUpperLimit(), t.getReference(), t.isPlaceholder(), t.isActive());
    }
}
