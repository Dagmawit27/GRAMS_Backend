-- =============================================================================
-- V5__create_reporting_views.sql
-- Reporting & Analytics module — Developer C
-- SRS §5.9 (Reporting), §10.9 (Audit/Compliance)
--
-- These are READ-ONLY PostgreSQL views used by reporting dashboards.
-- No new base tables are created — all data comes from existing tables.
-- =============================================================================

-- -----------------------------------------------------------------------
-- v_complaint_summary
-- Aggregates complaint counts + average resolution time by status,
-- category, and priority for the admin reports dashboard.
-- -----------------------------------------------------------------------
CREATE OR REPLACE VIEW v_complaint_summary AS
SELECT
    c.category,
    c.priority,
    c.status,
    COUNT(*)                                                        AS total,
    AVG(
        EXTRACT(EPOCH FROM (cr.resolved_at - c.created_at)) / 86400.0
    )                                                               AS avg_resolution_days
FROM complaints c
LEFT JOIN complaint_resolutions cr ON cr.complaint_id = c.id
GROUP BY c.category, c.priority, c.status;

-- -----------------------------------------------------------------------
-- v_property_status_summary
-- Aggregates property counts by status and type.
-- -----------------------------------------------------------------------
CREATE OR REPLACE VIEW v_property_status_summary AS
SELECT
    p.status,
    p.property_type,
    COUNT(*) AS total
FROM properties p
GROUP BY p.status, p.property_type;
