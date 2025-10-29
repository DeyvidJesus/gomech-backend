CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    authorities VARCHAR(1000)
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE clients (
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

CREATE TABLE vehicles (
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

CREATE TABLE service_orders (
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

-- Índices
CREATE INDEX idx_service_orders_status ON service_orders(status);
CREATE INDEX idx_service_orders_client ON service_orders(client_id);
CREATE INDEX idx_service_orders_vehicle ON service_orders(vehicle_id);
CREATE INDEX idx_service_orders_created ON service_orders(created_at);
CREATE INDEX idx_service_orders_technician ON service_orders(technician_name);
CREATE INDEX idx_service_orders_estimated ON service_orders(estimated_completion);

CREATE INDEX idx_service_order_items_so ON service_order_items(service_order_id);
CREATE INDEX idx_service_order_items_type ON service_order_items(item_type);
CREATE INDEX idx_service_order_items_product ON service_order_items(product_code);
CREATE INDEX idx_service_order_items_requires_stock ON service_order_items(requires_stock);
CREATE INDEX idx_service_order_items_applied ON service_order_items(applied);
