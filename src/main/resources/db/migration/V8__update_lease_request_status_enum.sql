-- Update lease request status from APPROVED to LANDLORD_APPROVED
-- This migration updates the enum values to match the new status naming convention
UPDATE lease_requests SET status = 'LANDLORD_APPROVED' WHERE status = 'APPROVED';
