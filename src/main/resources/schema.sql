-- ============================================
-- GOMECH DATABASE SCHEMA
-- Complete schema with Organizations (Multi-tenancy)
-- ============================================

-- Drop existing tables (in reverse order of dependencies)
DROP TABLE IF EXISTS inventory_movements CASCADE;
DROP TABLE IF EXISTS inventory_items CASCADE;
DROP TABLE IF EXISTS parts CASCADE;
DROP TABLE IF EXISTS service_order_items CASCADE;
DROP TABLE IF EXISTS service_orders CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS audit_events CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
DROP TABLE IF EXISTS conversations CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- ============================================
-- ORGANIZATIONS TABLE (Multi-tenancy)
-- ============================================
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

-- ============================================
-- USERS TABLE
-- ============================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret VARCHAR(512),
    organization_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_organization ON users(organization_id);

-- ============================================
-- REFRESH TOKENS TABLE
-- ============================================
CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- ============================================
-- CLIENTS TABLE
-- ============================================
CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    name VARCHAR(255),
    document VARCHAR(100),
    phone VARCHAR(100),
    email VARCHAR(255),
    address VARCHAR(255),
    birth_date DATE,
    observations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clients_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_clients_organization ON clients(organization_id);
CREATE INDEX idx_clients_document ON clients(document);
CREATE INDEX idx_clients_email ON clients(email);

