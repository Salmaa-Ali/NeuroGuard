-- NeuroGuard V1: Phase 2 schema.
-- All PKs are UUIDs; enum-like columns are varchar guarded by CHECK constraints
-- (mirrors @Enumerated(EnumType.STRING) in the entities).

-- ---------------------------------------------------------------- patient
CREATE TABLE patient (
    patient_id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_number    VARCHAR(50)  NOT NULL,
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    date_of_birth     DATE         NOT NULL,
    phone             VARCHAR(50),
    neurology_history TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_patient_patient_number UNIQUE (patient_number),
    CONSTRAINT uq_patient_email UNIQUE (email)
);

-- ------------------------------------------------------------------ staff
CREATE TABLE staff (
    staff_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    base_role        VARCHAR(30)  NOT NULL,
    permission_scope VARCHAR(30),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_email UNIQUE (email),
    CONSTRAINT ck_staff_base_role CHECK (base_role IN ('ADMIN', 'CLINICAL_STAFF')),
    CONSTRAINT ck_staff_permission_scope CHECK (permission_scope IS NULL OR permission_scope IN
        ('NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST', 'BIOMEDICAL_ENGINEER', 'BILLING_CLERK')),
    -- clinical staff must have a scope; admins may omit it
    CONSTRAINT ck_staff_scope_required CHECK (base_role = 'ADMIN' OR permission_scope IS NOT NULL)
);

-- ------------------------------------------------------------ appointment
CREATE TABLE appointment (
    appointment_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       UUID        NOT NULL REFERENCES patient (patient_id),
    staff_id         UUID        NOT NULL REFERENCES staff (staff_id),
    scheduled_time   TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER     NOT NULL DEFAULT 30,
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    reason           TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_appointment_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_appointment_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'MISSED'))
);
CREATE INDEX idx_appointment_patient_id ON appointment (patient_id);
CREATE INDEX idx_appointment_staff_scheduled ON appointment (staff_id, scheduled_time);

-- -------------------------------------------------------------- diagnosis
CREATE TABLE diagnosis (
    diagnosis_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id    UUID        NOT NULL REFERENCES patient (patient_id),
    staff_id      UUID        NOT NULL REFERENCES staff (staff_id),
    icd10_code    VARCHAR(10) NOT NULL,
    diagnosed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_diagnosis_patient_id ON diagnosis (patient_id);
CREATE INDEX idx_diagnosis_staff_id ON diagnosis (staff_id);

-- ---------------------------------------------------------- treatment_plan
CREATE TABLE treatment_plan (
    plan_id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id  UUID        NOT NULL REFERENCES patient (patient_id),
    staff_id    UUID        NOT NULL REFERENCES staff (staff_id),
    description TEXT        NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_treatment_plan_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_treatment_plan_patient_id ON treatment_plan (patient_id);
CREATE INDEX idx_treatment_plan_staff_id ON treatment_plan (staff_id);

-- ------------------------------------------------------------ prescription
CREATE TABLE prescription (
    prescription_id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id            UUID        NOT NULL REFERENCES treatment_plan (plan_id),
    medication_details TEXT        NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_prescription_plan_id ON prescription (plan_id);

-- ---------------------------------------------------------------- service
CREATE TABLE service (
    service_id UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255)  NOT NULL,
    price      NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_service_price CHECK (price >= 0)
);

-- ---------------------------------------------------------------- invoice
CREATE TABLE invoice (
    invoice_id     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id     UUID          NOT NULL REFERENCES patient (patient_id),
    appointment_id UUID          REFERENCES appointment (appointment_id),
    total_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    issue_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoice_total CHECK (total_amount >= 0),
    CONSTRAINT ck_invoice_status CHECK (status IN ('UNPAID', 'PAID', 'CANCELLED'))
);
CREATE INDEX idx_invoice_patient_id ON invoice (patient_id);
CREATE INDEX idx_invoice_appointment_id ON invoice (appointment_id);

-- ------------------------------------------------------------ invoice_item
CREATE TABLE invoice_item (
    item_id     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id  UUID          NOT NULL REFERENCES invoice (invoice_id),
    service_id  UUID          NOT NULL REFERENCES service (service_id),
    icd10_code  VARCHAR(10),
    quantity    INTEGER       NOT NULL DEFAULT 1,
    line_total  NUMERIC(10,2) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoice_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_item_line_total CHECK (line_total >= 0)
);
CREATE INDEX idx_invoice_item_invoice_id ON invoice_item (invoice_id);
CREATE INDEX idx_invoice_item_service_id ON invoice_item (service_id);

-- ---------------------------------------------------------------- payment
CREATE TABLE payment (
    payment_id   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id   UUID          NOT NULL REFERENCES invoice (invoice_id),
    amount_paid  NUMERIC(10,2) NOT NULL,
    payment_date TIMESTAMPTZ   NOT NULL DEFAULT now(),
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_payment_amount CHECK (amount_paid > 0),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);
CREATE INDEX idx_payment_invoice_id ON payment (invoice_id);

-- --------------------------------------------------------------- mri_order
CREATE TABLE mri_order (
    order_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id   UUID         NOT NULL REFERENCES appointment (appointment_id),
    patient_id       UUID         NOT NULL REFERENCES patient (patient_id),
    accession_number VARCHAR(64)  NOT NULL,
    indication       TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ORDERED',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_mri_order_accession_number UNIQUE (accession_number),
    CONSTRAINT ck_mri_order_status CHECK (status IN ('ORDERED', 'SCHEDULED', 'ACQUIRED', 'REPORTED', 'CANCELLED'))
);
CREATE INDEX idx_mri_order_appointment_id ON mri_order (appointment_id);
CREATE INDEX idx_mri_order_patient_id ON mri_order (patient_id);

-- --------------------------------------------------------------- audit_log
-- actor_id is polymorphic (staff or patient depending on actor_type), so it has no FK.
-- Append-only: created_at only, no updated_at.
CREATE TABLE audit_log (
    audit_id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         NOT NULL,
    actor_type  VARCHAR(20)  NOT NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_log_actor_type CHECK (actor_type IN ('STAFF', 'PATIENT'))
);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_type, actor_id);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);

-- ---------------------------------------------------------- scanner_device
CREATE TABLE scanner_device (
    scanner_id     UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    manufacturer   VARCHAR(255)     NOT NULL,
    model          VARCHAR(255)     NOT NULL,
    serial_number  VARCHAR(100)     NOT NULL,
    station_name   VARCHAR(255),
    field_strength DOUBLE PRECISION NOT NULL,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT uq_scanner_device_serial_number UNIQUE (serial_number),
    CONSTRAINT ck_scanner_device_field_strength CHECK (field_strength > 0)
);

-- ------------------------------------------------------ calibration_session
CREATE TABLE calibration_session (
    session_id    UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    scanner_id    UUID             NOT NULL REFERENCES scanner_device (scanner_id),
    performed_at  TIMESTAMPTZ      NOT NULL,
    result        VARCHAR(100),
    next_due_date DATE,
    risk_score    DOUBLE PRECISION,
    created_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE INDEX idx_calibration_session_scanner_id ON calibration_session (scanner_id);
