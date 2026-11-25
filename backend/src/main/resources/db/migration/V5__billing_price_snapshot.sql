-- NeuroGuard V5: billing.

-- Price snapshot: each invoice line keeps the unit price it was billed at, so later changes to
-- service.price never alter existing invoices.
ALTER TABLE invoice_item ADD COLUMN unit_price NUMERIC(10,2) NULL;

-- Backfill existing lines with what was actually charged (line_total / quantity). service.price is only a
-- fallback: it is today's price, which may differ from the price at invoicing time.
UPDATE invoice_item ii
SET unit_price = COALESCE(ROUND(ii.line_total / NULLIF(ii.quantity, 0), 2), s.price)
FROM service s
WHERE s.service_id = ii.service_id;

ALTER TABLE invoice_item ALTER COLUMN unit_price SET NOT NULL;
ALTER TABLE invoice_item ADD CONSTRAINT ck_invoice_item_unit_price CHECK (unit_price >= 0);

-- Service names identify catalogue entries for clerks; duplicates would make invoices ambiguous.
ALTER TABLE service ADD CONSTRAINT uq_service_name UNIQUE (name);
