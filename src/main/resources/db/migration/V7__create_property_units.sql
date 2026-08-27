-- Create property_units table for managing individual units within properties (e.g., shopping mall units)
CREATE TABLE property_units (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    unit_code VARCHAR(50) NOT NULL UNIQUE,
    unit_name VARCHAR(100) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    area_sq_meter DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    rent_amount DECIMAL(12, 2),
    tenant_name VARCHAR(100),
    floor_level VARCHAR(50),
    category VARCHAR(50),
    shop_number VARCHAR(50),
    submeter BOOLEAN DEFAULT FALSE,
    water_supply BOOLEAN DEFAULT FALSE,
    frontage VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_property_units_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- Create index on property_id for faster queries
CREATE INDEX idx_property_units_property_id ON property_units(property_id);

-- Create index on status for filtering available units
CREATE INDEX idx_property_units_status ON property_units(status);