-- ============================================
-- VEHICLES TABLE
-- ============================================
CREATE TABLE vehicles (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    client_id INT NOT NULL,
    license_plate VARCHAR(20) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    manufacture_date DATE NOT NULL,
    color VARCHAR(50),
    observations TEXT,
    kilometers INT NOT NULL,
    chassis_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicles_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_vehicles_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE INDEX idx_vehicles_organization ON vehicles(organization_id);
CREATE INDEX idx_vehicles_client ON vehicles(client_id);
CREATE INDEX idx_vehicles_license_plate ON vehicles(license_plate);

-- ============================================
-- SERVICE ORDERS TABLE
-- ============================================
CREATE TABLE service_orders (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    vehicle_id INT NOT NULL,
    client_id INT NOT NULL,
    description TEXT,
    problem_description TEXT,
    diagnosis TEXT,
    solution_description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    labor_cost NUMERIC(10,2) DEFAULT 0,
    parts_cost NUMERIC(10,2) DEFAULT 0,
    total_cost NUMERIC(10,2) DEFAULT 0,
    discount NUMERIC(10,2) DEFAULT 0,
    estimated_completion TIMESTAMP,
    actual_completion TIMESTAMP,
    observations TEXT,
    technician_name VARCHAR(100),
    current_kilometers NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_orders_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_service_orders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_service_orders_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE UNIQUE INDEX uk_service_orders_number_org ON service_orders(order_number, organization_id);
CREATE INDEX idx_service_orders_organization ON service_orders(organization_id);
CREATE INDEX idx_service_orders_status ON service_orders(status);
CREATE INDEX idx_service_orders_client ON service_orders(client_id);
CREATE INDEX idx_service_orders_vehicle ON service_orders(vehicle_id);
CREATE INDEX idx_service_orders_created ON service_orders(created_at);
CREATE INDEX idx_service_orders_technician ON service_orders(technician_name);
CREATE INDEX idx_service_orders_estimated ON service_orders(estimated_completion);

-- ============================================
-- SERVICE ORDER ITEMS TABLE
-- ============================================
CREATE TABLE service_order_items (
    id SERIAL PRIMARY KEY,
    service_order_id INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    quantity INT DEFAULT 1,
    unit_price NUMERIC(10,2) DEFAULT 0,
    total_price NUMERIC(10,2) DEFAULT 0,
    product_code VARCHAR(100),
    requires_stock BOOLEAN DEFAULT FALSE,
    stock_reserved BOOLEAN DEFAULT FALSE,
    stock_product_id INT,
    applied BOOLEAN DEFAULT FALSE,
    observations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_order_items_so FOREIGN KEY (service_order_id) REFERENCES service_orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_service_order_items_so ON service_order_items(service_order_id);
CREATE INDEX idx_service_order_items_type ON service_order_items(item_type);
CREATE INDEX idx_service_order_items_product ON service_order_items(product_code);
CREATE INDEX idx_service_order_items_requires_stock ON service_order_items(requires_stock);
CREATE INDEX idx_service_order_items_applied ON service_order_items(applied);

-- ============================================
-- PARTS TABLE
-- ============================================
CREATE TABLE parts (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(150),
    description TEXT,
    unit_cost NUMERIC(10,2),
    unit_price NUMERIC(10,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parts_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE UNIQUE INDEX uk_parts_sku_org ON parts(sku, organization_id);
CREATE INDEX idx_parts_organization ON parts(organization_id);
CREATE INDEX idx_parts_active ON parts(active);

-- ============================================
-- INVENTORY ITEMS TABLE
-- ============================================
CREATE TABLE inventory_items (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    part_id INT NOT NULL,
    location VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    minimum_quantity INT NOT NULL DEFAULT 0,
    unit_cost NUMERIC(10,2),
    sale_price NUMERIC(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_items_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_inventory_items_part FOREIGN KEY (part_id) REFERENCES parts(id),
    CONSTRAINT uk_inventory_part_location UNIQUE (part_id, location)
);

CREATE INDEX idx_inventory_items_organization ON inventory_items(organization_id);
CREATE INDEX idx_inventory_items_part ON inventory_items(part_id);

-- ============================================
-- INVENTORY MOVEMENTS TABLE
-- ============================================
CREATE TABLE inventory_movements (
    id SERIAL PRIMARY KEY,
    organization_id INT NOT NULL,
    inventory_item_id INT NOT NULL,
    part_id INT NOT NULL,
    service_order_id INT,
    vehicle_id INT,
    movement_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    reference_code VARCHAR(100),
    notes TEXT,
    movement_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_movements_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_inventory_movements_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT fk_inventory_movements_part FOREIGN KEY (part_id) REFERENCES parts(id),
    CONSTRAINT fk_inventory_movements_so FOREIGN KEY (service_order_id) REFERENCES service_orders(id),
    CONSTRAINT fk_inventory_movements_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);

CREATE INDEX idx_inventory_movements_organization ON inventory_movements(organization_id);
CREATE INDEX idx_inventory_movements_item ON inventory_movements(inventory_item_id);
CREATE INDEX idx_inventory_movements_part ON inventory_movements(part_id);
CREATE INDEX idx_inventory_movements_so ON inventory_movements(service_order_id);
CREATE INDEX idx_inventory_movements_vehicle ON inventory_movements(vehicle_id);
CREATE INDEX idx_inventory_movements_date ON inventory_movements(movement_date);

-- ============================================
-- AUDIT EVENTS TABLE
-- ============================================
CREATE TABLE audit_events (
    id SERIAL PRIMARY KEY,
    organization_id INT,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT,
    operation VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    module_name VARCHAR(255) NOT NULL,
    user_role VARCHAR(64) NOT NULL,
    entity_id BIGINT,
    occurred_at TIMESTAMP NOT NULL,
    event_hash VARCHAR(128) NOT NULL,
    blockchain_reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_audit_events_organization ON audit_events(organization_id);
CREATE INDEX idx_audit_events_entity_id ON audit_events(entity_id);
CREATE INDEX idx_audit_events_user_email ON audit_events(user_email);
CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at);

-- ============================================
-- CONVERSATIONS TABLE (for AI chatbot)
-- ============================================
CREATE TABLE conversations (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_conversations_user ON conversations(user_id);

-- ============================================
-- INITIAL DATA
-- ============================================

-- Insert default organization
INSERT INTO organizations (id, name, slug, description, active, contact_email) 
VALUES (1, 'GoMech', 'gomech', 'Organização principal do sistema GoMech', true, 'contato@gomech.com');

-- Reset sequence for organizations
SELECT setval('organizations_id_seq', 1, true);

-- Insert admin user
-- Password: admin123 (BCrypt hash)
INSERT INTO users (name, email, password, role, mfa_enabled, organization_id) 
VALUES (
    'Administrador',
    'admin@gomech.com',
    '$2a$10$xLVqN0mPq3nPmF5oRpq8a.fH3xJZxJGZ3eWYELK/8wvxDfQJXz9Gi',
    'ADMIN',
    false,
    1
);

-- ============================================
-- COMMENTS ON TABLES
-- ============================================
COMMENT ON TABLE organizations IS 'Organizations for multi-tenancy isolation';
COMMENT ON TABLE users IS 'System users with organization association';
COMMENT ON TABLE clients IS 'Customers/clients of the auto repair shop';
COMMENT ON TABLE vehicles IS 'Vehicles owned by clients';
COMMENT ON TABLE service_orders IS 'Service orders for vehicle maintenance and repair';
COMMENT ON TABLE service_order_items IS 'Items (parts/labor) in service orders';
COMMENT ON TABLE parts IS 'Parts catalog for inventory management';
COMMENT ON TABLE inventory_items IS 'Inventory stock items';
COMMENT ON TABLE inventory_movements IS 'History of inventory movements';
COMMENT ON TABLE audit_events IS 'Audit trail of system events';
COMMENT ON TABLE conversations IS 'AI chatbot conversation history';

-- ============================================
-- END OF SCHEMA
-- ============================================

