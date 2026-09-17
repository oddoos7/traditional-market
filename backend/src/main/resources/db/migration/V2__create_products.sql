CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    price DECIMAL(12, 2) NOT NULL,
    stock_quantity INT NOT NULL,
    image_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT ck_products_price CHECK (price > 0 AND price <= 9999999999.99),
    CONSTRAINT ck_products_stock_quantity CHECK (stock_quantity BETWEEN 0 AND 1000000),
    CONSTRAINT ck_products_status CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'HIDDEN'))
);

CREATE INDEX idx_products_store_id ON products (store_id);
CREATE INDEX idx_products_store_name ON products (store_id, name);
CREATE INDEX idx_products_store_status ON products (store_id, status);
CREATE INDEX idx_products_store_price ON products (store_id, price);

ALTER TABLE products
    ADD CONSTRAINT fk_products_store FOREIGN KEY (store_id) REFERENCES stores (id);
