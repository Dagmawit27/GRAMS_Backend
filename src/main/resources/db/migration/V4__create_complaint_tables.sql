-- =============================================================================
-- V4__create_complaint_tables.sql
-- Complaint Management module — Developer C
-- SRS §5.8 (Complaint Management), §6.6 (MinIO scope), §10.8 (metadata storage)
--
-- NOTE ON STORAGE (§10.8): complaint_attachments stores metadata + an opaque
-- storage_reference string. It deliberately never stores a raw filesystem path.
-- The reference format is defined by the active ComplaintAttachmentStorage
-- implementation (currently LocalFileComplaintAttachmentStorage — dev profile).
-- When Developer A's DocumentStorageService / MinIO adapter is shipped, only
-- the storage bean changes; this schema does NOT need to be altered.
-- =============================================================================

-- -----------------------------------------------------------------------
-- complaints
-- -----------------------------------------------------------------------
CREATE TABLE complaints (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    complainant_id  UUID        NOT NULL
                                REFERENCES citizens(id) ON DELETE RESTRICT,
    category        VARCHAR(50)  NOT NULL,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    subject         VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED',
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_complaints_complainant ON complaints(complainant_id);
CREATE INDEX idx_complaints_status      ON complaints(status);
CREATE INDEX idx_complaints_created_at  ON complaints(created_at DESC);

-- -----------------------------------------------------------------------
-- complaint_attachments  (§10.8 — metadata + hash + opaque reference only)
-- -----------------------------------------------------------------------
CREATE TABLE complaint_attachments (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id        UUID         NOT NULL
                                     REFERENCES complaints(id) ON DELETE CASCADE,
    original_filename   VARCHAR(255) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    size_bytes          BIGINT       NOT NULL,
    -- SHA-256 (or MD5 in dev) hex digest — required by SRS §10.8
    content_hash        VARCHAR(64)  NOT NULL,
    -- Opaque reference whose format is defined by the storage implementation.
    -- Never a raw OS path.  storageVersion lets future migrations know which
    -- format to expect when re-interpreting existing rows.
    storage_reference   TEXT         NOT NULL,
    storage_version     INTEGER      NOT NULL DEFAULT 1,
    uploaded_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_complaint_attachments_complaint ON complaint_attachments(complaint_id);

-- -----------------------------------------------------------------------
-- complaint_assignments
-- -----------------------------------------------------------------------
CREATE TABLE complaint_assignments (
    id                  UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id        UUID      NOT NULL
                                  REFERENCES complaints(id) ON DELETE CASCADE,
    assigned_officer_id UUID      NOT NULL
                                  REFERENCES government_employees(id) ON DELETE RESTRICT,
    assigned_by_id      UUID      NOT NULL
                                  REFERENCES government_employees(id) ON DELETE RESTRICT,
    notes               TEXT,
    assigned_at         TIMESTAMP NOT NULL DEFAULT now(),
    -- NULL means this assignment is currently active; set when superseded
    revoked_at          TIMESTAMP
);

CREATE INDEX idx_complaint_assignments_complaint ON complaint_assignments(complaint_id);
CREATE INDEX idx_complaint_assignments_officer   ON complaint_assignments(assigned_officer_id)
    WHERE revoked_at IS NULL;

-- -----------------------------------------------------------------------
-- complaint_resolutions
-- -----------------------------------------------------------------------
CREATE TABLE complaint_resolutions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id        UUID         NOT NULL UNIQUE
                                     REFERENCES complaints(id) ON DELETE CASCADE,
    resolved_by_id      UUID         NOT NULL
                                     REFERENCES government_employees(id) ON DELETE RESTRICT,
    resolution_summary  TEXT         NOT NULL,
    outcome             VARCHAR(100) NOT NULL,
    resolved_at         TIMESTAMP    NOT NULL DEFAULT now()
);
