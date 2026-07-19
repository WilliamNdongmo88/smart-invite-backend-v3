CREATE TABLE feedbacks (

    id BIGSERIAL PRIMARY KEY,

    rating INTEGER,

    category VARCHAR(100),

    status VARCHAR(30),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);