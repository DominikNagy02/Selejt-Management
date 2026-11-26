-- V3__create_inventory.sql
-- Inventory schema: lots and inventory_items (FR-03, FR-08, FR-09)

CREATE TABLE lots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    expiration_date DATE NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lots_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT uq_lots_product_lot_number UNIQUE (product_id, lot_number)
);

CREATE INDEX idx_lots_expiration ON lots (expiration_date);

CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shelf_id BIGINT NOT NULL,
    lot_id BIGINT NOT NULL,
    tier_level INT NOT NULL DEFAULT 0,
    quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'STORED',
    received_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_inv_shelf FOREIGN KEY (shelf_id) REFERENCES shelves (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inv_lot FOREIGN KEY (lot_id) REFERENCES lots (id) ON DELETE RESTRICT,
    CONSTRAINT uq_inv_shelf_lot_tier UNIQUE (shelf_id, lot_id, tier_level),
    CONSTRAINT chk_inv_tier_level CHECK (tier_level >= 0),
    CONSTRAINT chk_inv_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_inv_reserved_quantity CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity),
    CONSTRAINT chk_inv_status CHECK (status IN ('STORED', 'BLOCKED', 'DEPLETED'))
);

CREATE INDEX idx_inv_lot ON inventory_items (lot_id);
CREATE INDEX idx_inv_shelf ON inventory_items (shelf_id);
CREATE INDEX idx_inv_status_lot ON inventory_items (status, lot_id);
