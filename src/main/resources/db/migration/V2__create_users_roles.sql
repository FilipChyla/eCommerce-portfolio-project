CREATE TABLE roles (
                       id UUID PRIMARY KEY,
                       name VARCHAR(30) NOT NULL UNIQUE
);

INSERT INTO roles (id, name)
VALUES
    (gen_random_uuid(), 'USER'),
    (gen_random_uuid(), 'ADMIN');

ALTER TABLE users
    ADD COLUMN role_id UUID;

UPDATE users
SET role_id = (
    SELECT id
    FROM roles
    WHERE name = 'USER'
);

ALTER TABLE users
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
            REFERENCES roles(id);