-- V4__create_orders_tasks_reservations.sql
-- Delivery orders, tasks, and soft-reservations (FR-06, FR-07, FR-08, 5.2, 5.3)

CREATE TABLE delivery_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    do_number VARCHAR(30) NOT NULL UNIQUE,
    customer_name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_date DATE NULL,
    created_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_do_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_do_status CHECK (status IN ('OPEN', 'IN_PICKING', 'PICKED', 'SHIPPED', 'CANCELLED'))
);

CREATE TABLE delivery_order_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    product_id BIGINT NOT NULL,
    requested_lot_id BIGINT NULL,
    quantity INT NOT NULL,
    picked_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_do_lines_order FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_do_lines_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_do_lines_requested_lot FOREIGN KEY (requested_lot_id) REFERENCES lots (id) ON DELETE RESTRICT,
    CONSTRAINT uq_do_lines_order_line UNIQUE (delivery_order_id, line_no),
    CONSTRAINT chk_do_lines_quantity CHECK (quantity > 0),
    CONSTRAINT chk_do_lines_picked_quantity CHECK (picked_quantity >= 0 AND picked_quantity <= quantity)
);

CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 3,
    delivery_order_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    assigned_to BIGINT NULL,
    note VARCHAR(500) NULL,
    cancel_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_do FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tasks_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_tasks_type CHECK (task_type IN ('PUTAWAY', 'PICKING', 'TRANSFER', 'SCRAP')),
    CONSTRAINT chk_tasks_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_tasks_priority CHECK (priority BETWEEN 1 AND 5)
);

CREATE INDEX idx_tasks_status_priority ON tasks (status, priority);
CREATE INDEX idx_tasks_assignee ON tasks (assigned_to, status);

CREATE TABLE task_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    lot_id BIGINT NOT NULL,
    source_item_id BIGINT NULL,
    source_shelf_id BIGINT NULL,
    source_tier_level INT NULL,
    target_shelf_id BIGINT NULL,
    target_tier_level INT NULL,
    delivery_order_line_id BIGINT NULL,
    quantity INT NOT NULL,
    confirmed_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_lines_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_lines_lot FOREIGN KEY (lot_id) REFERENCES lots (id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_lines_source_item FOREIGN KEY (source_item_id) REFERENCES inventory_items (id) ON DELETE SET NULL,
    CONSTRAINT fk_task_lines_source_shelf FOREIGN KEY (source_shelf_id) REFERENCES shelves (id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_lines_target_shelf FOREIGN KEY (target_shelf_id) REFERENCES shelves (id) ON DELETE RESTRICT,
    CONSTRAINT fk_task_lines_do_line FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_lines (id) ON DELETE SET NULL,
    CONSTRAINT uq_task_lines_task_line UNIQUE (task_id, line_no),
    CONSTRAINT chk_task_lines_quantity CHECK (quantity > 0),
    CONSTRAINT chk_task_lines_confirmed_quantity CHECK (confirmed_quantity >= 0 AND confirmed_quantity <= quantity),
    CONSTRAINT chk_task_lines_shelf_present CHECK (source_shelf_id IS NOT NULL OR target_shelf_id IS NOT NULL)
);

CREATE TABLE stock_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    task_line_id BIGINT NULL,
    delivery_order_line_id BIGINT NULL,
    reservation_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    CONSTRAINT fk_res_inv_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_task_line FOREIGN KEY (task_line_id) REFERENCES task_lines (id) ON DELETE SET NULL,
    CONSTRAINT fk_res_do_line FOREIGN KEY (delivery_order_line_id) REFERENCES delivery_order_lines (id) ON DELETE SET NULL,
    CONSTRAINT fk_res_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_res_type CHECK (reservation_type IN ('PICKING', 'TRANSFER')),
    CONSTRAINT chk_res_quantity CHECK (quantity > 0),
    CONSTRAINT chk_res_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_res_status_expires ON stock_reservations (status, expires_at);
CREATE INDEX idx_res_item_status ON stock_reservations (inventory_item_id, status);
