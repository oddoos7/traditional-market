ALTER TABLE orders ADD COLUMN paid_at DATETIME(6) NULL;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    payment_key VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    method VARCHAR(50) NULL,
    amount DECIMAL(18, 2) NOT NULL,
    approved_at DATETIME(6) NOT NULL,
    receipt_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT uk_payments_payment_key UNIQUE (payment_key),
    CONSTRAINT ck_payments_provider CHECK (provider IN ('TOSS')),
    CONSTRAINT ck_payments_status CHECK (
        status IN (
            'READY',
            'IN_PROGRESS',
            'WAITING_FOR_DEPOSIT',
            'DONE',
            'CANCELED',
            'PARTIAL_CANCELED',
            'ABORTED',
            'EXPIRED'
        )
    ),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_approved_at ON payments (approved_at);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id);
