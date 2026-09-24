-- Remove securityDeposit column from agreements table
-- Replaced by advancePaymentMonths which is set from Property.advanceRent

ALTER TABLE agreements DROP COLUMN IF EXISTS security_deposit;
