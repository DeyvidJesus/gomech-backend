-- Create organizations table
CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(50) UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    address VARCHAR(200),
    document VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create default organization for existing data
INSERT INTO organizations (name, slug, description, active) 
VALUES ('Default Organization', 'default', 'Default organization for existing data', true);

-- Add organization_id to users table
ALTER TABLE users ADD COLUMN organization_id INT;
ALTER TABLE users ADD CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE users SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_users_organization ON users(organization_id);

-- Add organization_id to clients table
ALTER TABLE clients ADD COLUMN organization_id INT;
ALTER TABLE clients ADD CONSTRAINT fk_clients_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE clients SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE clients ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_clients_organization ON clients(organization_id);

-- Add organization_id to vehicles table
ALTER TABLE vehicles ADD COLUMN organization_id INT;
ALTER TABLE vehicles ADD CONSTRAINT fk_vehicles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE vehicles SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE vehicles ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_vehicles_organization ON vehicles(organization_id);

-- Add organization_id to service_orders table
ALTER TABLE service_orders ADD COLUMN organization_id INT;
ALTER TABLE service_orders ADD CONSTRAINT fk_service_orders_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE service_orders SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE service_orders ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX idx_service_orders_organization ON service_orders(organization_id);

-- Add organization_id to parts table if exists
CREATE TABLE IF NOT EXISTS parts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    manufacturer VARCHAR(150),
    description TEXT,
    unit_cost NUMERIC(10,2),
    unit_price NUMERIC(10,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE parts ADD COLUMN IF NOT EXISTS organization_id INT;
ALTER TABLE parts DROP CONSTRAINT IF EXISTS fk_parts_organization;
ALTER TABLE parts ADD CONSTRAINT fk_parts_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE parts SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE parts ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_parts_organization ON parts(organization_id);

-- Add organization_id to inventory_items table if exists
CREATE TABLE IF NOT EXISTS inventory_items (
    id SERIAL PRIMARY KEY,
    part_id INT NOT NULL,
    location VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    minimum_quantity INT NOT NULL DEFAULT 0,
    unit_cost NUMERIC(10,2),
    sale_price NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_items_part FOREIGN KEY (part_id) REFERENCES parts(id)
);

ALTER TABLE inventory_items ADD COLUMN IF NOT EXISTS organization_id INT;
ALTER TABLE inventory_items DROP CONSTRAINT IF EXISTS fk_inventory_items_organization;
ALTER TABLE inventory_items ADD CONSTRAINT fk_inventory_items_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE inventory_items SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE inventory_items ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_inventory_items_organization ON inventory_items(organization_id);

-- Add organization_id to inventory_movements table if exists
CREATE TABLE IF NOT EXISTS inventory_movements (
    id SERIAL PRIMARY KEY,
    inventory_item_id INT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_cost NUMERIC(10,2),
    reference_document VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    service_order_id INT,
    vehicle_id INT,
    CONSTRAINT fk_inventory_movements_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_inventory_movements_so FOREIGN KEY (service_order_id) REFERENCES service_orders(id),
    CONSTRAINT fk_inventory_movements_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);

ALTER TABLE inventory_movements ADD COLUMN IF NOT EXISTS organization_id INT;
ALTER TABLE inventory_movements DROP CONSTRAINT IF EXISTS fk_inventory_movements_organization;
ALTER TABLE inventory_movements ADD CONSTRAINT fk_inventory_movements_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE inventory_movements SET organization_id = 1 WHERE organization_id IS NULL;
ALTER TABLE inventory_movements ALTER COLUMN organization_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_inventory_movements_organization ON inventory_movements(organization_id);

-- Add organization_id to audit_events table if exists
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS organization_id INT;
ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS fk_audit_events_organization;
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
UPDATE audit_events SET organization_id = 1 WHERE organization_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_audit_events_organization ON audit_events(organization_id);

-- Update unique constraints to include organization_id where needed
-- For parts: SKU should be unique per organization
ALTER TABLE parts DROP CONSTRAINT IF EXISTS parts_sku_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_parts_sku_org ON parts(sku, organization_id);

-- For service_orders: order_number should be unique per organization
ALTER TABLE service_orders DROP CONSTRAINT IF EXISTS service_orders_order_number_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_service_orders_number_org ON service_orders(order_number, organization_id);

