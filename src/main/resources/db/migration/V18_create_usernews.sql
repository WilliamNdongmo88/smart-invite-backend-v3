CREATE TABLE usernews (

    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(150),

    email VARCHAR(150) UNIQUE,

    phone VARCHAR(30),

    newsletter BOOLEAN DEFAULT TRUE
);