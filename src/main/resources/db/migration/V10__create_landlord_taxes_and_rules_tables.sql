-- V10: Create landlord_taxes and tax_rules tables for Schedule B Ethiopian rental income tax

CREATE TABLE IF NOT EXISTS landlord_taxes (
    id UUID PRIMARY KEY,
    landlord_id UUID NOT NULL,
    landlord_email VARCHAR(255) NOT NULL,
    fiscal_year VARCHAR(50) NOT NULL,
    total_agreements INTEGER NOT NULL DEFAULT 0,
    total_contracted_monthly_rent NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    projected_annual_gross_income NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_gross_income NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_tax_accrued NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_tax_paid NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_months_paid INTEGER NOT NULL DEFAULT 0,
    tax_status VARCHAR(30) NOT NULL DEFAULT 'ACCRUING',
    settlement_date TIMESTAMP,
    clearance_certificate_number VARCHAR(100),
    last_payment_date TIMESTAMP,
    tax_bracket_percentage INTEGER,
    effective_tax_rate NUMERIC(5,2),
    net_income_after_tax NUMERIC(15,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_landlord_taxes_citizen FOREIGN KEY (landlord_id) REFERENCES citizens(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_landlord_taxes_email_fy ON landlord_taxes(landlord_email, fiscal_year);

CREATE TABLE IF NOT EXISTS tax_rules (
    id UUID PRIMARY KEY,
    proclamation_number VARCHAR(255) NOT NULL DEFAULT '1395/2017',
    proclamation_year INTEGER NOT NULL DEFAULT 2017,
    deduction_percentage NUMERIC(5,2) NOT NULL DEFAULT 50.00,
    tax_free_threshold NUMERIC(12,2) NOT NULL DEFAULT 24000.00,
    bracket_1_rate NUMERIC(5,2) NOT NULL DEFAULT 15.00,
    bracket_1_min NUMERIC(12,2) NOT NULL DEFAULT 24001.00,
    bracket_1_max NUMERIC(12,2) NOT NULL DEFAULT 48000.00,
    bracket_2_rate NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    bracket_2_min NUMERIC(12,2) NOT NULL DEFAULT 48001.00,
    bracket_2_max NUMERIC(12,2) NOT NULL DEFAULT 84000.00,
    bracket_3_rate NUMERIC(5,2) NOT NULL DEFAULT 25.00,
    bracket_3_min NUMERIC(12,2) NOT NULL DEFAULT 84001.00,
    bracket_3_max NUMERIC(12,2) NOT NULL DEFAULT 120000.00,
    bracket_4_rate NUMERIC(5,2) NOT NULL DEFAULT 30.00,
    bracket_4_min NUMERIC(12,2) NOT NULL DEFAULT 120001.00,
    bracket_4_max NUMERIC(12,2) NOT NULL DEFAULT 168000.00,
    bracket_5_rate NUMERIC(5,2) NOT NULL DEFAULT 35.00,
    bracket_5_min NUMERIC(12,2) NOT NULL DEFAULT 168001.00,
    corporate_tax_rate NUMERIC(5,2) NOT NULL DEFAULT 30.00,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    created_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP
);
