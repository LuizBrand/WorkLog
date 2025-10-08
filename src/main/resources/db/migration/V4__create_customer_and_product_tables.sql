CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID UNIQUE,
    name VARCHAR(100) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE customer_product (
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (customer_id, product_id),
    CONSTRAINT fk_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product (id)
);