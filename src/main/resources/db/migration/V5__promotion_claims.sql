CREATE TABLE promotion_claims (
    promotion_id UUID NOT NULL REFERENCES promotions (id),
    client_id VARCHAR(64) NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (promotion_id, client_id)
);
