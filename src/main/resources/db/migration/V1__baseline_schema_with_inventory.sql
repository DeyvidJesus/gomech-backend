CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    authorities VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    document VARCHAR(100),
    phone VARCHAR(100),
    email VARCHAR(255),
    address VARCHAR(255),
    birth_date DATE,
    observations TEXT,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicles (
    id SERIAL PRIMARY KEY,
    license_plate VARCHAR(20),
    brand VARCHAR(100),
    model VARCHAR(100),
    manufacture_date DATE,
    color VARCHAR(50),
    observations TEXT,
    kilometers NUMERIC(10,2),
    chassis_id VARCHAR(100),
    client_id INT,
    CONSTRAINT fk_vehicles_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE IF NOT EXISTS service_orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
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
    CONSTRAINT fk_service_orders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_service_orders_client FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE IF NOT EXISTS service_items (
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
    CONSTRAINT fk_inventory_item_part FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT,
    CONSTRAINT uk_inventory_part_location UNIQUE (part_id, location)
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id SERIAL PRIMARY KEY,
    inventory_item_id INT NOT NULL,
    part_id INT NOT NULL,
    service_order_id INT,
    vehicle_id INT,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    reference_code VARCHAR(100),
    notes TEXT,
    movement_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_movement_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_movement_part FOREIGN KEY (part_id) REFERENCES parts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movement_service_order FOREIGN KEY (service_order_id) REFERENCES service_orders(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_movement_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_service_orders_status ON service_orders(status);
CREATE INDEX IF NOT EXISTS idx_service_orders_client ON service_orders(client_id);
CREATE INDEX IF NOT EXISTS idx_service_orders_vehicle ON service_orders(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_service_orders_created ON service_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_service_orders_technician ON service_orders(technician_name);
CREATE INDEX IF NOT EXISTS idx_service_orders_estimated ON service_orders(estimated_completion);

CREATE INDEX IF NOT EXISTS idx_service_order_items_so ON service_items(service_order_id);
CREATE INDEX IF NOT EXISTS idx_service_order_items_type ON service_items(item_type);
CREATE INDEX IF NOT EXISTS idx_service_order_items_product ON service_items(product_code);
CREATE INDEX IF NOT EXISTS idx_service_order_items_requires_stock ON service_items(requires_stock);
CREATE INDEX IF NOT EXISTS idx_service_order_items_applied ON service_items(applied);

CREATE INDEX IF NOT EXISTS idx_parts_sku ON parts(sku);
CREATE INDEX IF NOT EXISTS idx_parts_active ON parts(active);
CREATE INDEX IF NOT EXISTS idx_inventory_items_part ON inventory_items(part_id);
CREATE INDEX IF NOT EXISTS idx_inventory_items_location ON inventory_items(location);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_item ON inventory_movements(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_type ON inventory_movements(movement_type);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_service_order ON inventory_movements(service_order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_vehicle ON inventory_movements(vehicle_id);

