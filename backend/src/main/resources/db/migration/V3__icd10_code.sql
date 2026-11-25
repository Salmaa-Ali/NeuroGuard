-- NeuroGuard V3: ICD-10 reference data.
-- Codes are stored the CMS ICD-10-CM way: upper case, no dot (e.g. G40.909 -> G40909).
-- category = first 3 characters, used for grouping (dashboard, search).
-- The full CMS list can be loaded later with the "icd10-import" profile; it upserts over these rows.

CREATE TABLE icd10_code (
    code        VARCHAR(10) PRIMARY KEY,
    description TEXT        NOT NULL,
    category    VARCHAR(3)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_icd10_code_category ON icd10_code (category);
-- Lets "code LIKE 'G40%'" use an index regardless of the database collation.
CREATE INDEX idx_icd10_code_code_pattern ON icd10_code (code varchar_pattern_ops);

-- Neurology starter set (ICD-10-CM category titles).
INSERT INTO icd10_code (code, description, category) VALUES
    ('G40', 'Epilepsy and recurrent seizures', 'G40'),
    ('G35', 'Multiple sclerosis', 'G35'),
    ('G43', 'Migraine', 'G43'),
    ('I63', 'Cerebral infarction', 'I63'),
    ('C71', 'Malignant neoplasm of brain', 'C71'),
    ('D32', 'Benign neoplasm of meninges', 'D32'),
    ('G20', 'Parkinson''s disease', 'G20'),
    ('G30', 'Alzheimer''s disease', 'G30');

-- Safety net for databases that already hold data (the shared one): any code already referenced by
-- diagnosis or invoice_item gets a placeholder row, so the foreign keys below can never fail.
-- The importer replaces the placeholder description if the code is a real ICD-10-CM code.
INSERT INTO icd10_code (code, description, category)
SELECT used.code, 'Unverified (pre-existing)', upper(left(replace(used.code, '.', ''), 3))
FROM (
    SELECT icd10_code AS code FROM diagnosis
    UNION
    SELECT icd10_code FROM invoice_item WHERE icd10_code IS NOT NULL
) used
ON CONFLICT (code) DO NOTHING;

ALTER TABLE diagnosis
    ADD CONSTRAINT fk_diagnosis_icd10_code FOREIGN KEY (icd10_code) REFERENCES icd10_code (code);
ALTER TABLE invoice_item
    ADD CONSTRAINT fk_invoice_item_icd10_code FOREIGN KEY (icd10_code) REFERENCES icd10_code (code);

CREATE INDEX idx_diagnosis_icd10_code ON diagnosis (icd10_code);
CREATE INDEX idx_invoice_item_icd10_code ON invoice_item (icd10_code);
