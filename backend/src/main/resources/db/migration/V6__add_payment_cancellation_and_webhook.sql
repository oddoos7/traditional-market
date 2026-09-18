ALTER TABLE payments ADD COLUMN confirmation_idempotency_key VARCHAR(80) NULL;
ALTER TABLE payments ADD COLUMN balance_amount DECIMAL(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN canceled_at DATETIME(6) NULL;

UPDATE payments SET balance_amount = amount WHERE balance_amount = 0;

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_confirmation_idempotency UNIQUE (confirmation_idempotency_key);
ALTER TABLE payments
    ADD CONSTRAINT ck_payments_balance_amount CHECK (
        balance_amount >= 0 AND balance_amount <= amount
    );

CREATE TABLE payment_cancellations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    transaction_key VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(80) NULL,
    cancel_amount DECIMAL(18, 2) NOT NULL,
    refundable_amount DECIMAL(18, 2) NOT NULL,
    cancel_reason VARCHAR(200) NOT NULL,
    cancel_status VARCHAR(20) NOT NULL,
    canceled_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payment_cancellations PRIMARY KEY (id),
    CONSTRAINT uk_payment_cancellations_transaction UNIQUE (transaction_key),
    CONSTRAINT uk_payment_cancellations_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_cancellations_amount CHECK (cancel_amount > 0),
    CONSTRAINT ck_payment_cancellations_refundable CHECK (refundable_amount >= 0),
    CONSTRAINT ck_payment_cancellations_status CHECK (cancel_status IN ('DONE'))
);

CREATE INDEX idx_payment_cancellations_payment ON payment_cancellations (payment_id);

ALTER TABLE payment_cancellations
    ADD CONSTRAINT fk_payment_cancellations_payment
    FOREIGN KEY (payment_id) REFERENCES payments (id);

CREATE TABLE toss_webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transmission_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payment_key VARCHAR(200) NULL,
    payload_hash VARCHAR(64) NOT NULL,
    result VARCHAR(30) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_toss_webhook_events PRIMARY KEY (id),
    CONSTRAINT uk_toss_webhook_events_transmission UNIQUE (transmission_id),
    CONSTRAINT ck_toss_webhook_events_result CHECK (
        result IN ('APPLIED', 'NO_CHANGE', 'IGNORED')
    )
);

CREATE INDEX idx_toss_webhook_events_payment ON toss_webhook_events (payment_key);
CREATE INDEX idx_toss_webhook_events_processed ON toss_webhook_events (processed_at);
