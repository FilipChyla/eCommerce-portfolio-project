CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  UUID REFERENCES categories (id) ON DELETE CASCADE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

CREATE TABLE products
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(200)   NOT NULL,
    description    TEXT,
    price          NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER        NOT NULL DEFAULT 0,
    active         BOOLEAN        NOT NULL DEFAULT true,
    category_id    UUID           NOT NULL REFERENCES categories (id),
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_active ON products (active);