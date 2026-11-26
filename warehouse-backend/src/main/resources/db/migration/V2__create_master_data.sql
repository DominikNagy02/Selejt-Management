-- V2__create_master_data.sql
-- Master data schema: warehouses, zones, shelves, products (FR-01, FR-02, FR-05)

CREATE TABLE warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    zone_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_zones_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT uq_zones_warehouse_code UNIQUE (warehouse_id, code),
    CONSTRAINT chk_zones_type CHECK (zone_type IN ('NORMAL', 'CHILLED', 'HAZARDOUS'))
);

CREATE TABLE shelves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    storage_type VARCHAR(20) NOT NULL DEFAULT 'RACK',
    max_tiers INT NOT NULL DEFAULT 1,
    max_weight_kg DECIMAL(12,3) NOT NULL,
    max_volume_m3 DECIMAL(12,6) NOT NULL,
    current_weight_kg DECIMAL(12,3) NOT NULL DEFAULT 0.000,
    current_volume_m3 DECIMAL(12,6) NOT NULL DEFAULT 0.000000,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shelves_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE RESTRICT,
    CONSTRAINT chk_shelves_storage_type CHECK (storage_type IN ('RACK', 'GROUND')),
    CONSTRAINT chk_shelves_max_tiers CHECK (max_tiers >= 1),
    CONSTRAINT chk_shelves_max_weight CHECK (max_weight_kg > 0),
    CONSTRAINT chk_shelves_max_volume CHECK (max_volume_m3 > 0),
    CONSTRAINT chk_shelves_current_weight CHECK (current_weight_kg >= 0 AND current_weight_kg <= max_weight_kg),
    CONSTRAINT chk_shelves_current_volume CHECK (current_volume_m3 >= 0 AND current_volume_m3 <= max_volume_m3)
);

CREATE INDEX idx_shelves_zone ON shelves (zone_id, active);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(40) NOT NULL UNIQUE,
    barcode VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    unit_weight_kg DECIMAL(12,3) NOT NULL,
    unit_volume_m3 DECIMAL(12,6) NOT NULL,
    required_zone_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    min_stock_level INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_products_unit_weight CHECK (unit_weight_kg > 0),
    CONSTRAINT chk_products_unit_volume CHECK (unit_volume_m3 > 0),
    CONSTRAINT chk_products_zone_type CHECK (required_zone_type IN ('NORMAL', 'CHILLED', 'HAZARDOUS')),
    CONSTRAINT chk_products_min_stock CHECK (min_stock_level >= 0)
);
