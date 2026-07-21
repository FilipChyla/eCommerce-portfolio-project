CREATE TABLE users
(
    id            UUID PRIMARY KEY,

    email         VARCHAR(255) NOT NULL UNIQUE,

    first_name    VARCHAR(50),
    last_name     VARCHAR(50),
    phone         VARCHAR(20),

    password_hash VARCHAR(255) NOT NULL,

    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,

    enabled       BOOLEAN      NOT NULL DEFAULT TRUE
);