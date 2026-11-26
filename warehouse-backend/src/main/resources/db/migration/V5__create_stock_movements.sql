-- V5__create_stock_movements.sql
-- Stock movement audit ledger (FR-07, FR-10, FR-11, FR-14)

CREATE TABLE stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movement_type VARCHAR(20) NOT NULL,
    lot_id BIGINT NOT NULL,
    inventory_item_id BIGINT NULL,
    from_shelf_id BIGINT NULL,
    from_tier_level INT NULL,
    to_shelf_id BIGINT NULL,
    to_tier_level INT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(500) NULL,
    task_id BIGINT NULL,
    delivery_order_id BIGINT NULL,
    performed_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_mov_lot FOREIGN KEY (lot_id) REFERENCES lots (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_inv_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id) ON DELETE SET NULL,
    CONSTRAINT fk_mov_from_shelf FOREIGN KEY (from_shelf_id) REFERENCES shelves (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_to_shelf FOREIGN KEY (to_shelf_id) REFERENCES shelves (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE SET NULL,
    CONSTRAINT fk_mov_do FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_mov_performed_by FOREIGN KEY (performed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_mov_type CHECK (movement_type IN ('INBOUND', 'TRANSFER', 'PICKING', 'SCRAP', 'ADJUSTMENT')),
    CONSTRAINT chk_mov_quantity CHECK (quantity > 0),
    CONSTRAINT chk_mov_scrap_reason CHECK (movement_type <> 'SCRAP' OR (reason IS NOT NULL AND LENGTH(TRIM(reason)) > 0))
);

CREATE INDEX idx_mov_created ON stock_movements (created_at);
CREATE INDEX idx_mov_type_created ON stock_movements (movement_type, created_at);
CREATE INDEX idx_mov_lot ON stock_movements (lot_id);
