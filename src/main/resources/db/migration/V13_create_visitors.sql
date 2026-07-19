CREATE TABLE visitors (

    id BIGSERIAL PRIMARY KEY,

    ip_address VARCHAR(50),

    country VARCHAR(100),

    city VARCHAR(100),

    device VARCHAR(100),

    os VARCHAR(100),

    browser VARCHAR(100)
);