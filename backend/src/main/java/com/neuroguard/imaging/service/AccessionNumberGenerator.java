package com.neuroguard.imaging.service;

import com.neuroguard.imaging.repository.MriOrderRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Accession numbers are ACC-YYYY-NNNNNN: YYYY is the creation year in UTC, NNNNNN comes from
 * accession_number_seq (never resets, zero-padded to 6 digits, grows past 999999). Generated numbers are
 * 15 characters, within the 16-character limit of the DICOM AccessionNumber (0008,0050) tag.
 */
@Component
public class AccessionNumberGenerator {

    private final MriOrderRepository orders;
    private final Clock clock;

    public AccessionNumberGenerator(MriOrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    /** Call inside the transaction that saves the order. */
    public String next() {
        int year = LocalDate.now(clock.withZone(ZoneOffset.UTC)).getYear();
        return format(year, orders.nextAccessionNumberSeq());
    }

    static String format(int year, long seq) {
        return String.format("ACC-%d-%06d", year, seq);
    }
}
