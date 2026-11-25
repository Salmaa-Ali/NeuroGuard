-- NeuroGuard V4: clinical additions.

-- Free-text clinical notes on a diagnosis (optional, so existing rows stay valid).
ALTER TABLE diagnosis ADD COLUMN notes TEXT NULL;

-- Who prescribed. Existing prescriptions are attributed to the neurologist who owns their treatment
-- plan (the only clinician the data can name); new ones store the prescribing neurologist.
ALTER TABLE prescription ADD COLUMN staff_id UUID REFERENCES staff (staff_id);
UPDATE prescription p
SET staff_id = tp.staff_id
FROM treatment_plan tp
WHERE tp.plan_id = p.plan_id;
ALTER TABLE prescription ALTER COLUMN staff_id SET NOT NULL;
CREATE INDEX idx_prescription_staff_id ON prescription (staff_id);
