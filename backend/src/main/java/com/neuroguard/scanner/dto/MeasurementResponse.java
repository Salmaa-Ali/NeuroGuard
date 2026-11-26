package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.CalibrationMeasurement;

import java.math.BigDecimal;
import java.util.UUID;

/** lowerLimit/upperLimit are the limits in force when the value was recorded. */
public record MeasurementResponse(
        UUID id,
        UUID qaTestId,
        String qaTestCode,
        String qaTestName,
        String unit,
        BigDecimal measuredValue,
        BigDecimal lowerLimit,
        BigDecimal upperLimit,
        boolean withinTolerance) {

    public static MeasurementResponse from(CalibrationMeasurement m) {
        return new MeasurementResponse(m.getId(), m.getQaTest().getId(), m.getQaTest().getCode(),
                m.getQaTest().getName(), m.getQaTest().getUnit(), m.getMeasuredValue(), m.getLowerLimit(),
                m.getUpperLimit(), m.isWithinTolerance());
    }
}
