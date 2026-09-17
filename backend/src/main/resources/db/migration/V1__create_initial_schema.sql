CREATE TABLE markets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    opening_hours VARCHAR(100) NULL,
    closed_days VARCHAR(100) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    image_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_markets PRIMARY KEY (id),
    CONSTRAINT ck_markets_status CHECK (status IN ('OPEN', 'TEMPORARILY_CLOSED', 'CLOSED'))
);

CREATE INDEX idx_markets_name ON markets (name);
CREATE INDEX idx_markets_status ON markets (status);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    category VARCHAR(30) NOT NULL,
    location VARCHAR(100) NULL,
    phone VARCHAR(20) NULL,
    opening_hours VARCHAR(100) NULL,
    closed_days VARCHAR(100) NULL,
    image_url VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT ck_stores_category CHECK (
        category IN ('AGRICULTURAL', 'FOOD', 'SEAFOOD', 'LIVESTOCK', 'RESTAURANT', 'CLOTHING', 'HOUSEHOLD', 'ETC')
    ),
    CONSTRAINT ck_stores_status CHECK (status IN ('OPEN', 'TEMPORARILY_CLOSED', 'CLOSED'))
);

CREATE INDEX idx_stores_market_id ON stores (market_id);
CREATE INDEX idx_stores_market_name ON stores (market_id, name);
CREATE INDEX idx_stores_market_category ON stores (market_id, category);
CREATE INDEX idx_stores_market_status ON stores (market_id, status);
ALTER TABLE stores
    ADD CONSTRAINT fk_stores_market FOREIGN KEY (market_id) REFERENCES markets (id);

CREATE TABLE store_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_store_favorites PRIMARY KEY (id),
    CONSTRAINT uk_store_favorites_user_store UNIQUE (user_id, store_id)
);

CREATE INDEX idx_store_favorites_user_id ON store_favorites (user_id);
CREATE INDEX idx_store_favorites_store_id ON store_favorites (store_id);
ALTER TABLE store_favorites
    ADD CONSTRAINT fk_store_favorites_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE store_favorites
    ADD CONSTRAINT fk_store_favorites_store FOREIGN KEY (store_id) REFERENCES stores (id);

CREATE TABLE store_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_store_reviews PRIMARY KEY (id),
    CONSTRAINT uk_store_reviews_user_store UNIQUE (user_id, store_id),
    CONSTRAINT ck_store_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_store_reviews_store_id ON store_reviews (store_id);
CREATE INDEX idx_store_reviews_user_id ON store_reviews (user_id);
CREATE INDEX idx_store_reviews_store_created ON store_reviews (store_id, created_at);
ALTER TABLE store_reviews
    ADD CONSTRAINT fk_store_reviews_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE store_reviews
    ADD CONSTRAINT fk_store_reviews_store FOREIGN KEY (store_id) REFERENCES stores (id);
