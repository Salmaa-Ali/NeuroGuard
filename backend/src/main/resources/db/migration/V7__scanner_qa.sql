-- Scanner QA / calibration module.
-- Assumes scanner_device and calibration_session are still empty (no endpoint wrote to them before V7):
-- the NOT NULL columns below have no defaults on purpose. If rows exist, this migration fails and rolls back.

-- ------------------------------------------------------------ scanner_device
ALTER TABLE scanner_device
    ADD COLUMN name                      VARCHAR(255) NOT NULL,
    ADD COLUMN status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN location                  VARCHAR(255),
    ADD COLUMN install_date              DATE,
    ADD COLUMN calibration_interval_days INTEGER      NOT NULL,
    ADD CONSTRAINT ck_scanner_device_status CHECK (status IN ('ACTIVE', 'OUT_OF_SERVICE', 'RETIRED')),
    ADD CONSTRAINT ck_scanner_device_interval CHECK (calibration_interval_days > 0);

-- --------------------------------------------------------- scanner_accessory
CREATE TABLE scanner_accessory (
    accessory_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    scanner_id    UUID         NOT NULL REFERENCES scanner_device (scanner_id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(100),
    serial_number VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_scanner_accessory_scanner_id ON scanner_accessory (scanner_id);

-- ------------------------------------------------------------------ qa_test
-- Tolerances are data entered by the biomedical engineer. NULL limit = no bound on that side.
CREATE TABLE qa_test (
    qa_test_id     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(50)    NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    unit           VARCHAR(30),
    lower_limit    NUMERIC(12, 4),
    upper_limit    NUMERIC(12, 4),
    reference      TEXT,
    is_placeholder BOOLEAN        NOT NULL DEFAULT FALSE,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_qa_test_code UNIQUE (code),
    CONSTRAINT ck_qa_test_limits CHECK (lower_limit IS NULL OR upper_limit IS NULL OR lower_limit <= upper_limit)
);

-- ------------------------------------------------------- calibration_session
ALTER TABLE calibration_session
    ADD COLUMN performed_by    UUID NOT NULL REFERENCES staff (staff_id),
    ADD COLUMN notes           TEXT,
    ADD COLUMN certificate_ref VARCHAR(100),
    ALTER COLUMN result TYPE VARCHAR(10),
    ALTER COLUMN result SET NOT NULL,
    ALTER COLUMN next_due_date SET NOT NULL,
    ALTER COLUMN risk_score SET NOT NULL,
    ADD CONSTRAINT ck_calibration_session_result CHECK (result IN ('PASS', 'FAIL')),
    ADD CONSTRAINT ck_calibration_session_risk CHECK (risk_score >= 0 AND risk_score <= 100);
CREATE INDEX idx_calibration_session_performed_by ON calibration_session (performed_by);
CREATE INDEX idx_calibration_session_scanner_performed ON calibration_session (scanner_id, performed_at DESC);

-- --------------------------------------------------- calibration_measurement
-- lower/upper_limit are copied from qa_test when recorded, so later tolerance edits never rewrite history.
CREATE TABLE calibration_measurement (
    measurement_id   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id       UUID           NOT NULL REFERENCES calibration_session (session_id) ON DELETE CASCADE,
    qa_test_id       UUID           NOT NULL REFERENCES qa_test (qa_test_id),
    measured_value   NUMERIC(12, 4) NOT NULL,
    lower_limit      NUMERIC(12, 4),
    upper_limit      NUMERIC(12, 4),
    within_tolerance BOOLEAN        NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_calibration_measurement_session_test UNIQUE (session_id, qa_test_id)
);
CREATE INDEX idx_calibration_measurement_qa_test_id ON calibration_measurement (qa_test_id);
