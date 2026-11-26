package com.neuroguard.demo;

import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.billing.repository.InvoiceItemRepository;
import com.neuroguard.billing.repository.InvoiceRepository;
import com.neuroguard.billing.repository.PaymentRepository;
import com.neuroguard.billing.repository.ServiceItemRepository;
import com.neuroguard.imaging.repository.MriOrderRepository;
import com.neuroguard.imaging.service.AccessionNumberGenerator;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The seeder is dev-only and opt-in, so it is built by hand here. */
class DemoDataSeederTest extends AbstractIntegrationTest {

    @Autowired AppointmentRepository appointments;
    @Autowired MriOrderRepository orders;
    @Autowired AccessionNumberGenerator accessionNumbers;
    @Autowired ServiceItemRepository services;
    @Autowired InvoiceRepository invoices;
    @Autowired InvoiceItemRepository invoiceItems;
    @Autowired PaymentRepository payments;
    @Autowired Clock clock;
    @Autowired TransactionTemplate tx;

    private void runSeeder() {
        DemoDataSeeder seeder = new DemoDataSeeder(patientRepository, staffRepository, appointments, orders,
                accessionNumbers, services, invoices, invoiceItems, payments, clock);
        tx.executeWithoutResult(s -> seeder.run(new DefaultApplicationArguments()));
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void addsHistoryToEveryPatientOnce() {
        runSeeder();
        runSeeder();   // second run must add nothing

        assertEquals(8, count("SELECT count(*) FROM appointment"));               // 4 per patient
        assertEquals(2, count("SELECT count(*) FROM appointment WHERE status = 'CANCELLED'"));
        assertEquals(2, count("SELECT count(*) FROM appointment WHERE status = 'SCHEDULED' AND scheduled_time > now()"));
        assertEquals(4, count("SELECT count(*) FROM mri_order WHERE accession_number ~ '^ACC-[0-9]{4}-[0-9]{6,}$'"));
        assertEquals(0, count("""
                SELECT count(*) FROM mri_order o JOIN appointment a USING (appointment_id)
                WHERE a.status = 'CANCELLED' OR a.patient_id <> o.patient_id
                """));
        assertEquals(2, count("SELECT count(*) FROM invoice WHERE status = 'PAID'"));
        assertEquals(2, count("SELECT count(*) FROM invoice WHERE status = 'UNPAID'"));
        assertEquals(2, count("SELECT count(*) FROM payment p JOIN invoice i USING (invoice_id) "
                + "WHERE p.status = 'COMPLETED' AND p.amount_paid = i.total_amount"));
        assertEquals(0, count("""
                SELECT count(*) FROM invoice i WHERE i.total_amount <>
                  (SELECT sum(line_total) FROM invoice_item ii WHERE ii.invoice_id = i.invoice_id)
                """));
    }
}
