package com.neuroguard.billing;

import com.neuroguard.billing.entity.InvoiceItem;
import com.neuroguard.billing.repository.InvoiceItemRepository;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** V5: invoice_item.unit_price (NOT NULL, >= 0, mapped) and a unique service name. */
class BillingMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @AfterEach
    void cleanServices() {
        // service is not covered by the fixture TRUNCATE; CASCADE also removes invoice items
        jdbc.execute("TRUNCATE TABLE service CASCADE");
    }

    private UUID service(String name, String price) {
        return jdbc.queryForObject("INSERT INTO service (name, price) VALUES (?, ?::numeric) RETURNING service_id",
                UUID.class, name, price);
    }

    private UUID invoice() {
        return jdbc.queryForObject("INSERT INTO invoice (patient_id) VALUES (?) RETURNING invoice_id",
                UUID.class, patientA.getId());
    }

    @Test
    void unitPriceIsRequiredNumeric10x2() {
        String type = jdbc.queryForObject("""
                SELECT data_type || '(' || numeric_precision || ',' || numeric_scale || '):' || is_nullable
                FROM information_schema.columns WHERE table_name = 'invoice_item' AND column_name = 'unit_price'
                """, String.class);
        assertEquals("numeric(10,2):NO", type);

        UUID serviceId = service("TEST consult", "100.00");
        UUID invoiceId = invoice();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO invoice_item (invoice_id, service_id, quantity, line_total) VALUES (?, ?, 1, 100.00)",
                invoiceId, serviceId));
    }

    @Test
    void negativeUnitPriceIsRejected() {
        UUID serviceId = service("TEST consult", "100.00");
        UUID invoiceId = invoice();
        String insert = "INSERT INTO invoice_item (invoice_id, service_id, quantity, unit_price, line_total) "
                + "VALUES (?, ?, 1, ?::numeric, 0)";
        jdbc.update(insert, invoiceId, serviceId, "0.00");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert, invoiceId, serviceId, "-0.01"));
    }

    @Test
    void entityMapsUnitPrice() {
        UUID serviceId = service("TEST MRI", "2500.50");
        UUID invoiceId = invoice();
        UUID itemId = jdbc.queryForObject("""
                INSERT INTO invoice_item (invoice_id, service_id, quantity, unit_price, line_total)
                VALUES (?, ?, 2, 2500.50, 5001.00) RETURNING item_id
                """, UUID.class, invoiceId, serviceId);

        InvoiceItem item = invoiceItemRepository.findById(itemId).orElseThrow();
        assertEquals(new BigDecimal("2500.50"), item.getUnitPrice());
        assertEquals(new BigDecimal("5001.00"), item.getLineTotal());
    }

    @Test
    void duplicateServiceNameIsRejected() {
        service("TEST Brain MRI", "2500.00");
        assertThrows(DataIntegrityViolationException.class, () -> service("TEST Brain MRI", "3000.00"));
    }
}
