-- ============================================
-- GOMECH SAMPLE DATA
-- Optional test data for development
-- ============================================
-- Execute this AFTER schema.sql if you want sample data

-- ============================================
-- ADDITIONAL ORGANIZATIONS
-- ============================================
INSERT INTO organizations (name, slug, description, active, contact_email, contact_phone, address) VALUES
('Oficina Turbo', 'oficina-turbo', 'Especializada em manutenção de veículos turbo', true, 'contato@oficinaturbo.com', '(11) 98765-4321', 'Rua das Flores, 123 - São Paulo, SP'),
('Auto Center Premium', 'auto-center-premium', 'Centro automotivo premium', true, 'contato@autopremium.com', '(21) 99876-5432', 'Av. Principal, 456 - Rio de Janeiro, RJ');

-- ============================================
-- ADDITIONAL USERS
-- ============================================
-- Password for all: senha123
INSERT INTO users (name, email, password, role, mfa_enabled, organization_id) VALUES
-- GoMech users
('João Silva', 'joao@gomech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'USER', false, 1),
('Maria Santos', 'maria@gomech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'USER', false, 1),

-- Oficina Turbo users
('Carlos Turbo', 'carlos@turbo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'ADMIN', false, 2),
('Ana Turbo', 'ana@turbo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'USER', false, 2),

-- Auto Center Premium users
('Roberto Premium', 'roberto@premium.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'ADMIN', false, 3),
('Fernanda Premium', 'fernanda@premium.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1LnL.xhqW5yCJVfAGGWXb9LcZvNPVpC', 'USER', false, 3);

-- ============================================
-- CLIENTS (Organization 1 - GoMech)
-- ============================================
INSERT INTO clients (organization_id, name, document, phone, email, address, birth_date, observations) VALUES
(1, 'Pedro Oliveira', '123.456.789-00', '(11) 91234-5678', 'pedro@email.com', 'Rua A, 100 - SP', '1985-03-15', 'Cliente VIP'),
(1, 'Juliana Costa', '987.654.321-00', '(11) 98765-4321', 'juliana@email.com', 'Av. B, 200 - SP', '1990-07-22', 'Prefere atendimento pela manhã'),
(1, 'Ricardo Mendes', '456.789.123-00', '(11) 99999-8888', 'ricardo@email.com', 'Rua C, 300 - SP', '1978-11-30', NULL);

-- ============================================
-- CLIENTS (Organization 2 - Oficina Turbo)
-- ============================================
INSERT INTO clients (organization_id, name, document, phone, email, address, birth_date, observations) VALUES
(2, 'Lucas Almeida', '111.222.333-44', '(11) 97777-6666', 'lucas@email.com', 'Rua X, 10 - SP', '1988-05-10', 'Possui veículo turbo'),
(2, 'Carla Ferreira', '222.333.444-55', '(11) 96666-5555', 'carla@email.com', 'Av. Y, 20 - SP', '1992-09-18', NULL);

-- ============================================
-- CLIENTS (Organization 3 - Auto Center Premium)
-- ============================================
INSERT INTO clients (organization_id, name, document, phone, email, address, birth_date, observations) VALUES
(3, 'Paulo Rodrigues', '333.444.555-66', '(21) 95555-4444', 'paulo@email.com', 'Rua Z, 30 - RJ', '1980-01-25', 'Cliente executivo'),
(3, 'Amanda Silva', '444.555.666-77', '(21) 94444-3333', 'amanda@email.com', 'Av. W, 40 - RJ', '1995-12-08', 'Possui frota de veículos');

-- ============================================
-- VEHICLES (Organization 1 - GoMech)
-- ============================================
INSERT INTO vehicles (organization_id, client_id, license_plate, brand, model, manufacture_date, color, observations, kilometers, chassis_id) VALUES
(1, 1, 'ABC-1234', 'Honda', 'Civic', '2020-01-01', 'Prata', 'Bem conservado', 45000, '9BWZZZ377VT004251'),
(1, 1, 'DEF-5678', 'Toyota', 'Corolla', '2019-06-15', 'Branco', NULL, 62000, '9BWZZZ377VT004252'),
(1, 2, 'GHI-9012', 'Volkswagen', 'Golf', '2021-03-20', 'Preto', 'Revisões em dia', 28000, '9BWZZZ377VT004253'),
(1, 3, 'JKL-3456', 'Ford', 'Focus', '2018-11-10', 'Vermelho', 'Necessita revisão', 89000, '9BWZZZ377VT004254');

-- ============================================
-- VEHICLES (Organization 2 - Oficina Turbo)
-- ============================================
INSERT INTO vehicles (organization_id, client_id, license_plate, brand, model, manufacture_date, color, observations, kilometers, chassis_id) VALUES
(2, 4, 'MNO-7890', 'Subaru', 'WRX', '2022-02-14', 'Azul', 'Veículo turbo', 15000, '9BWZZZ377VT004255'),
(2, 5, 'PQR-1234', 'Audi', 'A3', '2021-08-30', 'Cinza', NULL, 32000, '9BWZZZ377VT004256');

-- ============================================
-- VEHICLES (Organization 3 - Auto Center Premium)
-- ============================================
INSERT INTO vehicles (organization_id, client_id, license_plate, brand, model, manufacture_date, color, observations, kilometers, chassis_id) VALUES
(3, 6, 'STU-5678', 'BMW', '320i', '2023-01-05', 'Preto', 'Garantia de fábrica', 8000, '9BWZZZ377VT004257'),
(3, 7, 'VWX-9012', 'Mercedes', 'C180', '2022-07-20', 'Prata', 'Frota corporativa', 18000, '9BWZZZ377VT004258'),
(3, 7, 'YZA-3456', 'Audi', 'A4', '2023-03-15', 'Branco', 'Frota corporativa', 12000, '9BWZZZ377VT004259');

-- ============================================
-- PARTS (Organization 1 - GoMech)
-- ============================================
INSERT INTO parts (organization_id, name, sku, manufacturer, description, unit_cost, unit_price, active) VALUES
(1, 'Filtro de Óleo', 'FO-001', 'Bosch', 'Filtro de óleo para motores 1.6 a 2.0', 25.00, 45.00, true),
(1, 'Pastilha de Freio Dianteira', 'PF-001', 'TRW', 'Jogo de pastilhas para freio dianteiro', 85.00, 150.00, true),
(1, 'Vela de Ignição', 'VI-001', 'NGK', 'Vela de ignição padrão', 18.00, 32.00, true),
(1, 'Correia Dentada', 'CD-001', 'Gates', 'Correia dentada com tensor', 120.00, 220.00, true),
(1, 'Bateria 60Ah', 'BT-001', 'Moura', 'Bateria 60 amperes', 280.00, 450.00, true);

-- ============================================
-- PARTS (Organization 2 - Oficina Turbo)
-- ============================================
INSERT INTO parts (organization_id, name, sku, manufacturer, description, unit_cost, unit_price, active) VALUES
(2, 'Turbina Recondicionada', 'TB-001', 'Garrett', 'Turbina recondicionada com garantia', 1500.00, 2500.00, true),
(2, 'Intercooler', 'IC-001', 'Forge', 'Intercooler esportivo', 800.00, 1400.00, true),
(2, 'Filtro de Ar Esportivo', 'FA-001', 'K&N', 'Filtro de ar de alto fluxo', 250.00, 450.00, true);

-- ============================================
-- PARTS (Organization 3 - Auto Center Premium)
-- ============================================
INSERT INTO parts (organization_id, name, sku, manufacturer, description, unit_cost, unit_price, active) VALUES
(3, 'Óleo Sintético 5W30', 'OL-001', 'Castrol', 'Óleo sintético premium 1L', 45.00, 85.00, true),
(3, 'Disco de Freio Premium', 'DF-001', 'Brembo', 'Disco de freio ventilado premium', 320.00, 580.00, true),
(3, 'Suspensão Completa', 'SP-001', 'Bilstein', 'Kit suspensão esportiva', 2200.00, 3800.00, true);

-- ============================================
-- INVENTORY ITEMS (Organization 1 - GoMech)
-- ============================================
INSERT INTO inventory_items (organization_id, part_id, location, quantity, reserved_quantity, minimum_quantity, unit_cost, sale_price) VALUES
(1, 1, 'Estoque Principal', 50, 0, 10, 25.00, 45.00),
(1, 2, 'Estoque Principal', 20, 2, 5, 85.00, 150.00),
(1, 3, 'Estoque Principal', 100, 5, 20, 18.00, 32.00),
(1, 4, 'Estoque Principal', 15, 1, 3, 120.00, 220.00),
(1, 5, 'Estoque Principal', 8, 0, 2, 280.00, 450.00);

-- ============================================
-- INVENTORY ITEMS (Organization 2 - Oficina Turbo)
-- ============================================
INSERT INTO inventory_items (organization_id, part_id, location, quantity, reserved_quantity, minimum_quantity, unit_cost, sale_price) VALUES
(2, 6, 'Oficina', 3, 0, 1, 1500.00, 2500.00),
(2, 7, 'Oficina', 5, 1, 2, 800.00, 1400.00),
(2, 8, 'Oficina', 12, 0, 3, 250.00, 450.00);

-- ============================================
-- INVENTORY ITEMS (Organization 3 - Auto Center Premium)
-- ============================================
INSERT INTO inventory_items (organization_id, part_id, location, quantity, reserved_quantity, minimum_quantity, unit_cost, sale_price) VALUES
(3, 9, 'Depósito Central', 80, 5, 15, 45.00, 85.00),
(3, 10, 'Depósito Central', 25, 2, 5, 320.00, 580.00),
(3, 11, 'Depósito Central', 4, 0, 1, 2200.00, 3800.00);

-- ============================================
-- SERVICE ORDERS (Organization 1 - GoMech)
-- ============================================
INSERT INTO service_orders (organization_id, order_number, vehicle_id, client_id, description, problem_description, status, labor_cost, parts_cost, total_cost, technician_name, current_kilometers) VALUES
(1, 'OS-20250106-001', 1, 1, 'Revisão dos 45.000 km', 'Revisão preventiva', 'IN_PROGRESS', 150.00, 295.00, 445.00, 'João Silva', 45000.00),
(1, 'OS-20250106-002', 3, 2, 'Troca de pastilhas de freio', 'Freios rangendo', 'PENDING', 80.00, 150.00, 230.00, 'Maria Santos', 28000.00),
(1, 'OS-20250105-001', 4, 3, 'Revisão completa', 'Manutenção geral', 'COMPLETED', 250.00, 520.00, 770.00, 'João Silva', 89000.00);

-- ============================================
-- SERVICE ORDERS (Organization 2 - Oficina Turbo)
-- ============================================
INSERT INTO service_orders (organization_id, order_number, vehicle_id, client_id, description, problem_description, status, labor_cost, parts_cost, total_cost, technician_name, current_kilometers) VALUES
(2, 'OS-20250106-100', 5, 4, 'Instalação de Intercooler', 'Upgrade de performance', 'IN_PROGRESS', 300.00, 1400.00, 1700.00, 'Carlos Turbo', 15000.00),
(2, 'OS-20250105-100', 6, 5, 'Revisão turbo', 'Manutenção preventiva turbo', 'COMPLETED', 200.00, 450.00, 650.00, 'Ana Turbo', 32000.00);

-- ============================================
-- SERVICE ORDERS (Organization 3 - Auto Center Premium)
-- ============================================
INSERT INTO service_orders (organization_id, order_number, vehicle_id, client_id, description, problem_description, status, labor_cost, parts_cost, total_cost, technician_name, current_kilometers) VALUES
(3, 'OS-20250106-500', 7, 6, 'Troca de óleo e filtros', 'Manutenção preventiva', 'COMPLETED', 100.00, 170.00, 270.00, 'Roberto Premium', 8000.00),
(3, 'OS-20250106-501', 8, 7, 'Troca de discos de freio', 'Desgaste de discos', 'IN_PROGRESS', 180.00, 1160.00, 1340.00, 'Fernanda Premium', 18000.00);

-- ============================================
-- SERVICE ORDER ITEMS
-- ============================================
-- Items for OS-20250106-001 (GoMech)
INSERT INTO service_order_items (service_order_id, description, item_type, quantity, unit_price, total_price, applied) VALUES
(1, 'Filtro de Óleo', 'PART', 1, 45.00, 45.00, true),
(1, 'Vela de Ignição', 'PART', 4, 32.00, 128.00, true),
(1, 'Mão de obra - Revisão', 'LABOR', 1, 150.00, 150.00, true);

-- Items for OS-20250106-002 (GoMech)
INSERT INTO service_order_items (service_order_id, description, item_type, quantity, unit_price, total_price, applied) VALUES
(2, 'Pastilha de Freio Dianteira', 'PART', 1, 150.00, 150.00, true),
(2, 'Mão de obra - Troca de pastilhas', 'LABOR', 1, 80.00, 80.00, true);

-- Items for OS-20250106-100 (Oficina Turbo)
INSERT INTO service_order_items (service_order_id, description, item_type, quantity, unit_price, total_price, applied) VALUES
(4, 'Intercooler', 'PART', 1, 1400.00, 1400.00, true),
(4, 'Mão de obra - Instalação', 'LABOR', 1, 300.00, 300.00, true);

-- Items for OS-20250106-500 (Auto Center Premium)
INSERT INTO service_order_items (service_order_id, description, item_type, quantity, unit_price, total_price, applied) VALUES
(6, 'Óleo Sintético 5W30', 'PART', 2, 85.00, 170.00, true),
(6, 'Mão de obra - Troca de óleo', 'LABOR', 1, 100.00, 100.00, true);

-- ============================================
-- SUMMARY
-- ============================================
-- Organizations: 3
-- Users: 7 (1 admin + 6 regular/admin users)
-- Clients: 8 (distributed across organizations)
-- Vehicles: 9 (distributed across organizations)
-- Parts: 11 (distributed across organizations)
-- Inventory Items: 11 (distributed across organizations)
-- Service Orders: 7 (distributed across organizations)
-- Service Order Items: 10

-- ============================================
-- CREDENTIALS SUMMARY
-- ============================================
-- admin@gomech.com / admin123 (ADMIN - Org 1)
-- joao@gomech.com / senha123 (USER - Org 1)
-- maria@gomech.com / senha123 (USER - Org 1)
-- carlos@turbo.com / senha123 (ADMIN - Org 2)
-- ana@turbo.com / senha123 (USER - Org 2)
-- roberto@premium.com / senha123 (ADMIN - Org 3)
-- fernanda@premium.com / senha123 (USER - Org 3)

