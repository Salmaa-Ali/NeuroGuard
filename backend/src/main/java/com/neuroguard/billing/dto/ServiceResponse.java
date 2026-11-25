package com.neuroguard.billing.dto;

import com.neuroguard.billing.entity.ServiceItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** {@code price} is always serialised with 2 decimals (e.g. 1500.00). */
public record ServiceResponse(UUID id, String name, BigDecimal price, Instant createdAt, Instant updatedAt) {

    public static ServiceResponse from(ServiceItem s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getPrice(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
