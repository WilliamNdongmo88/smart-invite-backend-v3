CREATE TABLE maintenances (

    id BIGSERIAL PRIMARY KEY,

    status VARCHAR(30),

    maintenance_progress INTEGER,

    estimated_time VARCHAR(50)
);