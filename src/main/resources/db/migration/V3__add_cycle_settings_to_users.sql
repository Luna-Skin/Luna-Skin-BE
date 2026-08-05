ALTER TABLE users
    ADD COLUMN default_period_duration INT NOT NULL DEFAULT 4,
    ADD COLUMN default_cycle_length    INT NOT NULL DEFAULT 28;
