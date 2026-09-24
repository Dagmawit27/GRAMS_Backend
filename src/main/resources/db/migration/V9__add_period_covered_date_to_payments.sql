-- Add period_covered_date column to payments table
-- This stores the actual month the payment is for (due date), not when it was paid
-- This is important for accurate tax calculation based on rental periods

ALTER TABLE payments ADD COLUMN period_covered_date TIMESTAMP;

-- For existing payments, set period_covered_date to payment_date as a fallback
-- New payments will have this set correctly based on the due date
UPDATE payments SET period_covered_date = payment_date WHERE period_covered_date IS NULL AND payment_date IS NOT NULL;
