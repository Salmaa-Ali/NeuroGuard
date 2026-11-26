-- NeuroGuard V8: server-generated MRI order accession numbers (ACC-YYYY-NNNNNN).
-- One sequence that never resets; the year in the number is the order's creation year (UTC).
-- Starts at 1000 so generated numbers are easy to tell apart from hand-typed test values.
-- Existing orders keep their accession numbers; uq_mri_order_accession_number stays in place.
CREATE SEQUENCE IF NOT EXISTS accession_number_seq START 1000;
